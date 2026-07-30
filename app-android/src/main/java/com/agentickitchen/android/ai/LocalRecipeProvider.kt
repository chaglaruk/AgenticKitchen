package com.agentickitchen.android.ai

import com.agentickitchen.android.AppLogger
import com.agentickitchen.android.L
import com.agentickitchen.android.catalogIngredientForName
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.ai.dto.RecipeOptionDto
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import com.agentickitchen.shared.validator.IngredientSafety
import kotlinx.serialization.encodeToString
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
) : LlmProvider {

    constructor() : this(
        diagnosticSink = { AppLogger.i("AiProvider", it.asLogMessage()) }
    )

    override suspend fun generateContent(prompt: String): String {
        val response = when {
            OPTIONS_MARKER in prompt -> recipeOptions(prompt)
            PLAN_MARKER in prompt -> cookingPlan(prompt)
            SCAN_MARKER in prompt -> throw invalidRequest()
            else -> localAssistantResponse()
        }
        diagnosticSink(ProviderDiagnostic(PROVIDER_ID, null, "SUCCESS", response.length))
        return response
    }

    private fun recipeOptions(prompt: String): String {
        val ingredients = prompt.lineValue("Ingredients:").csv().map(::classifyIngredient)
        if (ingredients.isEmpty()) throw invalidRequest()
        val equipment = prompt.lineValue("Available equipment:").csv()
        val isTurkish = prompt.contains("Yanıtını Türkçe ver.")
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
        val ingredients = prompt.lineValue("Ingredients:").csv().map(::classifyIngredient)
        val equipment = prompt.lineValue("Available equipment:").csv().toSet()
        val servings = prompt.lineValue("Servings:").toIntOrNull()
        if (recipeName.isBlank() || ingredients.isEmpty() || servings == null || servings <= 0) {
            throw invalidRequest()
        }

        val isTurkish = prompt.contains("Yanıtını Türkçe ver.")
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

    private fun cookingSteps(
        technique: Technique,
        ingredients: List<LocalIngredient>,
        stoveType: String,
        stoveMax: Int,
        isTurkish: Boolean
    ): List<CookingStepDto> {
        val names = ingredients.joinToString(", ") { it.name }
        val primary = ingredients.firstOrNull { it.role.isPrincipal } ?: ingredients.first()
        val aromaticNames = ingredients.filter { it.role == IngredientRole.AROMATIC }.joinToString(", ") { it.name }
        val grain = ingredients.firstOrNull { it.role in stapleRoles }
        val vegetables = ingredients.filter { it.role in vegetableRoles }.joinToString(", ") { it.name }
        val steps = mutableListOf(
            CookingStepDto(
                "step_1",
                "prep",
                if (isTurkish) {
                    "$names malzemelerini yıka; ${primary.name} için gereken doğrama ve temizleme işlemlerini ayrı bir yüzeyde tamamla."
                } else {
                    "Wash $names; trim and prepare ${primary.name} on a separate clean surface."
                },
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

    private fun localAssistantResponse(): String = if (L.isTr) {
        "Mevcut adıma sadık kal, ısıyı kontrollü tut ve yemeğin görünümünü sık sık kontrol et."
    } else {
        "Stay with the current step, keep the heat controlled, and check the food often."
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
            "meat_poultry" -> if (catalog.id in setOf("chicken-breast", "chicken-thigh", "chicken-wing", "turkey")) IngredientRole.POULTRY else IngredientRole.RED_MEAT
            "fish_seafood" -> if (catalog.id in setOf("shrimp", "mussels", "squid", "octopus", "crab")) IngredientRole.SHELLFISH else IngredientRole.FISH
            "eggs_dairy" -> if (catalog.id == "egg") IngredientRole.EGG else IngredientRole.DAIRY
            "vegetables" -> IngredientRole.VEGETABLE
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
        const val OPTIONS_MARKER = "Generate exactly 3 different recipe options"
        const val PLAN_MARKER = "Create a detailed cooking plan for \""
        const val SCAN_MARKER = "görsel açıklamasındaki yiyecek malzemelerini"
        val stoveEquipmentIds = setOf("elec", "gas", "camping")
        val heatEquipmentIds = stoveEquipmentIds + setOf("oven", "airfryer", "grill", "microwave")
        val herbIds = setOf("parsley", "dill", "mint", "basil", "coriander", "thyme", "rosemary", "oregano", "bay-leaf")
        val stapleRoles = setOf(IngredientRole.GRAIN, IngredientRole.RICE, IngredientRole.PASTA, IngredientRole.LEGUME)
        val vegetableRoles = setOf(IngredientRole.VEGETABLE, IngredientRole.LEAFY_GREEN)
        val supportingRoles = stapleRoles + vegetableRoles + setOf(IngredientRole.AROMATIC, IngredientRole.FRUIT)
        val json = Json { encodeDefaults = true }
    }
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
            SAUTE -> "$primary malzemesini $secondary ile tavada katmanlı biçimde pişiren hızlı bir yemek."
            STEW -> "$primary ve eldeki sebzeleri yavaşça yumuşatan sulu bir tencere yemeği."
            WARM_BOWL -> "$primary, $secondary ve sıcak kiler malzemelerini ayrı dokularla buluşturan bir kase."
            PASTA -> "$primary ile sosu makarnaya bağlayan pratik bir tabak."
            SOUP -> "$primary ve $secondary malzemelerini yumuşak, sıcak bir çorbada birleştirir."
            OVEN_TRAY -> "$primary ve $secondary malzemelerini tek tepside eşitçe kızartır."
            OVEN_PARCEL -> "$primary ve $secondary aromalarını pişirme kâğıdında nemli tutar."
            OVEN_ROAST -> "$secondary malzemesini kızartıp $primary ile dengeler."
            AIRFRYER_ROAST -> "$primary ve $secondary malzemelerini az yağla kızartır."
            AIRFRYER_CRISP -> "$primary için çıtır dış, sulu iç doku hedefler."
            AIRFRYER_BOWL -> "Hava fritözünde pişen $primary ile taze $secondary malzemesini birleştirir."
            COLD_SALAD -> "$primary ve $secondary malzemelerini taze, asitli bir salatada birleştirir."
            COLD_PLATE -> "$primary ve $secondary için pişirmesiz, soslu bir soğuk tabak."
            OPEN_SANDWICH -> "$primary ve $secondary malzemelerini katmanlı bir açık sandviçte kullanır."
        }
    } else {
        when (this) {
            SAUTE -> "A quick layered sauté of $primary with $secondary."
            STEW -> "A gentle stovetop stew that softens $primary with the available vegetables."
            WARM_BOWL -> "A warm bowl that keeps $primary and $secondary in distinct textures."
            PASTA -> "A practical pasta dish that binds $primary into the sauce."
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
