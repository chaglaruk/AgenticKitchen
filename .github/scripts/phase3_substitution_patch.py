from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(path: str, content: str):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")


# ── Shared typed AI contract ─────────────────────────────────────────────────
write("shared/src/main/kotlin/com/agentickitchen/shared/ai/SubstitutionModels.kt", r'''package com.agentickitchen.shared.ai

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlinx.serialization.Serializable

data class SubstitutionPlanRequest(
    val plan: CookingPlanResponse,
    val missingIngredientName: String,
    val inventoryLines: List<String>,
    val equipment: Set<String>,
    val stoveType: String,
    val stoveMaxLevel: Int,
    val ovenAvailable: Boolean,
    val ovenHasFan: Boolean,
    val airfryerAvailable: Boolean,
    val dietType: String,
    val allergies: Set<String>,
    val language: String
)

@Serializable
data class SubstitutionPlanResponse(
    val originalIngredientName: String,
    val replacementIngredient: PlannedIngredientDto,
    val reason: String,
    val confidence: Double,
    val mutatedPlan: CookingPlanResponse
)
''')

replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/ai/KitchenAiProvider.kt",
    '''    suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse>\n    suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse>\n''',
    '''    suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse>\n    suspend fun generateSubstitution(request: SubstitutionPlanRequest): AiResult<SubstitutionPlanResponse> =\n        AiResult.Failure(\n            AiFailureType.ProviderUnavailable,\n            retryable = false,\n            userMessage = AiFailureType.ProviderUnavailable.userMessageRes,\n            technicalMessage = "substitution_not_supported"\n        )\n    suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse>\n''',
    "KitchenAiProvider substitution contract"
)

# ── Prompt ────────────────────────────────────────────────────────────────────
replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/ai/prompt/PromptFactory.kt",
    '''object PromptFactory {\n''',
    '''import com.agentickitchen.shared.ai.SubstitutionPlanRequest\n\nobject PromptFactory {\n''',
    "PromptFactory import"
)
replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/ai/prompt/PromptFactory.kt",
    '''    fun visionAssessmentPrompt(\n''',
    r'''    fun substitutionPlanPrompt(request: SubstitutionPlanRequest): String {
        val langInstr = if (request.language == "Türkçe") "Write user-visible text in Turkish." else "Write user-visible text in English."
        val ingredients = request.plan.ingredients.joinToString("\n") {
            "- ${it.quantity} ${it.unit} ${it.name} [${it.canonicalIngredientId.orEmpty()}]"
        }
        val steps = request.plan.steps.joinToString("\n") {
            "- ${it.id} | ${it.type} | ${it.resource} | ${it.durationSeconds}s | temp=${it.targetTemperatureC} | power=${it.powerLevel} | depends=${it.dependsOn.joinToString(",")} | ${it.instruction}"
        }
        return """You are proposing ONE pantry-aware substitution for an already validated cooking plan.
Target missing ingredient: ${request.missingIngredientName}
Recipe: ${request.plan.recipeName}
Servings: ${request.plan.servings}

Current ingredients:
$ingredients

Current steps:
$steps

Available pantry quantities (replacement MUST come from this list and fit the stated quantity):
${request.inventoryLines.joinToString("\n")}

Equipment: ${request.equipment.joinToString(", ")}
Stove: ${request.stoveType}, max level ${request.stoveMaxLevel}
Oven available: ${request.ovenAvailable}; fan: ${request.ovenHasFan}
Airfryer available: ${request.airfryerAvailable}
Diet: ${request.dietType}
Allergies: ${request.allergies.joinToString(", ").ifBlank { "none" }}
$langInstr

Rules:
- Replace exactly the target ingredient identity with exactly one pantry ingredient.
- Do not silently change any other ingredient identity.
- Keep recipeName, servings, and the complete set of step IDs unchanged.
- You MAY adjust quantities, instructions, step durations, resource, temperature, power level, and dependencies only when required by the substitution.
- The replacement must not introduce a diet or allergen conflict.
- Do not invent pantry stock, equipment, steps, or ingredients.
- If no safe pantry replacement exists, do not fabricate one; return a structurally valid response with confidence 0 and the original ingredient as replacement so the app will reject it fail-closed.
- mutatedPlan must be a complete plan, not a patch.
Return only JSON matching the substitution schema."""
    }

    fun visionAssessmentPrompt(
''',
    "substitution plan prompt"
)

# ── Structural mutation validator ─────────────────────────────────────────────
write("shared/src/main/kotlin/com/agentickitchen/shared/inventory/SubstitutionMutation.kt", r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.SubstitutionPlanResponse
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto

data class SubstitutionMutationCheck(
    val valid: Boolean,
    val errors: List<String>
)

object SubstitutionMutationValidator {
    fun validate(
        before: CookingPlanResponse,
        targetIngredientName: String,
        response: SubstitutionPlanResponse
    ): SubstitutionMutationCheck {
        val errors = mutableListOf<String>()
        val after = response.mutatedPlan
        val target = before.ingredients.singleOrNull { matches(it, targetIngredientName, null) }
        if (target == null) errors += "target_not_unique_or_missing"
        if (!LocalIngredientResolver.matches(
                response.originalIngredientName, null,
                targetIngredientName, target?.canonicalIngredientId
            )) errors += "response_original_mismatch"
        if (response.reason.isBlank()) errors += "reason_blank"
        if (!response.confidence.isFinite() || response.confidence <= 0.0 || response.confidence > 1.0) errors += "confidence_invalid"
        if (response.replacementIngredient.name.isBlank() || response.replacementIngredient.quantity <= 0.0) errors += "replacement_invalid"
        if (target != null && matches(response.replacementIngredient, target.name, target.canonicalIngredientId)) {
            errors += "replacement_same_as_original"
        }
        if (after.recipeName != before.recipeName) errors += "recipe_identity_changed"
        if (after.servings != before.servings) errors += "servings_changed"
        if (after.ingredients.size != before.ingredients.size) errors += "ingredient_count_changed"

        val beforeStepIds = before.steps.map { it.id }
        val afterStepIds = after.steps.map { it.id }
        if (beforeStepIds.size != beforeStepIds.toSet().size || afterStepIds.size != afterStepIds.toSet().size) {
            errors += "duplicate_step_id"
        }
        if (beforeStepIds.toSet() != afterStepIds.toSet()) errors += "step_identity_changed"

        if (target != null) {
            val unchanged = before.ingredients.filterNot { it === target }
            unchanged.forEach { ingredient ->
                if (after.ingredients.none { matches(it, ingredient.name, ingredient.canonicalIngredientId) }) {
                    errors += "unrelated_ingredient_identity_changed"
                }
            }
            val replacementCount = after.ingredients.count {
                matches(
                    it,
                    response.replacementIngredient.name,
                    response.replacementIngredient.canonicalIngredientId
                )
            }
            if (replacementCount != 1) errors += "replacement_not_unique"
            val targetStillPresent = after.ingredients.any { matches(it, target.name, target.canonicalIngredientId) }
            if (targetStillPresent) errors += "original_still_present"
        }

        return SubstitutionMutationCheck(errors.isEmpty(), errors.distinct())
    }

    private fun matches(
        ingredient: PlannedIngredientDto,
        otherName: String,
        otherCanonicalId: String?
    ): Boolean = LocalIngredientResolver.matches(
        firstName = ingredient.name,
        firstCanonicalId = ingredient.canonicalIngredientId,
        secondName = otherName,
        secondCanonicalId = otherCanonicalId
    )
}
''')

write("shared/src/test/kotlin/com/agentickitchen/shared/inventory/SubstitutionMutationValidatorTest.kt", r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.SubstitutionPlanResponse
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubstitutionMutationValidatorTest {
    private val before = CookingPlanResponse(
        recipeName = "Onion rice",
        servings = 2,
        ingredients = listOf(
            PlannedIngredientDto("Rice", 160.0, "g", "rice"),
            PlannedIngredientDto("Onion", 1.0, "piece", "onion")
        ),
        steps = listOf(
            CookingStepDto("prep", "prep", "Chop onion", "knife", 60),
            CookingStepDto("cook", "cook", "Cook rice and onion", "pot", 900, dependsOn = listOf("prep"))
        ),
        safetyNotes = emptyList()
    )

    @Test fun acceptsOneForOneIdentityMutationWithStepAdjustments() {
        val after = before.copy(
            ingredients = listOf(
                before.ingredients[0],
                PlannedIngredientDto("Garlic", 2.0, "clove", "garlic")
            ),
            steps = listOf(
                before.steps[0].copy(instruction = "Mince garlic", durationSeconds = 45),
                before.steps[1].copy(instruction = "Cook rice and garlic", durationSeconds = 840)
            )
        )
        val response = SubstitutionPlanResponse(
            "Onion",
            PlannedIngredientDto("Garlic", 2.0, "clove", "garlic"),
            "Garlic is available and changes the aromatic profile.",
            .8,
            after
        )
        assertTrue(SubstitutionMutationValidator.validate(before, "Onion", response).valid)
    }

    @Test fun rejectsUnrelatedIngredientIdentityChange() {
        val after = before.copy(ingredients = listOf(
            PlannedIngredientDto("Pasta", 160.0, "g", "pasta"),
            PlannedIngredientDto("Garlic", 2.0, "clove", "garlic")
        ))
        val response = SubstitutionPlanResponse(
            "Onion", PlannedIngredientDto("Garlic", 2.0, "clove", "garlic"), "reason", .8, after
        )
        assertFalse(SubstitutionMutationValidator.validate(before, "Onion", response).valid)
    }

    @Test fun rejectsChangedStepIdentityOrRecipeIdentity() {
        val after = before.copy(
            recipeName = "Different recipe",
            ingredients = listOf(before.ingredients[0], PlannedIngredientDto("Garlic", 2.0, "clove", "garlic")),
            steps = listOf(before.steps[0].copy(id = "new_step"), before.steps[1])
        )
        val response = SubstitutionPlanResponse(
            "Onion", PlannedIngredientDto("Garlic", 2.0, "clove", "garlic"), "reason", .8, after
        )
        assertFalse(SubstitutionMutationValidator.validate(before, "Onion", response).valid)
    }
}
''')

# ── Firebase structured schema + provider ─────────────────────────────────────
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/FirebaseResponseSchemas.kt",
    '''    COOKING_PLAN(FirebaseAiTask.REASONING, FirebaseResponseSchemas.cookingPlan),\n    SHOPPING_IMPORT''',
    '''    COOKING_PLAN(FirebaseAiTask.REASONING, FirebaseResponseSchemas.cookingPlan),\n    SUBSTITUTION_PLAN(FirebaseAiTask.REASONING, FirebaseResponseSchemas.substitutionPlan),\n    SHOPPING_IMPORT''',
    "Firebase response kind"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/FirebaseResponseSchemas.kt",
    '''    val shoppingImport = Schema.obj(\n''',
    '''    val substitutionPlan = Schema.obj(\n        properties = mapOf(\n            "originalIngredientName" to Schema.string(),\n            "replacementIngredient" to plannedIngredient,\n            "reason" to Schema.string(),\n            "confidence" to Schema.double(),\n            "mutatedPlan" to cookingPlan\n        )\n    )\n\n    val shoppingImport = Schema.obj(\n''',
    "Firebase substitution schema"
)

replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/FirebaseAiProvider.kt",
    '''import com.agentickitchen.shared.ai.ShoppingTextRequest\n''',
    '''import com.agentickitchen.shared.ai.ShoppingTextRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanResponse\n''',
    "Firebase substitution imports"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/FirebaseAiProvider.kt",
    '''    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =\n''',
    '''    override suspend fun generateSubstitution(request: SubstitutionPlanRequest): AiResult<SubstitutionPlanResponse> =\n        structured(\n            kind = FirebaseResponseKind.SUBSTITUTION_PLAN,\n            prompt = PromptFactory.substitutionPlanPrompt(request),\n            decode = json::decodeFromString,\n            validate = { response ->\n                response.originalIngredientName.isNotBlank() &&\n                    response.replacementIngredient.name.isNotBlank() &&\n                    response.replacementIngredient.quantity.isFinite() &&\n                    response.replacementIngredient.quantity > 0.0 &&\n                    response.reason.isNotBlank() &&\n                    response.confidence.isFinite() && response.confidence in 0.0..1.0 &&\n                    response.mutatedPlan.ingredients.isNotEmpty() && response.mutatedPlan.steps.isNotEmpty()\n            }\n        )\n\n    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =\n''',
    "Firebase substitution method"
)

# ── Gemini structured provider ────────────────────────────────────────────────
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt",
    '''import com.agentickitchen.shared.ai.ShoppingTextRequest\n''',
    '''import com.agentickitchen.shared.ai.ShoppingTextRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanResponse\n''',
    "Gemini substitution imports"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt",
    '''    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =\n''',
    '''    override suspend fun generateSubstitution(request: SubstitutionPlanRequest): AiResult<SubstitutionPlanResponse> =\n        structured(\n            feature = "substitution_plan",\n            prompt = PromptFactory.substitutionPlanPrompt(request),\n            schema = substitutionPlanSchema,\n            decode = json::decodeFromString,\n            validate = { response ->\n                response.originalIngredientName.isNotBlank() &&\n                    response.replacementIngredient.name.isNotBlank() &&\n                    response.replacementIngredient.quantity.isFinite() &&\n                    response.replacementIngredient.quantity > 0.0 &&\n                    response.reason.isNotBlank() &&\n                    response.confidence.isFinite() && response.confidence in 0.0..1.0 &&\n                    response.mutatedPlan.ingredients.isNotEmpty() && response.mutatedPlan.steps.isNotEmpty()\n            }\n        )\n\n    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =\n''',
    "Gemini substitution method"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt",
    '''        private val shoppingSchema = schema(\n''',
    '''        private val substitutionPlanSchema = schema(\n            """{"type":"object","properties":{"originalIngredientName":{"type":"string"},"replacementIngredient":{"type":"object","properties":{"name":{"type":"string"},"quantity":{"type":"number"},"unit":{"type":"string"},"canonicalIngredientId":{"type":["string","null"]}},"required":["name","quantity","unit"]},"reason":{"type":"string"},"confidence":{"type":"number"},"mutatedPlan":{"type":"object","properties":{"recipeName":{"type":"string"},"servings":{"type":"integer"},"ingredients":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"quantity":{"type":"number"},"unit":{"type":"string"},"canonicalIngredientId":{"type":["string","null"]}},"required":["name","quantity","unit"]}},"steps":{"type":"array","items":{"type":"object","properties":{"id":{"type":"string"},"type":{"type":"string"},"instruction":{"type":"string"},"resource":{"type":"string"},"durationSeconds":{"type":"integer"},"targetTemperatureC":{"type":["integer","null"]},"powerLevel":{"type":["integer","null"]},"dependsOn":{"type":"array","items":{"type":"string"}},"visionCheckpointRecommended":{"type":"boolean"}},"required":["id","type","instruction","resource","durationSeconds","dependsOn","visionCheckpointRecommended"]}},"safetyNotes":{"type":"array","items":{"type":"string"}}},"required":["recipeName","servings","ingredients","steps","safetyNotes"]}},"required":["originalIngredientName","replacementIngredient","reason","confidence","mutatedPlan"]}"""\n        )\n        private val shoppingSchema = schema(\n''',
    "Gemini substitution schema"
)

# wrappers must preserve provider path
for path in [
    "app-android/src/main/java/com/agentickitchen/android/ai/InventoryAwareOfflineProvider.kt",
    "app-android/src/main/java/com/agentickitchen/android/ai/SafetyEnforcingAiProvider.kt",
]:
    replace_once(path, '''import com.agentickitchen.shared.ai.ShoppingTextRequest\n''', '''import com.agentickitchen.shared.ai.ShoppingTextRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanResponse\n''', f"{path} substitution imports")
    replace_once(path, '''    override suspend fun parseShoppingText(request: ShoppingTextRequest)''', '''    override suspend fun generateSubstitution(request: SubstitutionPlanRequest): AiResult<SubstitutionPlanResponse> =\n        delegate.generateSubstitution(request)\n\n    override suspend fun parseShoppingText(request: ShoppingTextRequest)''', f"{path} substitution delegate")

replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/ResilientGeminiProvider.kt",
    '''import com.agentickitchen.shared.ai.ShoppingTextRequest\n''',
    '''import com.agentickitchen.shared.ai.ShoppingTextRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanRequest\n''',
    "Resilient substitution import"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ai/ResilientGeminiProvider.kt",
    '''    override suspend fun parseShoppingText(request: ShoppingTextRequest) =\n''',
    '''    override suspend fun generateSubstitution(request: SubstitutionPlanRequest) =\n        primary.generateSubstitution(request)\n\n    override suspend fun parseShoppingText(request: ShoppingTextRequest) =\n''',
    "Resilient substitution delegate"
)

# ── ViewModel state + request/review/apply ───────────────────────────────────
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''import com.agentickitchen.shared.ai.ShoppingCandidate\n''',
    '''import com.agentickitchen.shared.ai.ShoppingCandidate\nimport com.agentickitchen.shared.ai.SubstitutionPlanRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanResponse\n''',
    "ViewModel substitution AI imports"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''import com.agentickitchen.shared.inventory.RecipeMatchTier\n''',
    '''import com.agentickitchen.shared.inventory.RecipeMatchTier\nimport com.agentickitchen.shared.inventory.SubstitutionMutationValidator\n''',
    "ViewModel mutation validator import"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''sealed class PlanState {\n''',
    r'''sealed interface SubstitutionState {
    data object Idle : SubstitutionState
    data class Loading(val originalIngredientName: String) : SubstitutionState
    data class Review(
        val originalIngredientName: String,
        val response: SubstitutionPlanResponse,
        val remainingShortages: List<String>
    ) : SubstitutionState
    data class Error(val originalIngredientName: String, val message: String) : SubstitutionState
}

sealed class PlanState {
''',
    "Substitution UI state"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''        val plannedUsage: List<PlannedPantryUsage> = emptyList(),\n        val agentChatResponse: String? = null,\n''',
    '''        val plannedUsage: List<PlannedPantryUsage> = emptyList(),\n        val shortages: List<String> = emptyList(),\n        val substitutionState: SubstitutionState = SubstitutionState.Idle,\n        val agentChatResponse: String? = null,\n''',
    "RecipeActive substitution fields"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''    plan: CookingPlanResponse,\n    plannedUsage: List<PlannedPantryUsage> = emptyList()\n) = PlanState.RecipeActive(\n''',
    '''    plan: CookingPlanResponse,\n    plannedUsage: List<PlannedPantryUsage> = emptyList(),\n    shortages: List<String> = emptyList()\n) = PlanState.RecipeActive(\n''',
    "activeRecipeState signature"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''    cookingPlan = plan,\n    plannedUsage = plannedUsage\n)\n''',
    '''    cookingPlan = plan,\n    plannedUsage = plannedUsage,\n    shortages = shortages\n)\n''',
    "activeRecipeState fields"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''                        plan,\n                        usagePlan.usages\n                    )\n''',
    '''                        plan,\n                        usagePlan.usages,\n                        usagePlan.shortages\n                    )\n''',
    "initial active shortages"
)

# add substitution methods immediately before startCooking
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''    fun startCooking() {\n''',
    r'''    fun requestPantrySubstitution(originalIngredientName: String) {
        val active = _planState.value as? PlanState.RecipeActive ?: return
        val plan = active.cookingPlan ?: return
        if (_cookingState.value.status != CookingSessionStatus.READY) {
            emitUiEvent(if (L.isTr) "Değişiklik yalnızca pişirme başlamadan önce yapılabilir." else "Substitutions can only be applied before cooking starts.")
            return
        }
        if (active.shortages.none { shortage ->
                LocalIngredientResolver.matches(shortage, null, originalIngredientName, null)
            }) {
            emitUiEvent(if (L.isTr) "Bu malzeme mevcut planın eksiklerinden biri değil." else "That ingredient is not a current plan shortage.")
            return
        }
        val provider = CookingProviderSelection.provider(aiProviderFactory, _hardwareSettings.value)
        if (provider == null) {
            emitUiEvent(if (L.isTr) "Güvenli değişiklik önerisi şu anda kullanılamıyor." else "A safe substitution suggestion is not available right now.")
            return
        }
        _planState.value = active.copy(substitutionState = SubstitutionState.Loading(originalIngredientName))
        viewModelScope.launch {
            try {
                val hw = _hardwareSettings.value
                val result = provider.generateSubstitution(
                    SubstitutionPlanRequest(
                        plan = plan,
                        missingIngredientName = originalIngredientName,
                        inventoryLines = _inventory.value.map { "${it.quantity} ${it.unit} ${it.originalName}" },
                        equipment = _selectedEquipment.value,
                        stoveType = hw.stoveType,
                        stoveMaxLevel = hw.stovePowerMax,
                        ovenAvailable = hw.ovenAvailable,
                        ovenHasFan = hw.ovenHasFan,
                        airfryerAvailable = _selectedEquipment.value.contains("airfryer"),
                        dietType = dietSettings.value.dietType,
                        allergies = dietSettings.value.allergies,
                        language = _language.value
                    )
                ).requireValue()
                val current = _planState.value as? PlanState.RecipeActive ?: return@launch
                if (current.sessionId != active.sessionId || _cookingState.value.status != CookingSessionStatus.READY) return@launch
                val structural = SubstitutionMutationValidator.validate(plan, originalIngredientName, result)
                if (!structural.valid) throw ProviderFailure("SUBSTITUTION", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                val validation = CookingPlanValidator(
                    _selectedEquipment.value,
                    hw.stovePowerMax,
                    hw.stoveType,
                    hw.ovenAvailable,
                    _selectedEquipment.value.contains("airfryer"),
                    dietSettings.value.dietType,
                    dietSettings.value.allergies,
                    current.servings
                ).validate(result.mutatedPlan)
                if (!validation.valid) throw PlanValidationException(validation.errors)
                val usage = InventoryWorkflow.planUsage(result.mutatedPlan, _inventory.value, reservedQuantities())
                if (usage.shortages.size >= current.shortages.size || usage.shortages.any {
                        LocalIngredientResolver.matches(it, null, originalIngredientName, null)
                    }) {
                    throw ProviderFailure("SUBSTITUTION", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                }
                _planState.value = current.copy(
                    substitutionState = SubstitutionState.Review(originalIngredientName, result, usage.shortages)
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val current = _planState.value as? PlanState.RecipeActive ?: return@launch
                if (current.sessionId != active.sessionId) return@launch
                val message = if (error is AiRequestException && error.failure.technicalMessage == "substitution_not_supported") {
                    if (L.isTr) "Çevrimdışı sağlayıcı bu değişikliği güvenle öneremiyor." else "The offline provider cannot safely propose this substitution."
                } else {
                    readerSafeAiError(error)
                }
                _planState.value = current.copy(
                    substitutionState = SubstitutionState.Error(originalIngredientName, message)
                )
            }
        }
    }

    fun dismissPantrySubstitution() {
        val active = _planState.value as? PlanState.RecipeActive ?: return
        _planState.value = active.copy(substitutionState = SubstitutionState.Idle)
    }

    fun applyPantrySubstitution() {
        val active = _planState.value as? PlanState.RecipeActive ?: return
        val review = active.substitutionState as? SubstitutionState.Review ?: return
        val before = active.cookingPlan ?: return
        if (_cookingState.value.status != CookingSessionStatus.READY) {
            emitUiEvent(if (L.isTr) "Pişirme başladıktan sonra plan değiştirilemez." else "The plan cannot be changed after cooking starts.")
            return
        }
        viewModelScope.launch {
            try {
                val hw = _hardwareSettings.value
                val response = review.response
                val structural = SubstitutionMutationValidator.validate(before, review.originalIngredientName, response)
                if (!structural.valid) throw ProviderFailure("SUBSTITUTION", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                val validation = CookingPlanValidator(
                    _selectedEquipment.value,
                    hw.stovePowerMax,
                    hw.stoveType,
                    hw.ovenAvailable,
                    _selectedEquipment.value.contains("airfryer"),
                    dietSettings.value.dietType,
                    dietSettings.value.allergies,
                    active.servings
                ).validate(response.mutatedPlan)
                if (!validation.valid) throw PlanValidationException(validation.errors)
                val usage = InventoryWorkflow.planUsage(response.mutatedPlan, _inventory.value, reservedQuantities())
                if (usage.shortages.size >= active.shortages.size || usage.shortages.any {
                        LocalIngredientResolver.matches(it, null, review.originalIngredientName, null)
                    }) {
                    throw ProviderFailure("SUBSTITUTION", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                }
                val session = RecipeSession(
                    active.sessionId,
                    active.resolvedReadyTimeIso,
                    response.mutatedPlan.ingredients.map { IngredientAmount(slugify(it.name), quantityToGrams(it.quantity, it.unit)) },
                    "kitchen",
                    response.mutatedPlan.steps.map {
                        RecipeStep(it.id, it.type, it.resource, it.targetTemperatureC, it.durationSeconds, it.instruction, it.dependsOn)
                    }
                )
                val schedule = orchestrator.startSession(session)
                _planState.value = active.copy(
                    recipe = active.recipe.copy(
                        proposedIngredients = response.mutatedPlan.ingredients,
                        shortages = usage.shortages
                    ),
                    events = schedule.events,
                    cookingPlan = response.mutatedPlan,
                    plannedUsage = usage.usages,
                    shortages = usage.shortages,
                    substitutionState = SubstitutionState.Idle
                )
                emitUiEvent(if (L.isTr) "Değişiklik plana uygulandı ve zamanlama yeniden hesaplandı." else "Substitution applied and the schedule was recalculated.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val current = _planState.value as? PlanState.RecipeActive ?: return@launch
                _planState.value = current.copy(
                    substitutionState = SubstitutionState.Error(review.originalIngredientName, readerSafeAiError(error))
                )
            }
        }
    }

    fun startCooking() {
''',
    "ViewModel substitution methods"
)

# ── Operations UI + wiring ────────────────────────────────────────────────────
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''import com.agentickitchen.android.RecipeOption\n''',
    '''import com.agentickitchen.android.RecipeOption\nimport com.agentickitchen.android.SubstitutionState\n''',
    "Operations substitution import"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''    onCancelConsumption: () -> Unit = {}\n) {\n''',
    '''    onCancelConsumption: () -> Unit = {},\n    onRequestSubstitution: (String) -> Unit = {},\n    onApplySubstitution: () -> Unit = {},\n    onDismissSubstitution: () -> Unit = {}\n) {\n''',
    "Operations substitution callbacks"
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''            is PlanState.RecipeActive -> {\n                Spacer(Modifier.height(24.dp))\n                KitchenAssistantSection(\n''',
    '''            is PlanState.RecipeActive -> {\n                if (cookingState.status == CookingSessionStatus.READY && planState.shortages.isNotEmpty()) {\n                    Spacer(Modifier.height(20.dp))\n                    PantrySubstitutionSection(\n                        state = planState,\n                        onRequest = onRequestSubstitution,\n                        onApply = onApplySubstitution,\n                        onDismiss = onDismissSubstitution\n                    )\n                }\n                Spacer(Modifier.height(24.dp))\n                KitchenAssistantSection(\n''',
    "Operations substitution section"
)
# insert composable before duration formatter
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''internal fun formatCookingDuration(totalSeconds: Long): String {\n''',
    r'''@Composable
private fun PantrySubstitutionSection(
    state: PlanState.RecipeActive,
    onRequest: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        elevation = 0.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(if (L.isTr) "STOKTAN DEĞİŞTİR" else "SUBSTITUTE FROM PANTRY", color = colors.primary, style = MaterialTheme.typography.overline)
            Spacer(Modifier.height(6.dp))
            Text(
                if (L.isTr) "Eksik malzemeyi, stoktaki güvenli bir alternatifle planın tamamında değiştirebilirsin." else "Replace a shortage with a safe pantry alternative across the whole cooking plan.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body2
            )
            Spacer(Modifier.height(12.dp))
            state.shortages.forEach { shortage ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(shortage, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onRequest(shortage) }) {
                        Text(if (L.isTr) "Alternatif bul" else "Find substitute", color = colors.primary)
                    }
                }
            }
            when (val substitution = state.substitutionState) {
                SubstitutionState.Idle -> Unit
                is SubstitutionState.Loading -> {
                    Divider(color = colors.divider)
                    Spacer(Modifier.height(10.dp))
                    Text(if (L.isTr) "${substitution.originalIngredientName} için güvenli alternatif aranıyor…" else "Finding a safe substitute for ${substitution.originalIngredientName}…", color = colors.onSurfaceSub)
                }
                is SubstitutionState.Error -> {
                    Divider(color = colors.divider)
                    Spacer(Modifier.height(10.dp))
                    Text(substitution.message, color = colors.error, style = MaterialTheme.typography.body2)
                    TextButton(onClick = onDismiss) { Text(if (L.isTr) "Kapat" else "Dismiss") }
                }
                is SubstitutionState.Review -> {
                    Divider(color = colors.divider)
                    Spacer(Modifier.height(12.dp))
                    val replacement = substitution.response.replacementIngredient
                    Text(
                        "${substitution.originalIngredientName} → ${replacement.name}",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        "${formatPlanQuantity(replacement.quantity)} ${localizedPlanUnit(replacement.unit, L.isTr)} · ${substitution.response.reason}",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body2
                    )
                    val beforeSeconds = state.cookingPlan?.steps?.sumOf { it.durationSeconds } ?: 0
                    val afterSeconds = substitution.response.mutatedPlan.steps.sumOf { it.durationSeconds }
                    val changedSteps = state.cookingPlan?.steps.orEmpty().count { before ->
                        substitution.response.mutatedPlan.steps.firstOrNull { it.id == before.id } != before
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (L.isTr) "$changedSteps adım güncellenecek · süre ${formatCookingDuration(beforeSeconds.toLong())} → ${formatCookingDuration(afterSeconds.toLong())}" else "$changedSteps steps will change · duration ${formatCookingDuration(beforeSeconds.toLong())} → ${formatCookingDuration(afterSeconds.toLong())}",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.caption
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onApply, colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary)) {
                            Text(if (L.isTr) "Değişikliği uygula" else "Apply substitution", color = colors.onPrimary)
                        }
                        TextButton(onClick = onDismiss) { Text(if (L.isTr) "Vazgeç" else "Cancel") }
                    }
                }
            }
        }
    }
}

internal fun formatCookingDuration(totalSeconds: Long): String {
''',
    "Operations substitution composable"
)

replace_once(
    "app-android/src/main/java/com/agentickitchen/android/MainActivity.kt",
    '''                    onConsumeActual = viewModel::consumeActualInventory,\n                    onCancelConsumption = viewModel::cancelInventoryConsumption\n''',
    '''                    onConsumeActual = viewModel::consumeActualInventory,\n                    onCancelConsumption = viewModel::cancelInventoryConsumption,\n                    onRequestSubstitution = viewModel::requestPantrySubstitution,\n                    onApplySubstitution = viewModel::applyPantrySubstitution,\n                    onDismissSubstitution = viewModel::dismissPantrySubstitution\n''',
    "MainActivity substitution wiring"
)

# ── Provider contract tests ───────────────────────────────────────────────────
replace_once(
    "app-android/src/test/java/com/agentickitchen/android/ai/FirebaseAiProviderTest.kt",
    '''import com.agentickitchen.shared.ai.RecipeOptionsRequest\n''',
    '''import com.agentickitchen.shared.ai.RecipeOptionsRequest\nimport com.agentickitchen.shared.ai.SubstitutionPlanRequest\nimport com.agentickitchen.shared.ai.dto.CookingPlanResponse\nimport com.agentickitchen.shared.ai.dto.CookingStepDto\nimport com.agentickitchen.shared.ai.dto.PlannedIngredientDto\n''',
    "Firebase test substitution imports"
)
replace_once(
    "app-android/src/test/java/com/agentickitchen/android/ai/FirebaseAiProviderTest.kt",
    '''    @Test\n    fun `shopping photo uses extraction model class and forwards image`() = runBlocking {\n''',
    r'''    @Test
    fun `substitution plan uses reasoning response kind`() = runBlocking {
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            responseKind = kind
            FirebaseGatewayResponse(substitutionJson, "reasoning-test-model")
        })
        val plan = CookingPlanResponse(
            "Rice", 2,
            listOf(PlannedIngredientDto("Rice", 160.0, "g", "rice"), PlannedIngredientDto("Onion", 1.0, "piece", "onion")),
            listOf(CookingStepDto("s1", "prep", "Prep", "counter", 60)),
            emptyList()
        )
        val result = provider.generateSubstitution(
            SubstitutionPlanRequest(plan, "Onion", listOf("2 clove Garlic"), setOf("pan"), "electric", 9, false, false, false, "none", emptySet(), "English")
        )
        assertTrue(result is AiResult.Success)
        assertEquals(FirebaseResponseKind.SUBSTITUTION_PLAN, responseKind)
        assertEquals(FirebaseAiTask.REASONING, responseKind?.task)
        assertEquals("Garlic", result.getOrNull()?.replacementIngredient?.name)
    }

    @Test
    fun `shopping photo uses extraction model class and forwards image`() = runBlocking {
''',
    "Firebase substitution test"
)
replace_once(
    "app-android/src/test/java/com/agentickitchen/android/ai/FirebaseAiProviderTest.kt",
    '''        val cookingPlanJson = """\n''',
    r'''        val substitutionJson = """
            {"originalIngredientName":"Onion","replacementIngredient":{"name":"Garlic","quantity":2.0,"unit":"clove","canonicalIngredientId":"garlic"},"reason":"Available aromatic substitute","confidence":0.8,
             "mutatedPlan":{"recipeName":"Rice","servings":2,
             "ingredients":[{"name":"Rice","quantity":160.0,"unit":"g","canonicalIngredientId":"rice"},{"name":"Garlic","quantity":2.0,"unit":"clove","canonicalIngredientId":"garlic"}],
             "steps":[{"id":"s1","type":"prep","instruction":"Prep garlic","resource":"counter","durationSeconds":60,"dependsOn":[],"visionCheckpointRecommended":false}],"safetyNotes":[]}}
        """.trimIndent()

        val cookingPlanJson = """
''',
    "Firebase substitution fixture"
)

# Prompt guard test
replace_once(
    "shared/src/test/kotlin/com/agentickitchen/shared/ai/prompt/PromptFactoryTest.kt",
    '''import org.junit.Test\n''',
    '''import com.agentickitchen.shared.ai.SubstitutionPlanRequest\nimport com.agentickitchen.shared.ai.dto.CookingPlanResponse\nimport com.agentickitchen.shared.ai.dto.CookingStepDto\nimport com.agentickitchen.shared.ai.dto.PlannedIngredientDto\nimport org.junit.Test\n''',
    "PromptFactoryTest substitution imports"
)
replace_once(
    "shared/src/test/kotlin/com/agentickitchen/shared/ai/prompt/PromptFactoryTest.kt",
    '''}\n''',
    r'''    @Test
    fun substitutionPromptRequiresOneForOnePantryBoundMutation() {
        val plan = CookingPlanResponse(
            "Rice", 2,
            listOf(PlannedIngredientDto("Onion", 1.0, "piece", "onion")),
            listOf(CookingStepDto("s1", "prep", "Chop onion", "knife", 60)),
            emptyList()
        )
        val prompt = PromptFactory.substitutionPlanPrompt(
            SubstitutionPlanRequest(plan, "Onion", listOf("2 clove Garlic"), setOf("knife"), "none", 0, false, false, false, "none", emptySet(), "English")
        )
        assertTrue(prompt.contains("Replace exactly the target ingredient identity"))
        assertTrue(prompt.contains("replacement MUST come from this list"))
        assertTrue(prompt.contains("complete set of step IDs unchanged"))
        assertTrue(prompt.contains("mutatedPlan must be a complete plan"))
    }
}
''',
    "PromptFactory substitution test"
)
