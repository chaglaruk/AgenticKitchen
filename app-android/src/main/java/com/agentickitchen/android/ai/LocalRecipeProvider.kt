package com.agentickitchen.android.ai

import com.agentickitchen.android.AppLogger
import com.agentickitchen.android.catalogIngredientForName
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.ai.dto.RecipeOptionDto
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import com.agentickitchen.shared.ai.prompt.PromptFactory
import com.agentickitchen.shared.validator.IngredientSafety
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale

enum class ProviderFailureCategory {
    HTTP,
    TIMEOUT,
    NETWORK,
    EMPTY_RESPONSE,
    INVALID_REQUEST,
    CONSTRAINT_CONFLICT
}

class ProviderFailure(
    val providerId: String,
    val category: ProviderFailureCategory,
    val statusCode: Int? = null,
    val responseLength: Int = 0,
    cause: Throwable? = null
) : Exception(
    "provider=$providerId category=$category status=${statusCode ?: "none"} responseLength=$responseLength",
    cause
)

internal data class ProviderDiagnostic(
    val providerId: String,
    val statusCode: Int?,
    val category: String,
    val responseLength: Int
) {
    fun asLogMessage(): String =
        "provider=$providerId status=${statusCode ?: "none"} category=$category responseLength=$responseLength"
}

class LocalRecipeProvider internal constructor(
    private val diagnosticSink: (ProviderDiagnostic) -> Unit
) : KitchenAiProvider {

    constructor() : this(
        diagnosticSink = { AppLogger.i("AiProvider", it.asLogMessage()) }
    )

    suspend fun generateContent(prompt: String): String {
        val response = when {
            OPTIONS_MARKER in prompt -> recipeOptions(prompt)
            PLAN_MARKER in prompt -> cookingPlan(prompt)
            SCAN_MARKER in prompt -> throw invalidRequest()
            else -> localAssistantResponse(prompt)
        }
        diagnosticSink(ProviderDiagnostic(PROVIDER_ID, null, "SUCCESS", response.length))
        return response
    }

    override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> =
        offlineResult {
            json.decodeFromString<RecipeOptionsResponse>(
                generateContent(
                    PromptFactory.recipeOptionsPrompt(
                        request.ingredients,
                        request.equipment,
                        request.dietType,
                        request.allergies,
                        request.language
                    )
                )
            )
        }

    override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> =
        offlineResult {
            json.decodeFromString<CookingPlanResponse>(
                generateContent(
                    PromptFactory.cookingPlanPrompt(
                        request.recipeName,
                        request.ingredients,
                        request.equipment,
                        request.servings,
                        request.stoveType,
                        request.stoveMaxLevel,
                        request.ovenAvailable,
                        request.ovenHasFan,
                        request.airfryerAvailable,
                        request.dietType,
                        request.allergies,
                        request.language
                    )
                )
            )
        }

    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =
        unavailable()

    override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
        unavailable()

    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
        unavailable()

    override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> {
        val prompt = """
            Kitchen guidance request
            Recipe: ${request.recipeName}
            Current step: ${request.currentStep}
            Stove type: ${request.resource.orEmpty()}
            Language: ${request.language}
            Question: ${request.question}
        """.trimIndent()
        return offlineResult { CookingChatResponse(generateContent(prompt)) }
    }

    override suspend fun testConnection(): AiResult<Unit> =
        AiResult.Success(Unit, AiProviderId.FREE, MODEL)

    private suspend fun <T> offlineResult(block: suspend () -> T): AiResult<T> = try {
        AiResult.Success(block(), AiProviderId.FREE, MODEL)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        offlineFailure(error)
    }

    private fun offlineFailure(error: Throwable): AiResult.Failure {
        val type = if (error is ProviderFailure && error.category == ProviderFailureCategory.CONSTRAINT_CONFLICT) {
            AiFailureType.InvalidPlan
        } else {
            AiFailureType.InvalidResponse
        }
        return AiResult.Failure(type, false, type.userMessageRes)
    }

    private fun unavailable(): AiResult.Failure =
        AiResult.Failure(
            AiFailureType.ProviderUnavailable,
            false,
            AiFailureType.ProviderUnavailable.userMessageRes
        )

    private fun recipeOptions(prompt: String): String {
        var ingredients = prompt.lineValue("Ingredients:").csv().map(::classifyIngredient)
        if (ingredients.isEmpty()) throw invalidRequest()
        val equipment = prompt.lineValue("Available equipment:").csv()
        val isTurkish = prompt.contains("Yanıtını Türkçe ver.")
        ingredients = ingredients.map { it.localized(isTurkish) }
        checkConstraints(ingredients, prompt)
        val techniques = availableTechniques(ingredients, equipment.toSet())
        if (techniques.size < 3) throw constraintConflict()
        val options = techniques.take(3).mapIndexed { index, technique ->
            recipeOption(index, technique, ingredients, equipment.toSet(), isTurkish)
        }
        return json.encodeToString(RecipeOptionsResponse(options))
    }

    private fun cookingPlan(prompt: String): String {
        val recipeName = prompt.substringAfter(PLAN_MARKER, "").substringBefore('"').trim()
        var ingredients = prompt.lineValue("Ingredients:").csv().map(::classifyIngredient)
        val equipment = prompt.lineValue("Available equipment:").csv().toSet()
        val servings = prompt.lineValue("Servings:").toIntOrNull()
        if (recipeName.isBlank() || ingredients.isEmpty() || servings == null || servings <= 0) {
            throw invalidRequest()
        }

        val isTurkish = prompt.contains("Yanıtını Türkçe ver.")
        ingredients = ingredients.map { it.localized(isTurkish) }
        checkConstraints(ingredients, prompt)
        val stoveType = prompt.lineValue("Stove type:").lowercase()
        val stoveMax = Regex("""Electric stove maximum level: (\d+)""")
            .find(prompt)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: 9
        val technique = techniqueFromRecipeName(recipeName)
        if (!technique.isSupported(equipment)) throw invalidRequest()
        val recipeIngredients = ingredientsForRecipe(recipeName, ingredients)
        if (technique.isCold && recipeIngredients.any { it.role.isRawProtein }) throw constraintConflict()
        val plannedIngredients = recipeIngredients.map { plannedIngredient(it, servings) }
        val steps = cookingSteps(
            technique = technique,
            ingredients = recipeIngredients,
            stoveType = stoveType,
            stoveMax = stoveMax,
            isTurkish = isTurkish
        )
        val safetyNotes = safetyNotes(recipeIngredients, isTurkish)
        return json.encodeToString(
            CookingPlanResponse(recipeName, servings, plannedIngredients, steps, safetyNotes)
        )
    }

    private fun prepInstruction(ingredients: List<LocalIngredient>, isTurkish: Boolean): String {
        fun namesFor(predicate: (LocalIngredient) -> Boolean): String =
            ingredients.filter(predicate).joinToString(", ") { it.name }

        val rice = namesFor { it.role == IngredientRole.RICE }
        val grainsAndLegumes = namesFor { it.role in setOf(IngredientRole.GRAIN, IngredientRole.LEGUME) }
        val produce = namesFor {
            it.role in setOf(
                IngredientRole.VEGETABLE,
                IngredientRole.LEAFY_GREEN,
                IngredientRole.AROMATIC,
                IngredientRole.FRUIT,
                IngredientRole.HERB
            )
        }
        val rawProteins = namesFor { it.role.isRawProtein }
        val handledRoles = setOf(
            IngredientRole.RICE,
            IngredientRole.GRAIN,
            IngredientRole.LEGUME,
            IngredientRole.VEGETABLE,
            IngredientRole.LEAFY_GREEN,
            IngredientRole.AROMATIC,
            IngredientRole.FRUIT,
            IngredientRole.HERB,
            IngredientRole.POULTRY,
            IngredientRole.RED_MEAT,
            IngredientRole.FISH,
            IngredientRole.SHELLFISH
        )
        val other = namesFor { it.role !in handledRoles }

        val clauses = mutableListOf<String>()
        if (isTurkish) {
            if (rice.isNotBlank()) clauses += "$rice: süzgeçte duru su akana kadar yıka ve süz."
            if (grainsAndLegumes.isNotBlank()) clauses += "$grainsAndLegumes: ayıkla; gerekiyorsa sudan geçirip süz."
            if (produce.isNotBlank()) clauses += "$produce: temizle; gerekiyorsa tarifte kullanacağın boyutta doğra."
            if (rawProteins.isNotBlank()) clauses += "$rawProteins: ayrı bir kesme tahtasında fazla nemini al; gerekiyorsa eşit parçalara ayır."
            if (other.isNotBlank()) clauses += "$other: tarifte kullanacağın miktarı hazır et."
            return clauses.joinToString(" ").ifBlank { "Malzemeleri tarifte kullanacağın şekilde hazırla." }
        }

        if (rice.isNotBlank()) clauses += "$rice: rinse in a sieve until the water runs mostly clear, then drain."
        if (grainsAndLegumes.isNotBlank()) clauses += "$grainsAndLegumes: sort through; rinse and drain if appropriate."
        if (produce.isNotBlank()) clauses += "$produce: clean and cut to the size needed for the recipe."
        if (rawProteins.isNotBlank()) clauses += "$rawProteins: pat dry on a separate cutting board and portion evenly if needed."
        if (other.isNotBlank()) clauses += "$other: measure out the amount needed and set it aside."
        return clauses.joinToString(" ").ifBlank { "Prepare the ingredients for the recipe." }
    }

    private fun cookingSteps(
        technique: Technique,
        ingredients: List<LocalIngredient>,
        stoveType: String,
        stoveMax: Int,
        isTurkish: Boolean
    ): List<CookingStepDto> {
        val primary = ingredients.firstOrNull { it.role.isPrincipal } ?: ingredients.first()
        val aromaticNames = ingredients.filter { it.role == IngredientRole.AROMATIC }.joinToString(", ") { it.name }
        val grain = ingredients.firstOrNull { it.role in stapleRoles }
        val vegetables = ingredients.filter { it.role in vegetableRoles }.joinToString(", ") { it.name }
        val steps = mutableListOf(
            CookingStepDto(
                "step_1",
                "prep",
                prepInstruction(ingredients, isTurkish),
                "counter",
                180
            )
        )

        when (technique) {
            Technique.SAUTE, Technique.STEW, Technique.WARM_BOWL, Technique.PASTA, Technique.SOUP -> {
                val gentlePower = if (stoveType == "electric") (stoveMax / 3).coerceAtLeast(1) else null
                val cookPower = if (stoveType == "electric") stoveMax.coerceAtLeast(1) else null
                val gentleHeat = heatText(stoveType, gentlePower, stoveMax, isTurkish, gentle = true)
                val cookHeat = heatText(stoveType, cookPower, stoveMax, isTurkish, gentle = false)
                if (aromaticNames.isNotBlank()) {
                    steps += CookingStepDto(
                        "step_2",
                        "cook",
                        if (isTurkish) {
                            "$aromaticNames malzemelerini $gentleHeat kokusu belirginleşip kenarları yumuşayana kadar 3 dakika çevir."
                        } else {
                            "Cook $aromaticNames over $gentleHeat for 3 minutes, until fragrant and softened at the edges."
                        },
                        "stove",
                        180,
                        powerLevel = gentlePower,
                        dependsOn = listOf("step_1")
                    )
                }
                val previous = steps.last().id
                steps += CookingStepDto(
                    "step_${steps.size + 1}",
                    "cook",
                    cookingInstruction(primary, grain, vegetables, technique, cookHeat, isTurkish),
                    "stove",
                    cookingDurationSeconds(primary, grain, technique),
                    powerLevel = cookPower,
                    dependsOn = listOf(previous)
                )
            }
            Technique.OVEN_TRAY, Technique.OVEN_PARCEL, Technique.OVEN_ROAST -> {
                steps += CookingStepDto(
                    "step_2",
                    "heat",
                    if (isTurkish) "Fırını 200°C'ye ısıt." else "Preheat the oven to 200°C.",
                    "oven",
                    480,
                    targetTemperatureC = 200,
                    dependsOn = listOf("step_1")
                )
                steps += CookingStepDto(
                    "step_3",
                    "cook",
                    ovenInstruction(primary, vegetables, technique, isTurkish),
                    "oven",
                    ovenDurationSeconds(primary),
                    targetTemperatureC = 200,
                    dependsOn = listOf("step_2")
                )
            }
            Technique.AIRFRYER_ROAST, Technique.AIRFRYER_CRISP, Technique.AIRFRYER_BOWL -> {
                steps += CookingStepDto(
                    "step_2",
                    "heat",
                    if (isTurkish) "Hava fritözünü 190°C'ye ısıt." else "Preheat the air fryer to 190°C.",
                    "airfryer",
                    240,
                    targetTemperatureC = 190,
                    dependsOn = listOf("step_1")
                )
                steps += CookingStepDto(
                    "step_3",
                    "cook",
                    airfryerInstruction(primary, vegetables, isTurkish),
                    "airfryer",
                    airfryerDurationSeconds(primary),
                    targetTemperatureC = 190,
                    dependsOn = listOf("step_2")
                )
            }
            Technique.COLD_SALAD, Technique.COLD_PLATE, Technique.OPEN_SANDWICH -> {
                steps += CookingStepDto(
                    "step_2",
                    "combine",
                    coldInstruction(technique, ingredients, isTurkish),
                    "bowl",
                    240,
                    dependsOn = listOf("step_1")
                )
            }
        }

        val previous = steps.last().id
        steps += CookingStepDto(
            "step_${steps.size + 1}",
            "rest",
            if (isTurkish) {
                "Tadını kontrol et; gerekiyorsa az miktarda tuz veya asit ekleyip 2 dakika dinlendir."
            } else {
                "Taste, adjust with a small amount of salt or acidity if needed, then rest for 2 minutes."
            },
            "counter",
            120,
            dependsOn = listOf(previous)
        )
        steps += CookingStepDto(
            "step_${steps.size + 1}",
            "serve",
            if (isTurkish) "Sıcaklığı ve dokuyu son kez kontrol edip servis et." else "Check the final temperature and texture, then serve.",
            "counter",
            60,
            dependsOn = listOf(steps.last().id)
        )
        return steps
    }

    private fun localAssistantResponse(prompt: String): String {
        val question = prompt.lineValue("Question:").ifBlank { prompt }.normalized()
        val context = GuidanceContext(
            recipeName = prompt.lineValue("Recipe:"),
            currentStep = prompt.lineValue("Current step:"),
            stoveType = prompt.lineValue("Stove type:"),
            isTurkish = prompt.lineValue("Language:").normalized() in setOf("turkce", "turkish")
        )
        val intent = guidanceIntents.firstOrNull { (_, phrases) -> phrases.any(question::contains) }?.first
        return guidanceResponse(intent, context)
    }

    private fun guidanceResponse(intent: GuidanceIntent?, context: GuidanceContext): String {
        val step = context.currentStep.ifBlank {
            if (context.isTurkish) "mevcut adım" else "the current step"
        }
        val recipe = context.recipeName.ifBlank {
            if (context.isTurkish) "bu tarif" else "this recipe"
        }
        val stove = when (context.stoveType) {
            "gas" -> if (context.isTurkish) "gaz alevini" else "the gas flame"
            "electric" -> if (context.isTurkish) "elektrikli ocak seviyesini" else "the electric hob level"
            else -> if (context.isTurkish) "ısıyı" else "the heat"
        }
        return if (context.isTurkish) {
            when (intent) {
                GuidanceIntent.THICK_SAUCE -> "Sosu 1 yemek kaşığı sıcak suyla açıp 30 saniye karıştır. Kıvamı kontrol etmeden daha fazla sıvı ekleme."
                GuidanceIntent.THIN_SAUCE -> "Sosu kapağı açık, düşük ısıda 1 dakika koyulaştır. Dibi tutmaması için karıştır ve her dakika yeniden kontrol et."
                GuidanceIntent.BURNING -> "Tavayı hemen ocaktan al ve ısıyı kapat; yanmamış kısmı temiz bir kaba aktar. Duman veya alev varsa su dökme, kapağı kapat."
                GuidanceIntent.HOT_PAN -> "Tavayı ısıdan 30 saniye uzaklaştır, sonra $stove bir kademe azalt. Yağ duman çıkarıyorsa yemeği eklemeden önce soğumasını bekle."
                GuidanceIntent.UNDERCOOKED -> "En kalın parçayı yeniden ısıtıp küçük aralıklarla pişir. Et, balık veya yumurtada güvenli iç sıcaklığı termometreyle doğrula."
                GuidanceIntent.OVERCOOKED -> "Isıyı kapatıp az miktarda su, sos veya yağ ekle ve hemen servis et. Yanmış veya güvenliğinden şüpheli kısmı kullanma."
                GuidanceIntent.SALTY -> "Tuzsuz malzeme veya 1 yemek kaşığı su ekleyip karıştır, sonra yeniden tat. Şeker ekleyerek tuzu gizlemeye çalışma."
                GuidanceIntent.SPICY -> "Acılığı yoğurt, süt ürünü veya uygun bir yağlı eşlikle dengele; önce az miktar ekle. Alerji ve beslenme tercihini kontrol et."
                GuidanceIntent.SUBSTITUTION -> "$recipe için benzer görevdeki malzemeyi önce yarım miktarla ekle, kıvam ve tadı kontrol ederek artır. Alerji ve beslenme uyumunu doğrula."
                GuidanceIntent.TIMING -> "$recipe içinde \"$step\" adımını dokusu ve güvenli iç sıcaklığı uygun olana kadar sürdür; yalnız süreye güvenme. Bir dakikalık aralıklarla kontrol et."
                GuidanceIntent.LOWER_HEAT -> "$stove bir kademe azalt ve 30 saniye gözle. Duman veya hızlı kararma sürerse tavayı geçici olarak ısıdan al."
                GuidanceIntent.RAISE_HEAT -> "$stove yalnız bir kademe artır ve 30 saniye gözle. Yağ duman çıkarmaya başlarsa hemen geri azalt."
                null -> "Çevrimdışı asistan $recipe için bunu tam olarak belirleyemiyor. \"$step\" adımını durdurup koku, doku ve güvenli sıcaklığı kontrol et; şüphedeysen ısıyı azalt."
            }
        } else {
            when (intent) {
                GuidanceIntent.THICK_SAUCE -> "Stir in 1 tablespoon of hot water for 30 seconds. Check the texture before adding more liquid."
                GuidanceIntent.THIN_SAUCE -> "Reduce the sauce uncovered over low heat for 1 minute. Stir to prevent sticking and check it each minute."
                GuidanceIntent.BURNING -> "Take the pan off the heat and turn the heat off; move the unburnt food to a clean pan. If there is smoke or flame, do not add water—cover it."
                GuidanceIntent.HOT_PAN -> "Move the pan off the heat for 30 seconds, then lower $stove by one level. If the oil is smoking, let it cool before adding food."
                GuidanceIntent.UNDERCOOKED -> "Return the thickest piece to the heat and cook in short intervals. For meat, fish, or eggs, verify a safe internal temperature with a thermometer."
                GuidanceIntent.OVERCOOKED -> "Turn off the heat, add a little water, sauce, or oil, and serve promptly. Discard burnt or questionable portions."
                GuidanceIntent.SALTY -> "Add an unsalted ingredient or 1 tablespoon of water, stir, then taste again. Do not try to hide excess salt with sugar."
                GuidanceIntent.SPICY -> "Balance the heat with yoghurt, another suitable dairy product, or a little fat. Add a small amount first and check allergies and diet."
                GuidanceIntent.SUBSTITUTION -> "For $recipe, add a similar-purpose substitute at half the amount first, then adjust after checking texture and taste. Confirm allergy and diet compatibility."
                GuidanceIntent.TIMING -> "For $recipe, continue \"$step\" until its texture and safe internal temperature are right; do not rely on time alone. Check at one-minute intervals."
                GuidanceIntent.LOWER_HEAT -> "Lower $stove by one level and watch for 30 seconds. If smoking or rapid browning continues, take the pan off the heat briefly."
                GuidanceIntent.RAISE_HEAT -> "Raise $stove by only one level and watch for 30 seconds. Lower it immediately if the oil starts to smoke."
                null -> "The offline assistant cannot determine that precisely for $recipe. Pause \"$step\" and check smell, texture, and safe temperature; lower the heat if unsure."
            }
        }
    }

    private fun invalidRequest(): ProviderFailure {
        val failure = ProviderFailure(PROVIDER_ID, ProviderFailureCategory.INVALID_REQUEST)
        diagnosticSink(ProviderDiagnostic(PROVIDER_ID, null, ProviderFailureCategory.INVALID_REQUEST.name, 0))
        return failure
    }

    private fun constraintConflict(): ProviderFailure {
        val failure = ProviderFailure(PROVIDER_ID, ProviderFailureCategory.CONSTRAINT_CONFLICT)
        diagnosticSink(ProviderDiagnostic(PROVIDER_ID, null, ProviderFailureCategory.CONSTRAINT_CONFLICT.name, 0))
        return failure
    }

    private fun checkConstraints(ingredients: List<LocalIngredient>, prompt: String) {
        val diet = prompt.lineValue("Diet:")
        val allergies = prompt.lineValue("Allergies:").csv().filterNot { it.equals("none", true) }
        if (ingredients.any { IngredientSafety.conflictsWithDiet(it.name, diet) }) throw constraintConflict()
        if (ingredients.any { ingredient -> allergies.any { IngredientSafety.conflictsWithAllergen(ingredient.name, it) } }) {
            throw constraintConflict()
        }
        if (ingredients.any { it.role.isRawProtein } && prompt.lineValue("Available equipment:").csv().none { it in heatEquipmentIds }) {
            throw constraintConflict()
        }
    }

    private fun availableTechniques(
        ingredients: List<LocalIngredient>,
        equipment: Set<String>
    ): List<Technique> = buildList {
        val hasStove = equipment.any(stoveEquipmentIds::contains)
        if (hasStove && ingredients.any { it.role == IngredientRole.PASTA }) add(Technique.PASTA)
        if (hasStove && ingredients.any { it.role == IngredientRole.LEGUME }) add(Technique.SOUP)
        if (hasStove) addAll(listOf(Technique.SAUTE, Technique.STEW, Technique.WARM_BOWL))
        if ("oven" in equipment) addAll(listOf(Technique.OVEN_TRAY, Technique.OVEN_PARCEL, Technique.OVEN_ROAST))
        if ("airfryer" in equipment) addAll(listOf(Technique.AIRFRYER_ROAST, Technique.AIRFRYER_CRISP, Technique.AIRFRYER_BOWL))
        if (ingredients.none { it.role.isRawProtein }) addAll(listOf(Technique.COLD_SALAD, Technique.COLD_PLATE, Technique.OPEN_SANDWICH))
    }.distinct()

    private fun recipeOption(
        index: Int,
        technique: Technique,
        ingredients: List<LocalIngredient>,
        equipment: Set<String>,
        isTurkish: Boolean
    ): RecipeOptionDto {
        val primary = ingredients.firstOrNull { it.role.isPrincipal } ?: ingredients.first()
        val secondary = ingredients.firstOrNull { it != primary && it.role in supportingRoles }
            ?: ingredients.firstOrNull { it != primary }
            ?: primary
        val title = technique.title(primary.name, secondary.name, isTurkish)
        val summary = technique.summary(primary.name, secondary.name, isTurkish)
        return RecipeOptionDto(
            id = "option_${index + 1}",
            name = title,
            summary = summary,
            difficulty = if (isTurkish) "kolay" else "easy",
            estimatedMinutes = technique.minutes,
            requiredEquipment = technique.requiredEquipment(equipment),
            missingIngredients = emptyList()
        )
    }

    private fun classifyIngredient(name: String): LocalIngredient {
        val catalog = catalogIngredientForName(name)
        val role = when (catalog?.categoryId) {
            "meat_poultry" -> if (
                catalog.id in setOf(
                    "chicken-breast",
                    "chicken-thigh",
                    "chicken-drumstick",
                    "chicken-wing",
                    "whole-chicken",
                    "turkey"
                )
            ) IngredientRole.POULTRY else IngredientRole.RED_MEAT
            "fish_seafood" -> if (catalog.id in setOf("prawns", "mussels", "squid", "octopus", "crab", "scallops")) IngredientRole.SHELLFISH else IngredientRole.FISH
            "eggs_dairy" -> if (catalog.id == "egg") IngredientRole.EGG else IngredientRole.DAIRY
            "vegetables", "roots_mushrooms" -> IngredientRole.VEGETABLE
            "greens_herbs" -> if (catalog.id in herbIds) IngredientRole.HERB else IngredientRole.LEAFY_GREEN
            "fruits_citrus" -> IngredientRole.FRUIT
            "grains_bread" -> when (catalog.id) {
                "rice" -> IngredientRole.RICE
                "pasta", "spaghetti", "noodles" -> IngredientRole.PASTA
                "bread", "pita", "tortilla", "breadcrumbs" -> IngredientRole.BREAD
                else -> IngredientRole.GRAIN
            }
            "legumes" -> IngredientRole.LEGUME
            "nuts_seeds" -> IngredientRole.NUT_SEED
            "spices_aromatics" -> if (catalog.id in setOf("onion", "garlic", "ginger")) IngredientRole.AROMATIC else IngredientRole.SPICE
            "oils_sauces" -> if (catalog.id in setOf("olive-oil", "sunflower-oil", "vegetable-oil")) IngredientRole.OIL else IngredientRole.SAUCE
            "baking_pantry" -> IngredientRole.BAKING
            else -> fallbackRole(name)
        }
        return LocalIngredient(name, catalog?.id, role)
    }

    private fun LocalIngredient.localized(isTurkish: Boolean): LocalIngredient {
        val catalog = id?.let(::catalogIngredientForName) ?: catalogIngredientForName(name)
        return if (catalog == null) this else copy(name = catalog.name(isTurkish))
    }

    private fun fallbackRole(name: String): IngredientRole {
        val value = name.normalized()
        return when {
            listOf("chicken", "tavuk", "turkey", "hindi").any(value::contains) -> IngredientRole.POULTRY
            listOf("fish", "balik", "salmon", "somon").any(value::contains) -> IngredientRole.FISH
            listOf("egg", "yumurta").any(value::contains) -> IngredientRole.EGG
            listOf("milk", "sut", "cheese", "peynir", "yogurt").any(value::contains) -> IngredientRole.DAIRY
            listOf("rice", "pirinc").any(value::contains) -> IngredientRole.RICE
            listOf("pasta", "makarna").any(value::contains) -> IngredientRole.PASTA
            listOf("lentil", "mercimek", "bean", "fasulye", "chickpea", "nohut").any(value::contains) -> IngredientRole.LEGUME
            listOf("onion", "sogan", "garlic", "sarimsak").any(value::contains) -> IngredientRole.AROMATIC
            listOf("oil", "yag").any(value::contains) -> IngredientRole.OIL
            else -> IngredientRole.GENERIC
        }
    }

    private fun ingredientsForRecipe(recipeName: String, ingredients: List<LocalIngredient>): List<LocalIngredient> {
        val normalizedName = recipeName.normalized()
        val namedPrincipals = ingredients.filter {
            it.role.isPrincipal && normalizedName.contains(it.name.normalized())
        }.toSet()
        return ingredients.filter { !it.role.isPrincipal || namedPrincipals.isEmpty() || it in namedPrincipals }
    }

    private fun plannedIngredient(ingredient: LocalIngredient, servings: Int): PlannedIngredientDto {
        val quantityAndUnit = when (ingredient.role) {
            IngredientRole.POULTRY, IngredientRole.RED_MEAT -> 150.0 * servings to "g"
            IngredientRole.FISH -> 160.0 * servings to "g"
            IngredientRole.SHELLFISH -> 140.0 * servings to "g"
            IngredientRole.EGG -> (servings * 1.5).coerceAtLeast(1.0) to "pieces"
            IngredientRole.DAIRY -> 35.0 * servings to "g"
            IngredientRole.VEGETABLE -> 100.0 * servings to "g"
            IngredientRole.LEAFY_GREEN -> 50.0 * servings to "g"
            IngredientRole.AROMATIC -> when (ingredient.id) {
                "garlic" -> servings.toDouble() to "clove"
                "onion" -> (servings / 2.0).coerceAtLeast(1.0) to "pieces"
                else -> 20.0 * servings to "g"
            }
            IngredientRole.FRUIT -> 80.0 * servings to "g"
            IngredientRole.GRAIN, IngredientRole.RICE, IngredientRole.PASTA, IngredientRole.LEGUME -> 75.0 * servings to "g"
            IngredientRole.BREAD -> servings.toDouble() to "slices"
            IngredientRole.NUT_SEED -> 15.0 * servings to "g"
            IngredientRole.HERB -> 5.0 * servings to "g"
            IngredientRole.SPICE -> (0.25 * servings).coerceAtLeast(0.5) to "tsp"
            IngredientRole.OIL -> (0.5 * servings).coerceAtLeast(1.0) to "tbsp"
            IngredientRole.SAUCE -> 15.0 * servings to "ml"
            IngredientRole.BAKING -> 50.0 * servings to "g"
            IngredientRole.GENERIC -> 80.0 * servings to "g"
        }
        return PlannedIngredientDto(ingredient.name, quantityAndUnit.first, quantityAndUnit.second)
    }

    private fun cookingInstruction(
        primary: LocalIngredient,
        grain: LocalIngredient?,
        vegetables: String,
        technique: Technique,
        heat: String,
        isTurkish: Boolean
    ): String {
        val additions = listOfNotNull(grain?.name, vegetables.takeIf(String::isNotBlank)).distinct().joinToString(", ")
        val duration = cookingDurationSeconds(primary, grain, technique) / 60
        val cue = donenessCue(primary.role, isTurkish)
        val stapleMethod = when (grain?.role) {
            IngredientRole.RICE, IngredientRole.GRAIN -> if (isTurkish) {
                "${grain.name} ve ölçüsünün iki katı sıcak suyu ekleyip kapağı kapat"
            } else {
                "add ${grain.name} with twice its volume of hot water and cover"
            }
            IngredientRole.LEGUME -> if (isTurkish) {
                "${grain.name} ve üzerini üç parmak geçecek sıcak suyu ekle"
            } else {
                "add ${grain.name} with enough hot water to cover generously"
            }
            IngredientRole.PASTA -> if (isTurkish) {
                "${grain.name} malzemesini bol suda diri kalacak şekilde pişirip süz"
            } else {
                "cook ${grain.name} in plenty of water until just tender, then drain"
            }
            else -> null
        }
        return if (isTurkish) {
            "${primary.name}${vegetables.takeIf(String::isNotBlank)?.let { " ve $it" }.orEmpty()} malzemelerini kısa süre çevir; " +
                "${stapleMethod ?: "${additions.ifBlank { primary.name }} malzemesini ekle"} ve $heat yaklaşık $duration dakika pişir; $cue."
        } else {
            "Turn ${primary.name}${vegetables.takeIf(String::isNotBlank)?.let { " with $it" }.orEmpty()} briefly; " +
                "${stapleMethod ?: "add ${additions.ifBlank { primary.name }}"} and cook over $heat for about $duration minutes; $cue."
        }
    }

    private fun ovenInstruction(
        primary: LocalIngredient,
        vegetables: String,
        technique: Technique,
        isTurkish: Boolean
    ): String {
        val method = when (technique) {
            Technique.OVEN_PARCEL -> if (isTurkish) "pişirme kâğıdında kapalı paket içinde" else "in a sealed parchment parcel"
            Technique.OVEN_ROAST -> if (isTurkish) "tek kat halinde" else "in a single layer"
            else -> if (isTurkish) "tepsiye yayarak" else "spread across a tray"
        }
        val cue = donenessCue(primary.role, isTurkish)
        return if (isTurkish) {
            "${primary.name}${vegetables.takeIf(String::isNotBlank)?.let { " ve $it" }.orEmpty()} malzemelerini $method 200°C'de pişir; $cue."
        } else {
            "Cook ${primary.name}${vegetables.takeIf(String::isNotBlank)?.let { " with $it" }.orEmpty()} $method at 200°C; $cue."
        }
    }

    private fun airfryerInstruction(primary: LocalIngredient, vegetables: String, isTurkish: Boolean): String {
        val cue = donenessCue(primary.role, isTurkish)
        return if (isTurkish) {
            "${primary.name}${vegetables.takeIf(String::isNotBlank)?.let { " ve $it" }.orEmpty()} malzemelerini 190°C'de, sepeti yarıda sallayarak pişir; $cue."
        } else {
            "Cook ${primary.name}${vegetables.takeIf(String::isNotBlank)?.let { " with $it" }.orEmpty()} at 190°C, shaking the basket halfway; $cue."
        }
    }

    private fun coldInstruction(technique: Technique, ingredients: List<LocalIngredient>, isTurkish: Boolean): String {
        val names = ingredients.joinToString(", ") { it.name }
        val method = when (technique) {
            Technique.OPEN_SANDWICH -> if (isTurkish) "ekmek dilimlerinin üzerine dengeli biçimde yerleştir" else "layer evenly over the bread"
            Technique.COLD_PLATE -> if (isTurkish) "sosla harmanlayıp soğuk tabağa yay" else "dress and arrange on a cold plate"
            else -> if (isTurkish) "geniş bir kasede nazikçe karıştır" else "toss gently in a wide bowl"
        }
        return if (isTurkish) "$names malzemelerini $method; taze ve gevrek dokuyu koru." else "$method $names, keeping the fresh ingredients crisp."
    }

    private fun cookingDurationSeconds(
        primary: LocalIngredient,
        grain: LocalIngredient?,
        technique: Technique
    ): Int {
        val principalDuration = when (primary.role) {
            IngredientRole.POULTRY -> 900
            IngredientRole.RED_MEAT -> 720
            IngredientRole.FISH -> 480
            IngredientRole.SHELLFISH, IngredientRole.EGG -> 300
            IngredientRole.LEGUME -> 1_200
            IngredientRole.PASTA -> 600
            IngredientRole.RICE, IngredientRole.GRAIN -> 900
            else -> 480
        }
        val stapleDuration = when (grain?.role) {
            IngredientRole.LEGUME -> 1_200
            IngredientRole.PASTA -> 600
            IngredientRole.RICE, IngredientRole.GRAIN -> 900
            else -> 0
        }
        return maxOf(principalDuration, stapleDuration).coerceAtLeast(if (technique == Technique.STEW) 900 else 300)
    }

    private fun ovenDurationSeconds(primary: LocalIngredient): Int = when (primary.role) {
        IngredientRole.POULTRY -> 1_800
        IngredientRole.RED_MEAT -> 1_500
        IngredientRole.FISH -> 900
        IngredientRole.SHELLFISH -> 600
        else -> 1_200
    }

    private fun airfryerDurationSeconds(primary: LocalIngredient): Int = when (primary.role) {
        IngredientRole.POULTRY -> 1_200
        IngredientRole.RED_MEAT -> 900
        IngredientRole.FISH -> 720
        IngredientRole.SHELLFISH -> 480
        else -> 900
    }

    private fun heatText(stoveType: String, power: Int?, stoveMax: Int, isTurkish: Boolean, gentle: Boolean): String =
        if (stoveType == "gas") {
            if (isTurkish) {
                if (gentle) "orta-kısık gaz ateşinde" else "orta gaz ateşinde"
            } else {
                if (gentle) "medium-low gas flame" else "medium gas flame"
            }
        } else if (isTurkish) {
            "elektrikli ocakta ${power ?: 1}/$stoveMax seviyede"
        } else {
            "electric hob level ${power ?: 1} of $stoveMax"
        }

    private fun donenessCue(role: IngredientRole, isTurkish: Boolean): String = when (role) {
        IngredientRole.POULTRY -> if (isTurkish) "en kalın kısmı 74°C'ye ulaşmalı ve içi pembe kalmamalı" else "the thickest part must reach 74°C with no pink centre"
        IngredientRole.RED_MEAT -> if (isTurkish) "merkezi seçtiğin pişme derecesine ulaşmalı; kıyma 71°C olmalı" else "the centre should reach your chosen doneness; minced meat must reach 71°C"
        IngredientRole.FISH -> if (isTurkish) "eti matlaşmalı, kolayca yapraklanmalı ve 63°C'ye ulaşmalı" else "the flesh should turn opaque, flake easily, and reach 63°C"
        IngredientRole.SHELLFISH -> if (isTurkish) "eti opak ve sıkı olmalı; kabuklular tamamen açılmalı" else "the flesh should be opaque and firm; discard shellfish that stay closed"
        IngredientRole.EGG -> if (isTurkish) "akı tamamen pişmeli" else "the white should be fully set"
        else -> if (isTurkish) "malzemeler yumuşamalı ama dağılmamalı" else "the ingredients should be tender without falling apart"
    }

    private fun safetyNotes(ingredients: List<LocalIngredient>, isTurkish: Boolean): List<String> =
        ingredients.filter { it.role.isRawProtein }.map { ingredient ->
            if (isTurkish) {
                "${ingredient.name} için ayrı kesme yüzeyi kullan; ${donenessCue(ingredient.role, true)}."
            } else {
                "Use a separate board for ${ingredient.name}; ${donenessCue(ingredient.role, false)}."
            }
        }.ifEmpty {
            listOf(if (isTurkish) "Servisten önce tat, sıcaklık ve tazeliği kontrol et." else "Check taste, temperature, and freshness before serving.")
        }

    private fun techniqueFromRecipeName(recipeName: String): Technique {
        val name = recipeName.normalized()
        return when {
            listOf("kagitta", "parchment").any(name::contains) -> Technique.OVEN_PARCEL
            listOf("firinlanmis", "with roasted").any(name::contains) -> Technique.OVEN_ROAST
            listOf("firinda", "roasted", "tray").any(name::contains) -> Technique.OVEN_TRAY
            listOf("citir", "crisp").any(name::contains) -> Technique.AIRFRYER_CRISP
            listOf("hava fritozu kasesi", "air fryer bowl").any(name::contains) -> Technique.AIRFRYER_BOWL
            listOf("hava fritoz", "air fried").any(name::contains) -> Technique.AIRFRYER_ROAST
            listOf("makarna", "pasta").any(name::contains) -> Technique.PASTA
            listOf("corba", "soup").any(name::contains) -> Technique.SOUP
            listOf("tencere", "stew").any(name::contains) -> Technique.STEW
            listOf("sicak kase", "warm").any(name::contains) -> Technique.WARM_BOWL
            listOf("salata", "salad").any(name::contains) -> Technique.COLD_SALAD
            listOf("soguk tabak", "cold").any(name::contains) -> Technique.COLD_PLATE
            listOf("acik sandvic", "open").any(name::contains) -> Technique.OPEN_SANDWICH
            else -> Technique.SAUTE
        }
    }

    private fun String.lineValue(prefix: String): String =
        lineSequence().firstOrNull { it.startsWith(prefix) }?.substringAfter(prefix)?.trim().orEmpty()

    private fun String.csv(): List<String> =
        split(',').map(String::trim).filter(String::isNotEmpty)

    private fun String.normalized(): String = Normalizer.normalize(
        lowercase(Locale.ROOT).replace('ı', 'i'),
        Normalizer.Form.NFD
    ).replace(Regex("""\p{Mn}+"""), "")

    private companion object {
        const val PROVIDER_ID = "FREE_LOCAL"
        const val MODEL = "offline-local"
        const val OPTIONS_MARKER = "Generate exactly 3 different recipe options"
        const val PLAN_MARKER = "Create a detailed cooking plan for \""
        const val SCAN_MARKER = "görsel açıklamasındaki yiyecek malzemelerini"
        val guidanceIntents = listOf(
            GuidanceIntent.BURNING to setOf("burning", "burnt", "yanıyor", "yaniyor", "yandı", "yandi"),
            GuidanceIntent.HOT_PAN to setOf("pan too hot", "pan is too hot", "tava çok sıcak", "tava cok sicak", "tava fazla sıcak", "tava fazla sicak"),
            GuidanceIntent.THICK_SAUCE to setOf("too thick", "çok koyu", "cok koyu", "fazla koyu"),
            GuidanceIntent.THIN_SAUCE to setOf("too thin", "watery", "çok sulu", "cok sulu", "fazla sulu"),
            GuidanceIntent.UNDERCOOKED to setOf("undercooked", "still raw", "az pişmiş", "az pismis", "çiğ", "cig"),
            GuidanceIntent.OVERCOOKED to setOf("overcooked", "too dry", "fazla pişmiş", "fazla pismis", "çok pişti", "cok pisti", "kurudu"),
            GuidanceIntent.SALTY to setOf("too salty", "çok tuzlu", "cok tuzlu", "fazla tuzlu"),
            GuidanceIntent.SPICY to setOf("too spicy", "çok acı", "cok aci", "fazla acı", "fazla aci"),
            GuidanceIntent.SUBSTITUTION to setOf("substitute", "replace", "instead of", "yerine", "değiştir", "degistir"),
            GuidanceIntent.TIMING to setOf("how long", "timing", "kaç dakika", "kac dakika", "ne kadar"),
            GuidanceIntent.LOWER_HEAT to setOf("reduce heat", "lower heat", "ısıyı azalt", "isiyi azalt", "ateşi kıs", "atesi kis"),
            GuidanceIntent.RAISE_HEAT to setOf("increase heat", "raise heat", "ısıyı artır", "isiyi artir", "ateşi artır", "atesi artir", "ateşi aç", "atesi ac")
        )
        val stoveEquipmentIds = setOf("elec", "gas", "camping")
        val heatEquipmentIds = stoveEquipmentIds + setOf("oven", "airfryer", "grill", "microwave")
        val herbIds = setOf("parsley", "dill", "mint", "basil", "coriander", "thyme", "rosemary", "oregano", "bay-leaf")
        val stapleRoles = setOf(IngredientRole.GRAIN, IngredientRole.RICE, IngredientRole.PASTA, IngredientRole.LEGUME)
        val vegetableRoles = setOf(IngredientRole.VEGETABLE, IngredientRole.LEAFY_GREEN)
        val supportingRoles = stapleRoles + vegetableRoles + setOf(IngredientRole.AROMATIC, IngredientRole.FRUIT)
        val json = Json { encodeDefaults = true }
    }
}

private data class GuidanceContext(
    val recipeName: String,
    val currentStep: String,
    val stoveType: String,
    val isTurkish: Boolean
)

private enum class GuidanceIntent {
    THICK_SAUCE,
    THIN_SAUCE,
    BURNING,
    HOT_PAN,
    UNDERCOOKED,
    OVERCOOKED,
    SALTY,
    SPICY,
    SUBSTITUTION,
    TIMING,
    LOWER_HEAT,
    RAISE_HEAT
}

private data class LocalIngredient(
    val name: String,
    val id: String?,
    val role: IngredientRole
)

private enum class IngredientRole {
    POULTRY,
    RED_MEAT,
    FISH,
    SHELLFISH,
    EGG,
    DAIRY,
    VEGETABLE,
    LEAFY_GREEN,
    AROMATIC,
    FRUIT,
    GRAIN,
    RICE,
    PASTA,
    BREAD,
    LEGUME,
    NUT_SEED,
    HERB,
    SPICE,
    OIL,
    SAUCE,
    BAKING,
    GENERIC;

    val isRawProtein get() = this in setOf(POULTRY, RED_MEAT, FISH, SHELLFISH)
    val isPrincipal get() = this in setOf(POULTRY, RED_MEAT, FISH, SHELLFISH, EGG, PASTA, RICE, GRAIN, LEGUME)
}

private enum class Technique(val minutes: Int) {
    SAUTE(30),
    STEW(40),
    WARM_BOWL(35),
    PASTA(30),
    SOUP(40),
    OVEN_TRAY(45),
    OVEN_PARCEL(35),
    OVEN_ROAST(40),
    AIRFRYER_ROAST(30),
    AIRFRYER_CRISP(25),
    AIRFRYER_BOWL(30),
    COLD_SALAD(15),
    COLD_PLATE(15),
    OPEN_SANDWICH(12);

    val isCold get() = this in setOf(COLD_SALAD, COLD_PLATE, OPEN_SANDWICH)

    fun isSupported(equipment: Set<String>): Boolean = when (this) {
        SAUTE, STEW, WARM_BOWL, PASTA, SOUP -> equipment.any { it in setOf("elec", "gas", "camping") }
        OVEN_TRAY, OVEN_PARCEL, OVEN_ROAST -> "oven" in equipment
        AIRFRYER_ROAST, AIRFRYER_CRISP, AIRFRYER_BOWL -> "airfryer" in equipment
        COLD_SALAD, COLD_PLATE, OPEN_SANDWICH -> true
    }

    fun requiredEquipment(equipment: Set<String>): List<String> = when (this) {
        SAUTE, STEW, WARM_BOWL, PASTA, SOUP -> listOfNotNull(equipment.firstOrNull { it in setOf("elec", "gas", "camping") })
        OVEN_TRAY, OVEN_PARCEL, OVEN_ROAST -> listOf("oven")
        AIRFRYER_ROAST, AIRFRYER_CRISP, AIRFRYER_BOWL -> listOf("airfryer")
        COLD_SALAD, COLD_PLATE, OPEN_SANDWICH -> emptyList()
    }

    fun title(primary: String, secondary: String, isTurkish: Boolean): String = if (isTurkish) {
        when (this) {
            SAUTE -> "$primary ve $secondary Tavası"
            STEW -> "$primary ile Sebzeli Tencere"
            WARM_BOWL -> "$secondary ve $primary Sıcak Kasesi"
            PASTA -> "$primary ile Makarna"
            SOUP -> "$primary ve $secondary Çorbası"
            OVEN_TRAY -> "Fırında $primary ve $secondary"
            OVEN_PARCEL -> "Kağıtta $primary ve $secondary"
            OVEN_ROAST -> "Fırınlanmış $secondary ile $primary"
            AIRFRYER_ROAST -> "Hava Fritözünde $primary ve $secondary"
            AIRFRYER_CRISP -> "Çıtır $primary ve $secondary"
            AIRFRYER_BOWL -> "Hava Fritözü $primary Kasesi"
            COLD_SALAD -> "$primary ve $secondary Salatası"
            COLD_PLATE -> "$primary ile Soğuk Tabak"
            OPEN_SANDWICH -> "$primary Açık Sandviçi"
        }
    } else {
        when (this) {
            SAUTE -> "$primary and $secondary Sauté"
            STEW -> "$primary Vegetable Stew"
            WARM_BOWL -> "Warm $secondary and $primary Bowl"
            PASTA -> "$primary Pasta"
            SOUP -> "$primary and $secondary Soup"
            OVEN_TRAY -> "Roasted $primary and $secondary Tray"
            OVEN_PARCEL -> "Parchment-Baked $primary and $secondary"
            OVEN_ROAST -> "$primary with Roasted $secondary"
            AIRFRYER_ROAST -> "Air-Fried $primary and $secondary"
            AIRFRYER_CRISP -> "Crisp $primary with $secondary"
            AIRFRYER_BOWL -> "Air-Fryer $primary Bowl"
            COLD_SALAD -> "$primary and $secondary Salad"
            COLD_PLATE -> "Cold $primary Plate"
            OPEN_SANDWICH -> "Open $primary Sandwich"
        }
    }

    fun summary(primary: String, secondary: String, isTurkish: Boolean): String = if (isTurkish) {
        when (this) {
            SAUTE -> "$primary ve $secondary ile tavada hızlıca hazırlanan pratik bir yemek."
            STEW -> "$primary ve eldeki sebzelerle hazırlanan sıcak, doyurucu bir tencere yemeği."
            WARM_BOWL -> "$primary ve $secondary ile hazırlanan sıcak, dengeli bir kase."
            PASTA -> "$primary ile hazırlanan, sosu makarnaya iyice geçen pratik bir tabak."
            SOUP -> "$primary ve $secondary ile hazırlanan sıcak, yumuşak içimli bir çorba."
            OVEN_TRAY -> "$primary ve $secondary tek tepside birlikte kızarıp lezzetlenir."
            OVEN_PARCEL -> "$primary ve $secondary pişirme kâğıdında kendi buharıyla yumuşak kalır."
            OVEN_ROAST -> "$primary, fırında kızaran $secondary ile dengeli bir tabak oluşturur."
            AIRFRYER_ROAST -> "$primary ve $secondary hava fritözünde az yağla kızarıp hazırlanır."
            AIRFRYER_CRISP -> "$primary dışı çıtır, içi yumuşak kalacak şekilde hazırlanır."
            AIRFRYER_BOWL -> "Hava fritözünde pişen $primary, taze $secondary ile sıcak bir kasede buluşur."
            COLD_SALAD -> "$primary ve $secondary ile ferah, taze bir salata hazırlanır."
            COLD_PLATE -> "$primary ve $secondary ile pişirmeden hazırlanan hafif bir soğuk tabak."
            OPEN_SANDWICH -> "$primary ve $secondary ile bol malzemeli açık bir sandviç hazırlanır."
        }
    } else {
        when (this) {
            SAUTE -> "A quick pan-cooked dish with $primary and $secondary."
            STEW -> "A warm, hearty stew with $primary and the vegetables you have on hand."
            WARM_BOWL -> "A balanced warm bowl built around $primary and $secondary."
            PASTA -> "A simple pasta dish with $primary and a well-coated sauce."
            SOUP -> "A warming soup built around $primary and $secondary."
            OVEN_TRAY -> "An even tray roast of $primary and $secondary."
            OVEN_PARCEL -> "A parchment bake that keeps $primary and $secondary moist."
            OVEN_ROAST -> "Roasted $secondary balanced with $primary."
            AIRFRYER_ROAST -> "A lightly oiled air-fryer roast of $primary and $secondary."
            AIRFRYER_CRISP -> "A crisp exterior and tender centre for $primary."
            AIRFRYER_BOWL -> "Air-fried $primary paired with fresh $secondary."
            COLD_SALAD -> "A fresh, bright salad of $primary and $secondary."
            COLD_PLATE -> "A dressed, no-cook plate of $primary and $secondary."
            OPEN_SANDWICH -> "A layered open sandwich using $primary and $secondary."
        }
    }
}
