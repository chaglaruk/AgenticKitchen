package com.agentickitchen.android.ai

import com.agentickitchen.android.AppLogger
import com.agentickitchen.android.L
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.ai.dto.RecipeOptionDto
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ProviderFailureCategory {
    HTTP,
    TIMEOUT,
    NETWORK,
    EMPTY_RESPONSE,
    INVALID_REQUEST
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
        val ingredients = prompt.lineValue("Ingredients:").csv()
        if (ingredients.isEmpty()) throw invalidRequest()
        val equipment = prompt.lineValue("Available equipment:").csv()
        val first = ingredients[0]
        val second = ingredients.getOrElse(1) { first }
        val third = ingredients.getOrElse(2) { second }
        val isTurkish = prompt.contains("Yanıtını Türkçe ver.")
        val options = if (isTurkish) {
            listOf(
                RecipeOptionDto("option_1", "$first ile Pratik Tava", "$first ve $second ile sade bir ev yemeği.", "kolay", 25, equipment.take(2), emptyList()),
                RecipeOptionDto("option_2", "$second Mutfağı Kasesi", "$second ve $third odaklı dengeli bir kase.", "kolay", 30, equipment.take(2), emptyList()),
                RecipeOptionDto("option_3", "$first, $second ve $third Sofrası", "Eldeki malzemeleri birlikte değerlendiren sakin bir tarif.", "orta", 35, equipment.take(2), emptyList())
            )
        } else {
            listOf(
                RecipeOptionDto("option_1", "Simple $first Skillet", "A straightforward home dish with $first and $second.", "easy", 25, equipment.take(2), emptyList()),
                RecipeOptionDto("option_2", "$second Kitchen Bowl", "A balanced bowl centred on $second and $third.", "easy", 30, equipment.take(2), emptyList()),
                RecipeOptionDto("option_3", "$first, $second and $third Supper", "A calm recipe that uses what is already available.", "medium", 35, equipment.take(2), emptyList())
            )
        }
        return json.encodeToString(RecipeOptionsResponse(options))
    }

    private fun cookingPlan(prompt: String): String {
        val recipeName = prompt.substringAfter(PLAN_MARKER, "").substringBefore('"').trim()
        val ingredients = prompt.lineValue("Ingredients:").csv()
        val equipment = prompt.lineValue("Available equipment:").csv().toSet()
        val servings = prompt.lineValue("Servings:").toIntOrNull()
        if (recipeName.isBlank() || ingredients.isEmpty() || servings == null || servings <= 0) {
            throw invalidRequest()
        }

        val isTurkish = prompt.contains("Yanıtını Türkçe ver.")
        val stoveType = prompt.lineValue("Stove type:").lowercase()
        val stoveMax = Regex("""Electric stove maximum level: (\d+)""")
            .find(prompt)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: 9
        val cookingResource = when {
            stoveType != "none" && equipment.any { it in setOf("elec", "gas", "camping") } -> "stove"
            "oven" in equipment -> "oven"
            "airfryer" in equipment -> "airfryer"
            else -> "counter"
        }
        val powerLevel = if (cookingResource == "stove" && stoveType == "electric") {
            stoveMax.coerceIn(1, 6)
        } else {
            null
        }
        val targetTemperature = if (cookingResource in setOf("oven", "airfryer")) 180 else null
        val plannedIngredients = ingredients.map {
            PlannedIngredientDto(it, (100.0 * servings).coerceAtMost(1_200.0), "g")
        }
        val steps = if (isTurkish) {
            cookingSteps(
                resource = cookingResource,
                powerLevel = powerLevel,
                targetTemperature = targetTemperature,
                prep = "Malzemeleri yıka ve pişirmeye hazırla.",
                cook = if (cookingResource == "counter") "Malzemeleri kasede dikkatlice birleştir." else "Malzemeleri kontrollü biçimde pişir.",
                rest = "Lezzetlerin dengelenmesi için kısa süre dinlendir.",
                serve = "Tabağa al ve servis et."
            )
        } else {
            cookingSteps(
                resource = cookingResource,
                powerLevel = powerLevel,
                targetTemperature = targetTemperature,
                prep = "Wash the ingredients and prepare them for cooking.",
                cook = if (cookingResource == "counter") "Combine the ingredients carefully in a bowl." else "Cook the ingredients with steady attention.",
                rest = "Rest briefly so the flavours settle.",
                serve = "Plate and serve."
            )
        }
        val safetyNote = if (isTurkish) {
            "Malzemelerin güvenli iç sıcaklığa ulaştığını kontrol et."
        } else {
            "Check that ingredients reach a safe internal temperature."
        }
        return json.encodeToString(
            CookingPlanResponse(recipeName, servings, plannedIngredients, steps, listOf(safetyNote))
        )
    }

    private fun cookingSteps(
        resource: String,
        powerLevel: Int?,
        targetTemperature: Int?,
        prep: String,
        cook: String,
        rest: String,
        serve: String
    ) = listOf(
        CookingStepDto("step_1", "prep", prep, "counter", 120),
        CookingStepDto("step_2", "cook", cook, resource, 600, targetTemperature, powerLevel, listOf("step_1")),
        CookingStepDto("step_3", "rest", rest, "counter", 120, dependsOn = listOf("step_2")),
        CookingStepDto("step_4", "serve", serve, "counter", 60, dependsOn = listOf("step_3"))
    )

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

    private fun String.lineValue(prefix: String): String =
        lineSequence().firstOrNull { it.startsWith(prefix) }?.substringAfter(prefix)?.trim().orEmpty()

    private fun String.csv(): List<String> =
        split(',').map(String::trim).filter(String::isNotEmpty)

    private companion object {
        const val PROVIDER_ID = "FREE_LOCAL"
        const val OPTIONS_MARKER = "Generate exactly 3 different recipe options"
        const val PLAN_MARKER = "Create a detailed cooking plan for \""
        const val SCAN_MARKER = "görsel açıklamasındaki yiyecek malzemelerini"
        val json = Json { encodeDefaults = true }
    }
}
