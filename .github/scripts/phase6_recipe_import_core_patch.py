from pathlib import Path

files = {
"shared/src/main/kotlin/com/agentickitchen/shared/ai/RecipeImport.kt": r'''package com.agentickitchen.shared.ai

import com.agentickitchen.shared.inventory.LocalIngredientResolver
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class RecipeImportSource {
    URL_JSON_LD,
    PLAIN_TEXT,
    AI_TEXT,
    AI_PHOTO,
    ANDROID_SHARE
}

@Serializable
data class ImportedRecipeIngredient(
    val displayName: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val canonicalIngredientId: String? = null,
    val rawText: String? = null,
    val confidence: Double = 1.0,
    val uncertaintyReason: String? = null
)

@Serializable
data class ImportedRecipe(
    val name: String,
    val servings: Int? = null,
    val ingredients: List<ImportedRecipeIngredient>,
    val instructions: List<String>,
    val sourceLabel: String? = null,
    val sourceUrl: String? = null
)

@Serializable
data class RecipeImportResponse(
    val recipe: ImportedRecipe,
    val confidence: Double,
    val uncertainty: String? = null,
    val source: RecipeImportSource
)

data class RecipeTextImportRequest(
    val text: String,
    val language: String,
    val sourceLabel: String? = null,
    val sourceUrl: String? = null
)

data class RecipePhotoImportRequest(
    val image: KitchenImage,
    val language: String,
    val sourceLabel: String? = null
)

object DeterministicRecipeImportParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonLdScript = Regex(
        """<script[^>]*type\s*=\s*[\"']application/ld\+json[\"'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val ingredientHeading = Regex(
        """^(ingredients?|malzemeler)\s*:?[\s]*$""",
        RegexOption.IGNORE_CASE
    )
    private val instructionHeading = Regex(
        """^(instructions?|directions?|method|steps?|yapılışı|yapilisi|hazırlanışı|hazirlanisi)\s*:?[\s]*$""",
        RegexOption.IGNORE_CASE
    )
    private val quantityPrefix = Regex(
        """^(\d+\s*/\s*\d+|\d+(?:[.,]\d+)?)\s*(kg|kilogram|kilo|g|gram|gr|l|litre|liter|ml|millilitre|milliliter|adet|piece|pieces|pcs|count|paket|package|pack|packs|demet|bunch|bunches|cup|cups|tbsp|tablespoon|tablespoons|tsp|teaspoon|teaspoons)?\b\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(source: String, sourceLabel: String? = null, sourceUrl: String? = null): RecipeImportResponse? =
        parseJsonLd(source, sourceLabel, sourceUrl) ?: parsePlainText(source, sourceLabel, sourceUrl)

    fun parseJsonLd(document: String, sourceLabel: String? = null, sourceUrl: String? = null): RecipeImportResponse? {
        val blocks = jsonLdScript.findAll(document).map { it.groupValues[1].trim() }.toList()
            .ifEmpty { listOf(document.trim()) }
        blocks.forEach { block ->
            val root = runCatching { json.parseToJsonElement(block) }.getOrNull() ?: return@forEach
            val recipeNode = findRecipeNode(root) ?: return@forEach
            val parsed = recipeFromJsonLd(recipeNode, sourceLabel, sourceUrl) ?: return@forEach
            val uncertain = parsed.ingredients.count { it.quantity == null || it.unit == null }
            return RecipeImportResponse(
                recipe = parsed,
                confidence = if (uncertain == 0) 1.0 else 0.92,
                uncertainty = if (uncertain == 0) null else "$uncertain ingredient amount(s) need review",
                source = RecipeImportSource.URL_JSON_LD
            )
        }
        return null
    }

    fun parsePlainText(text: String, sourceLabel: String? = null, sourceUrl: String? = null): RecipeImportResponse? {
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val ingredientIndex = lines.indexOfFirst { ingredientHeading.matches(it) }
        val instructionIndex = lines.indexOfFirst { instructionHeading.matches(it) }
        if (ingredientIndex < 0 || instructionIndex <= ingredientIndex + 1) return null

        val title = lines.take(ingredientIndex)
            .firstOrNull { !it.startsWith("http://") && !it.startsWith("https://") }
            ?.removePrefix("#")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return null
        val ingredientLines = lines.subList(ingredientIndex + 1, instructionIndex)
            .map(::stripListPrefix)
            .filter(String::isNotEmpty)
        val instructionLines = lines.drop(instructionIndex + 1)
            .map(::stripInstructionPrefix)
            .filter(String::isNotEmpty)
        if (ingredientLines.isEmpty() || instructionLines.isEmpty()) return null

        val ingredients = ingredientLines.map(::parseIngredientLine)
        val uncertain = ingredients.count { it.quantity == null || it.unit == null }
        return RecipeImportResponse(
            recipe = ImportedRecipe(
                name = title,
                servings = parseServings(lines.take(ingredientIndex).joinToString(" ")),
                ingredients = ingredients,
                instructions = instructionLines,
                sourceLabel = sourceLabel,
                sourceUrl = sourceUrl
            ),
            confidence = if (uncertain == 0) 0.98 else 0.88,
            uncertainty = if (uncertain == 0) null else "$uncertain ingredient amount(s) need review",
            source = RecipeImportSource.PLAIN_TEXT
        )
    }

    fun parseIngredientLine(rawLine: String): ImportedRecipeIngredient {
        val clean = stripListPrefix(rawLine)
        val match = quantityPrefix.find(clean)
        val quantity = match?.groupValues?.get(1)?.let(::parseQuantity)
        val rawUnit = match?.groupValues?.get(2)?.trim().orEmpty()
        val remainder = match?.groupValues?.get(3)?.trim().orEmpty()
        val name = if (match != null && remainder.isNotBlank()) remainder else clean
        val unit = when {
            quantity == null -> null
            rawUnit.isNotBlank() -> canonicalUnit(rawUnit)
            else -> "adet"
        }
        val normalizedName = name
            .substringBefore("(")
            .substringBefore(",")
            .trim()
            .ifBlank { name.trim() }
        val canonical = LocalIngredientResolver.resolveCanonicalId(normalizedName)
        val needsReview = quantity == null || unit == null
        return ImportedRecipeIngredient(
            displayName = normalizedName,
            quantity = quantity,
            unit = unit,
            canonicalIngredientId = canonical,
            rawText = clean,
            confidence = if (needsReview) 0.75 else 1.0,
            uncertaintyReason = if (needsReview) "amount_not_explicit" else null
        )
    }

    private fun recipeFromJsonLd(node: JsonObject, sourceLabel: String?, sourceUrl: String?): ImportedRecipe? {
        val name = node.string("name")?.trim().orEmpty()
        if (name.isBlank()) return null
        val ingredients = node["recipeIngredient"]?.asStringList()?.map(::parseIngredientLine).orEmpty()
        val instructions = node["recipeInstructions"]?.let(::instructionStrings).orEmpty()
        if (ingredients.isEmpty() || instructions.isEmpty()) return null
        return ImportedRecipe(
            name = name,
            servings = node["recipeYield"]?.let(::servingsFromElement),
            ingredients = ingredients,
            instructions = instructions,
            sourceLabel = sourceLabel,
            sourceUrl = sourceUrl
        )
    }

    private fun findRecipeNode(element: JsonElement): JsonObject? = when (element) {
        is JsonObject -> {
            if (isRecipeType(element["@type"])) element
            else element["@graph"]?.let(::findRecipeNode)
                ?: element.values.asSequence().mapNotNull(::findRecipeNode).firstOrNull()
        }
        is JsonArray -> element.asSequence().mapNotNull(::findRecipeNode).firstOrNull()
        else -> null
    }

    private fun isRecipeType(element: JsonElement?): Boolean = when (element) {
        is JsonPrimitive -> element.contentOrNull.equals("Recipe", ignoreCase = true)
        is JsonArray -> element.any(::isRecipeType)
        else -> false
    }

    private fun instructionStrings(element: JsonElement): List<String> = when (element) {
        is JsonPrimitive -> listOfNotNull(element.contentOrNull?.trim()?.takeIf(String::isNotEmpty))
        is JsonArray -> element.flatMap(::instructionStrings)
        is JsonObject -> {
            val direct = element.string("text")?.trim()?.takeIf(String::isNotEmpty)
            if (direct != null) listOf(direct)
            else element["itemListElement"]?.let(::instructionStrings).orEmpty()
        }
        else -> emptyList()
    }

    private fun JsonElement.asStringList(): List<String> = when (this) {
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
        is JsonPrimitive -> listOfNotNull(contentOrNull?.trim()?.takeIf(String::isNotEmpty))
        else -> emptyList()
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun servingsFromElement(element: JsonElement): Int? = when (element) {
        is JsonPrimitive -> element.intOrNull ?: parseServings(element.contentOrNull.orEmpty())
        is JsonArray -> element.asSequence().mapNotNull(::servingsFromElement).firstOrNull()
        else -> null
    }

    private fun parseServings(text: String): Int? = Regex("""\b(\d{1,3})\b""")
        .find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }

    private fun parseQuantity(raw: String): Double? {
        val trimmed = raw.replace(" ", "")
        if ("/" in trimmed) {
            val parts = trimmed.split("/")
            if (parts.size != 2) return null
            val numerator = parts[0].toDoubleOrNull() ?: return null
            val denominator = parts[1].toDoubleOrNull()?.takeIf { it != 0.0 } ?: return null
            return numerator / denominator
        }
        return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    }

    private fun canonicalUnit(raw: String): String = when (raw.trim().lowercase()) {
        "kg", "kilogram", "kilo" -> "kg"
        "g", "gram", "gr" -> "g"
        "l", "litre", "liter" -> "l"
        "ml", "millilitre", "milliliter" -> "ml"
        "adet", "piece", "pieces", "pcs", "count" -> "adet"
        "paket", "package", "pack", "packs" -> "paket"
        "demet", "bunch", "bunches" -> "demet"
        "cup", "cups" -> "cup"
        "tbsp", "tablespoon", "tablespoons" -> "tbsp"
        "tsp", "teaspoon", "teaspoons" -> "tsp"
        else -> raw.trim()
    }

    private fun stripListPrefix(line: String): String = line
        .replace(Regex("""^[-*•]+\s*"""), "")
        .trim()

    private fun stripInstructionPrefix(line: String): String = stripListPrefix(line)
        .replace(Regex("""^(?:step\s*)?\d+[.)-]?\s*""", RegexOption.IGNORE_CASE), "")
        .trim()
}
''',
"shared/src/main/kotlin/com/agentickitchen/shared/inventory/RecipeImportPantryPlanner.kt": r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient

enum class RecipeImportAvailability {
    AVAILABLE,
    PARTIAL,
    MISSING,
    NEEDS_REVIEW
}

data class RecipeImportIngredientMatch(
    val ingredient: ImportedRecipeIngredient,
    val availability: RecipeImportAvailability,
    val matchingPantryItemIds: List<String>,
    val availableQuantity: Double? = null,
    val normalizedUnit: String? = null,
    val canCheckSubstitution: Boolean = false
)

data class RecipeImportPantrySummary(
    val matches: List<RecipeImportIngredientMatch>
) {
    val availableCount: Int get() = matches.count { it.availability == RecipeImportAvailability.AVAILABLE }
    val partialCount: Int get() = matches.count { it.availability == RecipeImportAvailability.PARTIAL }
    val missingCount: Int get() = matches.count { it.availability == RecipeImportAvailability.MISSING }
    val needsReviewCount: Int get() = matches.count { it.availability == RecipeImportAvailability.NEEDS_REVIEW }
    val readyForValidatedPlan: Boolean get() = needsReviewCount == 0
}

object RecipeImportPantryPlanner {
    fun compare(
        recipe: ImportedRecipe,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double> = emptyMap()
    ): RecipeImportPantrySummary = RecipeImportPantrySummary(
        recipe.ingredients.map { ingredient -> compareIngredient(ingredient, inventory, reservedByItem) }
    )

    private fun compareIngredient(
        ingredient: ImportedRecipeIngredient,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double>
    ): RecipeImportIngredientMatch {
        val requestedQuantity = ingredient.quantity
        val requestedUnit = ingredient.unit
        if (requestedQuantity == null || requestedUnit.isNullOrBlank()) {
            return RecipeImportIngredientMatch(
                ingredient = ingredient,
                availability = RecipeImportAvailability.NEEDS_REVIEW,
                matchingPantryItemIds = emptyList(),
                canCheckSubstitution = false
            )
        }
        val requested = runCatching { InventoryUnits.normalize(requestedQuantity, requestedUnit) }.getOrNull()
            ?: return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.NEEDS_REVIEW,
                emptyList(),
                canCheckSubstitution = false
            )
        if (requested.dimension == UnitDimension.UNKNOWN) {
            return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.NEEDS_REVIEW,
                emptyList(),
                normalizedUnit = requested.unit,
                canCheckSubstitution = false
            )
        }

        val nameMatches = inventory.filter { item ->
            LocalIngredientResolver.matches(
                ingredient.displayName,
                ingredient.canonicalIngredientId,
                item.originalName,
                item.canonicalIngredientId
            )
        }
        if (nameMatches.isEmpty()) {
            return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.MISSING,
                emptyList(),
                availableQuantity = 0.0,
                normalizedUnit = requested.unit,
                canCheckSubstitution = true
            )
        }

        val compatible = nameMatches.mapNotNull { item ->
            val amount = runCatching { InventoryUnits.normalize(item.quantity, item.unit) }.getOrNull() ?: return@mapNotNull null
            if (amount.dimension != requested.dimension) return@mapNotNull null
            val available = (amount.quantity - (reservedByItem[item.id] ?: 0.0)).coerceAtLeast(0.0)
            item.id to available
        }
        if (compatible.isEmpty()) {
            return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.NEEDS_REVIEW,
                nameMatches.map(PantryStockItem::id),
                normalizedUnit = requested.unit,
                canCheckSubstitution = false
            )
        }

        val totalAvailable = compatible.sumOf { it.second }
        val availability = when {
            totalAvailable >= requested.quantity -> RecipeImportAvailability.AVAILABLE
            totalAvailable > 0.0 -> RecipeImportAvailability.PARTIAL
            else -> RecipeImportAvailability.MISSING
        }
        return RecipeImportIngredientMatch(
            ingredient = ingredient,
            availability = availability,
            matchingPantryItemIds = compatible.map { it.first },
            availableQuantity = totalAvailable,
            normalizedUnit = requested.unit,
            canCheckSubstitution = availability != RecipeImportAvailability.AVAILABLE
        )
    }
}
''',
"shared/src/test/kotlin/com/agentickitchen/shared/ai/RecipeImportParserTest.kt": r'''package com.agentickitchen.shared.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportParserTest {
    @Test
    fun `parses recipe JSON-LD from graph without AI`() {
        val html = """
            <html><head><script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[
              {"@type":"WebSite","name":"Example"},
              {"@type":"Recipe","name":"Tomato Rice","recipeYield":"2 servings",
               "recipeIngredient":["200 g rice","2 tomatoes","1 onion"],
               "recipeInstructions":[{"@type":"HowToStep","text":"Rinse the rice."},{"@type":"HowToStep","text":"Cook with tomato and onion."}]}
            ]}
            </script></head></html>
        """.trimIndent()

        val result = DeterministicRecipeImportParser.parseJsonLd(html, "example.com", "https://example.com/rice")
        assertNotNull(result)
        assertEquals(RecipeImportSource.URL_JSON_LD, result?.source)
        assertEquals("Tomato Rice", result?.recipe?.name)
        assertEquals(2, result?.recipe?.servings)
        assertEquals(3, result?.recipe?.ingredients?.size)
        assertEquals(200.0, result?.recipe?.ingredients?.first()?.quantity ?: 0.0, 0.0001)
        assertEquals("g", result?.recipe?.ingredients?.first()?.unit)
        assertEquals("rice", result?.recipe?.ingredients?.first()?.canonicalIngredientId)
        assertEquals(2, result?.recipe?.instructions?.size)
    }

    @Test
    fun `parses explicitly sectioned plain text deterministically`() {
        val text = """
            Onion Eggs
            Serves 2
            Ingredients:
            - 2 eggs
            - 1 onion
            Instructions:
            1. Slice the onion.
            2. Cook the onion and eggs.
        """.trimIndent()

        val result = DeterministicRecipeImportParser.parsePlainText(text, "shared text")
        assertNotNull(result)
        assertEquals(RecipeImportSource.PLAIN_TEXT, result?.source)
        assertEquals("Onion Eggs", result?.recipe?.name)
        assertEquals(2, result?.recipe?.servings)
        assertEquals("egg", result?.recipe?.ingredients?.first()?.canonicalIngredientId)
        assertEquals("adet", result?.recipe?.ingredients?.first()?.unit)
        assertTrue(result?.recipe?.instructions?.first()?.startsWith("Slice") == true)
    }

    @Test
    fun `keeps implicit ingredient amount uncertain rather than inventing`() {
        val ingredient = DeterministicRecipeImportParser.parseIngredientLine("salt to taste")
        assertEquals("salt to taste", ingredient.displayName)
        assertNull(ingredient.quantity)
        assertNull(ingredient.unit)
        assertEquals("amount_not_explicit", ingredient.uncertaintyReason)
    }

    @Test
    fun `ambiguous prose without recipe sections does not pretend to parse`() {
        val result = DeterministicRecipeImportParser.parsePlainText("I made a nice tomato dish yesterday and it was great.")
        assertNull(result)
    }
}
''',
"shared/src/test/kotlin/com/agentickitchen/shared/inventory/RecipeImportPantryPlannerTest.kt": r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportPantryPlannerTest {
    private fun pantry(id: String, name: String, quantity: Double, unit: String, canonical: String? = null) = PantryStockItem(
        id = id,
        canonicalIngredientId = canonical,
        originalName = name,
        quantity = quantity,
        unit = unit,
        unitDimension = InventoryUnits.normalize(quantity, unit).dimension,
        source = "test",
        createdAt = "2026-08-31T12:00:00Z",
        updatedAt = "2026-08-31T12:00:00Z"
    )

    @Test
    fun `classifies available partial missing and review-required ingredients`() {
        val recipe = ImportedRecipe(
            name = "Dinner",
            ingredients = listOf(
                ImportedRecipeIngredient("Rice", 200.0, "g", "rice"),
                ImportedRecipeIngredient("Tomato", 3.0, "adet", "tomato"),
                ImportedRecipeIngredient("Garlic", 1.0, "adet", "garlic"),
                ImportedRecipeIngredient("Salt", null, null, "salt")
            ),
            instructions = listOf("Cook.")
        )
        val summary = RecipeImportPantryPlanner.compare(
            recipe,
            inventory = listOf(
                pantry("rice", "Pirinç", 500.0, "g", "rice"),
                pantry("tomato", "Domates", 2.0, "adet", "tomato")
            )
        )

        assertEquals(RecipeImportAvailability.AVAILABLE, summary.matches[0].availability)
        assertEquals(RecipeImportAvailability.PARTIAL, summary.matches[1].availability)
        assertEquals(RecipeImportAvailability.MISSING, summary.matches[2].availability)
        assertEquals(RecipeImportAvailability.NEEDS_REVIEW, summary.matches[3].availability)
        assertEquals(1, summary.availableCount)
        assertEquals(1, summary.partialCount)
        assertEquals(1, summary.missingCount)
        assertEquals(1, summary.needsReviewCount)
        assertFalse(summary.readyForValidatedPlan)
        assertTrue(summary.matches[2].canCheckSubstitution)
    }

    @Test
    fun `reserved quantities reduce imported recipe availability`() {
        val recipe = ImportedRecipe(
            name = "Rice",
            ingredients = listOf(ImportedRecipeIngredient("Rice", 300.0, "g", "rice")),
            instructions = listOf("Cook.")
        )
        val summary = RecipeImportPantryPlanner.compare(
            recipe,
            inventory = listOf(pantry("rice", "Rice", 500.0, "g", "rice")),
            reservedByItem = mapOf("rice" to 250.0)
        )
        assertEquals(RecipeImportAvailability.PARTIAL, summary.matches.single().availability)
        assertEquals(250.0, summary.matches.single().availableQuantity ?: 0.0, 0.0001)
    }

    @Test
    fun `unknown measurement units fail closed to review`() {
        val recipe = ImportedRecipe(
            name = "Soup",
            ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "cup", "milk")),
            instructions = listOf("Cook.")
        )
        val summary = RecipeImportPantryPlanner.compare(
            recipe,
            inventory = listOf(pantry("milk", "Milk", 500.0, "ml", "milk"))
        )
        assertEquals(RecipeImportAvailability.NEEDS_REVIEW, summary.matches.single().availability)
    }
}
'''
}

for path, content in files.items():
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists():
        raise SystemExit(f"Refusing to overwrite existing file: {path}")
    target.write_text(content, encoding="utf-8")
