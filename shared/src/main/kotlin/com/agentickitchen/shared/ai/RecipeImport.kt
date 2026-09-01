package com.agentickitchen.shared.ai

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

private const val RECIPE_IMPORT_AMOUNT_REVIEW_PREFIX = "amount_review_required:"

fun recipeImportAmountReviewCode(count: Int): String? =
    count.takeIf { it > 0 }?.let { "$RECIPE_IMPORT_AMOUNT_REVIEW_PREFIX$it" }

fun recipeImportAmountReviewCount(uncertainty: String?): Int? = uncertainty
    ?.trim()
    ?.takeIf { it.startsWith(RECIPE_IMPORT_AMOUNT_REVIEW_PREFIX) }
    ?.removePrefix(RECIPE_IMPORT_AMOUNT_REVIEW_PREFIX)
    ?.toIntOrNull()
    ?.takeIf { it > 0 }

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

object RecipeImportNormalizer {
    fun normalize(
        response: RecipeImportResponse,
        source: RecipeImportSource,
        sourceLabel: String? = null,
        sourceUrl: String? = null
    ): RecipeImportResponse? {
        if (!response.confidence.isFinite() || response.confidence !in 0.0..1.0) return null
        val recipe = response.recipe
        val cleanName = recipe.name.trim().takeIf(String::isNotEmpty) ?: return null
        val servings = recipe.servings?.takeIf { it > 0 }
        if (recipe.servings != null && servings == null) return null
        val instructions = recipe.instructions.map(String::trim).filter(String::isNotEmpty)
        if (instructions.isEmpty() || recipe.ingredients.isEmpty()) return null
        val ingredients = recipe.ingredients.map { ingredient ->
            val display = ingredient.displayName.trim().takeIf(String::isNotEmpty) ?: return null
            if (!ingredient.confidence.isFinite() || ingredient.confidence !in 0.0..1.0) return null
            val quantity = ingredient.quantity
            if (quantity != null && (!quantity.isFinite() || quantity <= 0.0)) return null
            val unit = ingredient.unit?.trim()?.takeIf(String::isNotEmpty)
            if (ingredient.unit != null && unit == null) return null
            ingredient.copy(
                displayName = display,
                quantity = quantity,
                unit = unit,
                canonicalIngredientId = LocalIngredientResolver.resolveCanonicalId(display),
                rawText = ingredient.rawText?.trim()?.takeIf(String::isNotEmpty),
                uncertaintyReason = ingredient.uncertaintyReason?.trim()?.takeIf(String::isNotEmpty)
            )
        }
        return response.copy(
            recipe = recipe.copy(
                name = cleanName,
                servings = servings,
                ingredients = ingredients,
                instructions = instructions,
                sourceLabel = sourceLabel?.trim()?.takeIf(String::isNotEmpty) ?: recipe.sourceLabel?.trim()?.takeIf(String::isNotEmpty),
                sourceUrl = sourceUrl?.trim()?.takeIf(String::isNotEmpty) ?: recipe.sourceUrl?.trim()?.takeIf(String::isNotEmpty)
            ),
            uncertainty = response.uncertainty?.trim()?.takeIf(String::isNotEmpty),
            source = source
        )
    }

    fun normalizeResult(
        result: AiResult<RecipeImportResponse>,
        source: RecipeImportSource,
        sourceLabel: String? = null,
        sourceUrl: String? = null
    ): AiResult<RecipeImportResponse> = when (result) {
        is AiResult.Failure -> result
        is AiResult.Success -> {
            val normalized = normalize(result.value, source, sourceLabel, sourceUrl)
            if (normalized == null) {
                AiResult.Failure(
                    AiFailureType.InvalidResponse,
                    retryable = true,
                    userMessage = AiFailureType.InvalidResponse.userMessageRes,
                    technicalMessage = "invalid_recipe_import"
                )
            } else {
                AiResult.Success(normalized, result.provider, result.model)
            }
        }
    }
}

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
                uncertainty = recipeImportAmountReviewCode(uncertain),
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
            uncertainty = recipeImportAmountReviewCode(uncertain),
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
