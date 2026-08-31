from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Anchor not found in {path}: {old[:120]!r}')
    if text.count(old) != 1:
        raise SystemExit(f'Anchor not unique in {path}: {text.count(old)} matches')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


def append_before(path, marker, addition):
    replace_once(path, marker, addition + marker)

# Shared provider contract: add recipe extraction methods and imported-source context to plan request.
replace_once(
    'shared/src/main/kotlin/com/agentickitchen/shared/ai/KitchenAiProvider.kt',
    '    suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse>\n    suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse>\n',
    '''    suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse>\n    suspend fun parseRecipeText(request: RecipeTextImportRequest): AiResult<RecipeImportResponse> =\n        AiResult.Failure(\n            AiFailureType.ProviderUnavailable,\n            retryable = false,\n            userMessage = AiFailureType.ProviderUnavailable.userMessageRes,\n            technicalMessage = "recipe_import_not_supported"\n        )\n    suspend fun scanRecipePhoto(request: RecipePhotoImportRequest): AiResult<RecipeImportResponse> =\n        AiResult.Failure(\n            AiFailureType.ProviderUnavailable,\n            retryable = false,\n            userMessage = AiFailureType.ProviderUnavailable.userMessageRes,\n            technicalMessage = "recipe_import_not_supported"\n        )\n    suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse>\n'''
)
replace_once(
    'shared/src/main/kotlin/com/agentickitchen/shared/ai/KitchenAiProvider.kt',
    '    val language: String,\n    val inventoryLines: List<String> = emptyList()\n)',
    '''    val language: String,\n    val inventoryLines: List<String> = emptyList(),\n    val sourceRecipeIngredientLines: List<String> = emptyList(),\n    val sourceRecipeInstructions: List<String> = emptyList()\n)'''
)

# Normalize AI extraction locally: model canonical IDs/source labels are never authoritative.
append_before(
    'shared/src/main/kotlin/com/agentickitchen/shared/ai/RecipeImport.kt',
    '\nobject DeterministicRecipeImportParser {',
    r'''
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
'''
)

# Source recipe guard: generated plan must preserve imported ingredient identity/amount.
Path('shared/src/main/kotlin/com/agentickitchen/shared/inventory/RecipeImportPlanGuard.kt').write_text(r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.dto.CookingPlanResponse

object RecipeImportPlanGuard {
    data class Result(val valid: Boolean, val reasons: List<String>)

    fun validate(recipe: ImportedRecipe, plan: CookingPlanResponse): Result {
        val reasons = mutableListOf<String>()
        if (!plan.recipeName.equals(recipe.name, ignoreCase = true)) reasons += "recipe_name_changed"
        if (recipe.servings == null || recipe.servings <= 0 || plan.servings != recipe.servings) reasons += "servings_changed"
        if (plan.ingredients.size != recipe.ingredients.size) reasons += "ingredient_count_changed"

        val unmatched = plan.ingredients.toMutableList()
        recipe.ingredients.forEach { imported ->
            val sourceQuantity = imported.quantity
            val sourceUnit = imported.unit
            if (sourceQuantity == null || sourceUnit.isNullOrBlank()) {
                reasons += "source_amount_unreviewed"
                return@forEach
            }
            val candidates = unmatched.withIndex().filter { (_, planned) ->
                LocalIngredientResolver.matches(
                    imported.displayName,
                    imported.canonicalIngredientId,
                    planned.name,
                    planned.canonicalIngredientId
                )
            }
            if (candidates.size != 1) {
                reasons += if (candidates.isEmpty()) "ingredient_missing" else "ingredient_ambiguous"
                return@forEach
            }
            val indexed = candidates.single()
            val planned = indexed.value
            unmatched.removeAt(indexed.index)
            val source = runCatching { InventoryUnits.normalize(sourceQuantity, sourceUnit) }.getOrNull()
            val target = runCatching { InventoryUnits.normalize(planned.quantity, planned.unit) }.getOrNull()
            if (source == null || target == null || source.dimension == UnitDimension.UNKNOWN || target.dimension != source.dimension) {
                reasons += "ingredient_unit_changed"
                return@forEach
            }
            val tolerance = maxOf(0.0001, source.quantity * 0.05)
            if (kotlin.math.abs(target.quantity - source.quantity) > tolerance) reasons += "ingredient_amount_changed"
        }
        if (unmatched.isNotEmpty()) reasons += "ingredient_added"
        return Result(reasons.isEmpty(), reasons.distinct())
    }
}
''', encoding='utf-8')

Path('shared/src/test/kotlin/com/agentickitchen/shared/inventory/RecipeImportPlanGuardTest.kt').write_text(r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeImportPlanGuardTest {
    private val recipe = ImportedRecipe(
        name = "Tomato Rice",
        servings = 2,
        ingredients = listOf(
            ImportedRecipeIngredient("Rice", 200.0, "g", "rice"),
            ImportedRecipeIngredient("Tomato", 2.0, "adet", "tomato")
        ),
        instructions = listOf("Cook the rice with tomato.")
    )

    private fun plan(ingredients: List<PlannedIngredientDto>, servings: Int = 2, name: String = "Tomato Rice") = CookingPlanResponse(
        recipeName = name,
        servings = servings,
        ingredients = ingredients,
        steps = listOf(CookingStepDto("s1", "cook", "Cook.", "stove", 300)),
        safetyNotes = emptyList()
    )

    @Test fun equivalentUnitsAndExactIdentitiesPass() {
        val result = RecipeImportPlanGuard.validate(
            recipe,
            plan(listOf(PlannedIngredientDto("Rice", .2, "kg", "rice"), PlannedIngredientDto("Tomato", 2.0, "adet", "tomato")))
        )
        assertTrue(result.valid)
    }

    @Test fun addedOrRemovedIngredientFailsClosed() {
        val result = RecipeImportPlanGuard.validate(
            recipe,
            plan(listOf(PlannedIngredientDto("Rice", 200.0, "g", "rice"), PlannedIngredientDto("Onion", 1.0, "adet", "onion")))
        )
        assertFalse(result.valid)
        assertTrue("ingredient_missing" in result.reasons || "ingredient_added" in result.reasons)
    }

    @Test fun materialQuantityRewriteFailsClosed() {
        val result = RecipeImportPlanGuard.validate(
            recipe,
            plan(listOf(PlannedIngredientDto("Rice", 300.0, "g", "rice"), PlannedIngredientDto("Tomato", 2.0, "adet", "tomato")))
        )
        assertFalse(result.valid)
        assertTrue("ingredient_amount_changed" in result.reasons)
    }
}
''', encoding='utf-8')

# Prompt context for extraction and source-faithful plan conversion.
append_before(
    'shared/src/main/kotlin/com/agentickitchen/shared/ai/prompt/PromptFactory.kt',
    '\n    fun visionAssessmentPrompt(',
    r'''
    fun recipeImportTextPrompt(text: String, language: String): String = """Extract exactly one cooking recipe from the supplied text.
Language for user-visible names/instructions: $language
Source text:
$text

Rules:
- Use only recipe facts supported by the source.
- Never invent an ingredient, amount, yield, or instruction.
- If quantity or unit is not explicit, return null for that field and explain uncertainty.
- Keep ingredient confidence between 0 and 1.
- Keep top-level confidence between 0 and 1.
- source must be AI_TEXT.
- Return only JSON matching the recipe import schema."""

    fun recipeImportPhotoPrompt(language: String): String = """Extract exactly one cooking recipe from this screenshot or photo.
Language for user-visible names/instructions: $language
Rules:
- Read only text and recipe facts visibly supported by the image.
- Never invent cropped/hidden ingredients, amounts, yield, or instructions.
- If quantity or unit is not visible, return null and explain uncertainty.
- Keep ingredient and top-level confidence between 0 and 1.
- source must be AI_PHOTO.
- Return only JSON matching the recipe import schema."""

    fun importedRecipeContext(ingredientLines: List<String>, instructions: List<String>): String {
        if (ingredientLines.isEmpty() && instructions.isEmpty()) return ""
        return """

This plan is being converted from an imported source recipe. The imported recipe is authoritative.
Source ingredient amounts:
${ingredientLines.joinToString("\n")}
Source instructions:
${instructions.mapIndexed { index, instruction -> "${index + 1}. $instruction" }.joinToString("\n")}
Do not add, remove, substitute, rename to a different ingredient identity, or materially change ingredient amounts.
You may expand the source instructions into safe timed operations and equipment-specific detail, but preserve the dish and instruction intent.
""".trimEnd()
    }
'''
)

# Firebase response kinds/schema.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/FirebaseResponseSchemas.kt',
    '    SHOPPING_IMPORT(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.shoppingImport),\n    COOKING_PHOTO(FirebaseAiTask.VISION, FirebaseResponseSchemas.cookingPhoto),',
    '    SHOPPING_IMPORT(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.shoppingImport),\n    RECIPE_IMPORT_TEXT(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.recipeImport),\n    RECIPE_IMPORT_PHOTO(FirebaseAiTask.VISION, FirebaseResponseSchemas.recipeImport),\n    COOKING_PHOTO(FirebaseAiTask.VISION, FirebaseResponseSchemas.cookingPhoto),'
)
append_before(
    'app-android/src/main/java/com/agentickitchen/android/ai/FirebaseResponseSchemas.kt',
    '\n    val cookingPhoto = Schema.obj(',
    r'''
    val recipeImport = Schema.obj(
        properties = mapOf(
            "recipe" to Schema.obj(
                properties = mapOf(
                    "name" to Schema.string(),
                    "servings" to Schema.integer(nullable = true),
                    "ingredients" to Schema.array(
                        items = Schema.obj(
                            properties = mapOf(
                                "displayName" to Schema.string(),
                                "quantity" to Schema.double(nullable = true),
                                "unit" to Schema.string(nullable = true),
                                "confidence" to Schema.double(),
                                "uncertaintyReason" to Schema.string(nullable = true)
                            ),
                            optionalProperties = listOf("quantity", "unit", "uncertaintyReason")
                        ),
                        minItems = 1
                    ),
                    "instructions" to Schema.array(Schema.string(), minItems = 1)
                ),
                optionalProperties = listOf("servings")
            ),
            "confidence" to Schema.double(),
            "uncertainty" to Schema.string(nullable = true),
            "source" to Schema.enumeration(listOf("AI_TEXT", "AI_PHOTO"))
        ),
        optionalProperties = listOf("uncertainty")
    )
'''
)

# Firebase provider imports + source context + extraction methods.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/FirebaseAiProvider.kt',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\n',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\nimport com.agentickitchen.shared.ai.RecipeImportNormalizer\nimport com.agentickitchen.shared.ai.RecipeImportResponse\nimport com.agentickitchen.shared.ai.RecipeImportSource\nimport com.agentickitchen.shared.ai.RecipePhotoImportRequest\nimport com.agentickitchen.shared.ai.RecipeTextImportRequest\n'
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/FirebaseAiProvider.kt',
    '            ) + inventoryPlanContext(request),',
    '            ) + inventoryPlanContext(request) + PromptFactory.importedRecipeContext(request.sourceRecipeIngredientLines, request.sourceRecipeInstructions),'
)
append_before(
    'app-android/src/main/java/com/agentickitchen/android/ai/FirebaseAiProvider.kt',
    '\n    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =',
    r'''
    override suspend fun parseRecipeText(request: RecipeTextImportRequest): AiResult<RecipeImportResponse> =
        RecipeImportNormalizer.normalizeResult(
            structured(
                kind = FirebaseResponseKind.RECIPE_IMPORT_TEXT,
                prompt = PromptFactory.recipeImportTextPrompt(request.text, request.language),
                decode = json::decodeFromString,
                validate = { it.recipe.name.isNotBlank() && it.recipe.ingredients.isNotEmpty() && it.recipe.instructions.isNotEmpty() }
            ),
            source = RecipeImportSource.AI_TEXT,
            sourceLabel = request.sourceLabel,
            sourceUrl = request.sourceUrl
        )

    override suspend fun scanRecipePhoto(request: RecipePhotoImportRequest): AiResult<RecipeImportResponse> =
        RecipeImportNormalizer.normalizeResult(
            structured(
                kind = FirebaseResponseKind.RECIPE_IMPORT_PHOTO,
                prompt = PromptFactory.recipeImportPhotoPrompt(request.language),
                image = request.image,
                decode = json::decodeFromString,
                validate = { it.recipe.name.isNotBlank() && it.recipe.ingredients.isNotEmpty() && it.recipe.instructions.isNotEmpty() }
            ),
            source = RecipeImportSource.AI_PHOTO,
            sourceLabel = request.sourceLabel
        )
'''
)

# Gemini direct provider imports + plan context + extraction methods/schema.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\n',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\nimport com.agentickitchen.shared.ai.RecipeImportNormalizer\nimport com.agentickitchen.shared.ai.RecipeImportResponse\nimport com.agentickitchen.shared.ai.RecipeImportSource\nimport com.agentickitchen.shared.ai.RecipePhotoImportRequest\nimport com.agentickitchen.shared.ai.RecipeTextImportRequest\n'
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt',
    '''            } else {\n                "\\nAvailable pantry quantities:\\n${request.inventoryLines.joinToString("\\n")}\\nDo not exceed these quantities."\n            },\n            schema = cookingPlanSchema,''',
    '''            } else {\n                "\\nAvailable pantry quantities:\\n${request.inventoryLines.joinToString("\\n")}\\nDo not exceed these quantities."\n            } + PromptFactory.importedRecipeContext(request.sourceRecipeIngredientLines, request.sourceRecipeInstructions),\n            schema = cookingPlanSchema,'''
)
append_before(
    'app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt',
    '\n    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =',
    r'''
    override suspend fun parseRecipeText(request: RecipeTextImportRequest): AiResult<RecipeImportResponse> =
        RecipeImportNormalizer.normalizeResult(
            structured(
                feature = "recipe_import_text",
                prompt = PromptFactory.recipeImportTextPrompt(request.text, request.language),
                schema = recipeImportSchema,
                decode = json::decodeFromString,
                validate = { it.recipe.name.isNotBlank() && it.recipe.ingredients.isNotEmpty() && it.recipe.instructions.isNotEmpty() }
            ),
            source = RecipeImportSource.AI_TEXT,
            sourceLabel = request.sourceLabel,
            sourceUrl = request.sourceUrl
        )

    override suspend fun scanRecipePhoto(request: RecipePhotoImportRequest): AiResult<RecipeImportResponse> =
        RecipeImportNormalizer.normalizeResult(
            structured(
                feature = "recipe_import_photo",
                prompt = PromptFactory.recipeImportPhotoPrompt(request.language),
                schema = recipeImportSchema,
                image = request.image,
                decode = json::decodeFromString,
                validate = { it.recipe.name.isNotBlank() && it.recipe.ingredients.isNotEmpty() && it.recipe.instructions.isNotEmpty() }
            ),
            source = RecipeImportSource.AI_PHOTO,
            sourceLabel = request.sourceLabel
        )
'''
)
append_before(
    'app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt',
    '\n        private val cookingPhotoSchema = schema(',
    r'''
        private val recipeImportSchema = schema(
            """{"type":"object","properties":{"recipe":{"type":"object","properties":{"name":{"type":"string"},"servings":{"type":["integer","null"]},"ingredients":{"type":"array","minItems":1,"items":{"type":"object","properties":{"displayName":{"type":"string"},"quantity":{"type":["number","null"]},"unit":{"type":["string","null"]},"confidence":{"type":"number"},"uncertaintyReason":{"type":["string","null"]}},"required":["displayName","quantity","unit","confidence"]}},"instructions":{"type":"array","minItems":1,"items":{"type":"string"}}},"required":["name","servings","ingredients","instructions"]},"confidence":{"type":"number"},"uncertainty":{"type":["string","null"]},"source":{"type":"string","enum":["AI_TEXT","AI_PHOTO"]}},"required":["recipe","confidence","uncertainty","source"]}"""
        )
'''
)

# Resilient BYOK delegates new methods; no second request on extraction failures.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/ResilientGeminiProvider.kt',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\n',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\nimport com.agentickitchen.shared.ai.RecipePhotoImportRequest\nimport com.agentickitchen.shared.ai.RecipeTextImportRequest\n'
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ai/ResilientGeminiProvider.kt',
    '''    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest) =\n        primary.inspectCookingPhoto(request)\n''',
    '''    override suspend fun parseRecipeText(request: RecipeTextImportRequest) =\n        primary.parseRecipeText(request)\n\n    override suspend fun scanRecipePhoto(request: RecipePhotoImportRequest) =\n        primary.scanRecipePhoto(request)\n\n    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest) =\n        primary.inspectCookingPhoto(request)\n'''
)

# URL loader: bounded redirects/content and public-host check, deterministic extraction first later in ViewModel.
Path('app-android/src/main/java/com/agentickitchen/android/ai/RecipeImportUrlLoader.kt').write_text(r'''package com.agentickitchen.android.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.InetAddress
import java.net.URI

internal data class LoadedRecipeSource(
    val finalUrl: String,
    val sourceLabel: String,
    val body: String
)

internal class RecipeImportUrlLoader(
    private val client: HttpClient = defaultClient(),
    private val ownsClient: Boolean = true,
    private val hostAllowed: suspend (String) -> Boolean = ::isPublicHost
) : Closeable {
    suspend fun load(rawUrl: String): Result<LoadedRecipeSource> = runCatching {
        var current = validateUri(rawUrl)
        repeat(MAX_REDIRECTS + 1) { redirectIndex ->
            check(hostAllowed(current.host)) { "Blocked recipe URL host" }
            val response = client.get(current.toString())
            if (response.status.value in 300..399) {
                check(redirectIndex < MAX_REDIRECTS) { "Too many redirects" }
                val location = response.headers[HttpHeaders.Location] ?: error("Redirect without location")
                current = validateUri(current.resolve(location).toString())
            } else {
                check(response.status.value in 200..299) { "Recipe URL request failed" }
                val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                check(declared == null || declared <= MAX_BODY_BYTES) { "Recipe page too large" }
                val body = response.bodyAsText()
                check(body.toByteArray().size <= MAX_BODY_BYTES) { "Recipe page too large" }
                return@runCatching LoadedRecipeSource(current.toString(), current.host, body)
            }
        }
        error("Too many redirects")
    }

    override fun close() {
        if (ownsClient) client.close()
    }

    companion object {
        const val MAX_BODY_BYTES = 1_500_000L
        const val MAX_REDIRECTS = 3
        const val MAX_AI_TEXT_CHARS = 45_000

        internal fun validateUri(rawUrl: String): URI {
            val uri = URI(rawUrl.trim())
            require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) { "Only HTTP(S) recipe URLs are allowed" }
            require(!uri.host.isNullOrBlank()) { "Recipe URL host is required" }
            require(uri.userInfo == null) { "Recipe URL credentials are not allowed" }
            return uri
        }

        internal fun visibleRecipeText(html: String): String = html
            .replace(Regex("""(?is)<script\b[^>]*>.*?</script>"""), " ")
            .replace(Regex("""(?is)<style\b[^>]*>.*?</style>"""), " ")
            .replace(Regex("""(?is)<[^>]+>"""), "\n")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace(Regex("""[\t ]+"""), " ")
            .replace(Regex("""\n\s*\n+"""), "\n")
            .trim()
            .take(MAX_AI_TEXT_CHARS)

        private suspend fun isPublicHost(host: String): Boolean = withContext(Dispatchers.IO) {
            if (host.equals("localhost", true) || host.endsWith(".localhost", true)) return@withContext false
            runCatching { InetAddress.getAllByName(host).all(::isPublicAddress) }.getOrDefault(false)
        }

        private fun isPublicAddress(address: InetAddress): Boolean {
            if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) return false
            val bytes = address.address
            if (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc) return false
            if (bytes.size == 4) {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                if (first == 100 && second in 64..127) return false
                if (first == 169 && second == 254) return false
            }
            return true
        }

        private fun defaultClient() = HttpClient(OkHttp) {
            followRedirects = false
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
        }
    }
}
''', encoding='utf-8')

Path('app-android/src/test/java/com/agentickitchen/android/ai/RecipeImportUrlLoaderTest.kt').write_text(r'''package com.agentickitchen.android.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportUrlLoaderTest {
    @Test fun visibleTextDropsScriptAndKeepsRecipeContentBounded() {
        val html = "<html><script>secret()</script><body><h1>Rice</h1><p>200 g rice &amp; tomato</p></body></html>"
        val text = RecipeImportUrlLoader.visibleRecipeText(html)
        assertFalse(text.contains("secret"))
        assertTrue(text.contains("Rice"))
        assertTrue(text.contains("200 g rice & tomato"))
    }

    @Test fun rejectsNonHttpAndCredentialUrls() {
        assertTrue(runCatching { RecipeImportUrlLoader.validateUri("file:///tmp/recipe") }.isFailure)
        assertTrue(runCatching { RecipeImportUrlLoader.validateUri("https://user:pass@example.com/r") }.isFailure)
    }

    @Test fun followsBoundedPublicRedirectAndReturnsFinalSource() = runBlocking {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/start") {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/recipe"))
            } else {
                respond("<h1>Recipe</h1>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
            }
        }
        val client = HttpClient(engine) { install(HttpTimeout) }
        val loader = RecipeImportUrlLoader(client, ownsClient = false, hostAllowed = { true })
        val loaded = loader.load("https://example.com/start").getOrThrow()
        assertEquals("example.com", loaded.sourceLabel)
        assertTrue(loaded.finalUrl.endsWith("/recipe"))
        assertEquals("<h1>Recipe</h1>", loaded.body)
        client.close()
    }
}
''', encoding='utf-8')

# Firebase managed-provider regression test for task routing and image forwarding.
replace_once(
    'app-android/src/test/java/com/agentickitchen/android/ai/FirebaseAiProviderTest.kt',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\n',
    'import com.agentickitchen.shared.ai.RecipeOptionsRequest\nimport com.agentickitchen.shared.ai.RecipePhotoImportRequest\nimport com.agentickitchen.shared.ai.RecipeTextImportRequest\n'
)
append_before(
    'app-android/src/test/java/com/agentickitchen/android/ai/FirebaseAiProviderTest.kt',
    '\n    @Test\n    fun `cooking photo uses vision model class`() {',
    r'''
    @Test
    fun `recipe text import uses extraction class and normalizes source`() = runBlocking {
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            responseKind = kind
            FirebaseGatewayResponse(recipeImportJson, "extraction-test-model")
        })
        val result = provider.parseRecipeText(RecipeTextImportRequest("ambiguous recipe", "English", "shared"))
        assertEquals(FirebaseResponseKind.RECIPE_IMPORT_TEXT, responseKind)
        assertEquals(FirebaseAiTask.EXTRACTION, responseKind?.task)
        assertTrue(result is AiResult.Success)
        assertEquals(com.agentickitchen.shared.ai.RecipeImportSource.AI_TEXT, result.getOrNull()?.source)
        assertEquals("shared", result.getOrNull()?.recipe?.sourceLabel)
    }

    @Test
    fun `recipe photo import uses vision class and forwards image`() = runBlocking {
        var imageSeen = false
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, image ->
            responseKind = kind
            imageSeen = image?.bytes?.contentEquals(byteArrayOf(4, 5, 6)) == true
            FirebaseGatewayResponse(recipeImportJson.replace("AI_TEXT", "AI_PHOTO"), "vision-test-model")
        })
        val result = provider.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(4, 5, 6), "image/jpeg"), "English"))
        assertTrue(imageSeen)
        assertEquals(FirebaseResponseKind.RECIPE_IMPORT_PHOTO, responseKind)
        assertEquals(FirebaseAiTask.VISION, responseKind?.task)
        assertTrue(result is AiResult.Success)
    }
'''
)
replace_once(
    'app-android/src/test/java/com/agentickitchen/android/ai/FirebaseAiProviderTest.kt',
    '        val cookingPlanJson = """',
    '''        val recipeImportJson = """\n            {"recipe":{"name":"Tomato Rice","servings":2,"ingredients":[{"displayName":"Rice","quantity":200.0,"unit":"g","confidence":0.98},{"displayName":"Tomato","quantity":2.0,"unit":"adet","confidence":0.95}],"instructions":["Cook rice with tomato."]},"confidence":0.95,"source":"AI_TEXT"}\n        """.trimIndent()\n\n        val cookingPlanJson = """'''
)
