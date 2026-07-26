package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto

class CookingPlanValidator(
    private val availableEquipment: Set<String>,
    private val stoveMaxLevel: Int,
    private val ovenAvailable: Boolean,
    private val airfryerAvailable: Boolean,
    private val dietType: String,
    private val allergens: Set<String>,
    private val servings: Int,
    private val knownResources: Set<String> = setOf("stove", "oven", "airfryer", "counter", "knife", "bowl", "fridge", "sink", "cutting_board", "pan", "pot", "baking_tray", "mixer", "blender"),
    private val maxDurationSeconds: Int = 7200,
    private val maxTemperatureC: Int = 300
) {

    fun validate(plan: CookingPlanResponse): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<String>()

        validateServings(plan, errors)
        validateIngredients(plan, errors)
        validateSteps(plan, errors, warnings)
        validateDietAndAllergens(plan, errors, warnings)
        validateSafety(plan, warnings)

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateServings(plan: CookingPlanResponse, errors: MutableList<ValidationError>) {
        if (plan.servings != servings) {
            errors.add(
                ValidationError(
                    ErrorType.SERVING_MISMATCH,
                    "servings",
                    "Plan has ${plan.servings} servings but user requested $servings"
                )
            )
        }
    }

    private fun validateIngredients(plan: CookingPlanResponse, errors: MutableList<ValidationError>) {
        for ((index, ingredient) in plan.ingredients.withIndex()) {
            if (ingredient.name.isBlank()) {
                errors.add(
                    ValidationError(
                        ErrorType.MISSING_INGREDIENT,
                        "ingredients[$index].name",
                        "Ingredient name is empty"
                    )
                )
            }
            if (ingredient.quantity <= 0) {
                errors.add(
                    ValidationError(
                        ErrorType.MISSING_QUANTITY,
                        "ingredients[$index].quantity",
                        "Ingredient '${ingredient.name}' has zero or negative quantity"
                    )
                )
            }
            val normalizedUnit = ingredient.unit.lowercase()
            if (normalizedUnit !in setOf("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "piece", "pieces", "slice", "slices", "clove", "pinch", "unit", "to taste", "")) {
                errors.add(
                    ValidationError(
                        ErrorType.UNKNOWN_UNIT,
                        "ingredients[$index].unit",
                        "Unknown unit '${ingredient.unit}' for '${ingredient.name}'"
                    )
                )
            }
        }
    }

    private fun validateSteps(plan: CookingPlanResponse, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        if (plan.steps.isEmpty()) errors.add(ValidationError(ErrorType.MISSING_INGREDIENT, "steps", "Plan has no steps"))
        val stepIds = mutableSetOf<String>()
        val stepMap = plan.steps.associateBy { it.id }

        for ((index, step) in plan.steps.withIndex()) {
            validateStepId(step, index, stepIds, errors)
            validateResource(step, index, errors)
            validatePowerLevel(step, index, errors)
            validateDuration(step, index, errors, warnings)
            validateDependencies(step, index, stepMap, errors)
            validateStepType(step, index, errors)
        }

        validateDependencyCycle(plan.steps, errors)
        validateParallelResourceConflicts(plan.steps, errors)
    }

    private fun validateStepId(step: CookingStepDto, index: Int, stepIds: MutableSet<String>, errors: MutableList<ValidationError>) {
        if (step.id.isBlank()) {
            errors.add(ValidationError(ErrorType.DUPLICATE_STEP_ID, "steps[$index].id", "Step ID is empty"))
            return
        }
        if (!stepIds.add(step.id)) {
            errors.add(ValidationError(ErrorType.DUPLICATE_STEP_ID, "steps[$index].id", "Duplicate step ID '${step.id}'"))
        }
    }

    private fun validateResource(step: CookingStepDto, index: Int, errors: MutableList<ValidationError>) {
        if (step.instruction.isBlank()) errors.add(ValidationError(ErrorType.MISSING_INGREDIENT, "steps[$index].instruction", "Step instruction is empty"))
        if (step.resource !in knownResources) {
            errors.add(
                ValidationError(
                    ErrorType.UNKNOWN_RESOURCE,
                    "steps[$index].resource",
                    "Unknown resource '${step.resource}' in step '${step.id}'"
                )
            )
            return
        }

        if (step.resource == "oven" && !ovenAvailable) {
            errors.add(
                ValidationError(
                    ErrorType.UNAVAILABLE_EQUIPMENT,
                    "steps[$index].resource",
                    "Step '${step.id}' requires oven but user has no oven"
                )
            )
        }

        if (step.resource == "airfryer" && !airfryerAvailable) {
            errors.add(
                ValidationError(
                    ErrorType.UNAVAILABLE_EQUIPMENT,
                    "steps[$index].resource",
                    "Step '${step.id}' requires airfryer but user has no airfryer"
                )
            )
        }
        if (step.resource in setOf("stove", "pan", "pot") && availableEquipment.none { it in setOf("stove", "elec", "gas", "camping", "pan") }) {
            errors.add(ValidationError(ErrorType.UNAVAILABLE_EQUIPMENT, "steps[$index].resource", "Step '${step.id}' requires stove equipment"))
        }
    }

    private fun validatePowerLevel(step: CookingStepDto, index: Int, errors: MutableList<ValidationError>) {
        val power = step.powerLevel ?: return
        if (power > stoveMaxLevel) {
            errors.add(
                ValidationError(
                    ErrorType.POWER_EXCEEDS_MAXIMUM,
                    "steps[$index].powerLevel",
                    "Step '${step.id}' requires power level $power but max is $stoveMaxLevel"
                )
            )
        }
        if (power <= 0) {
            errors.add(
                ValidationError(
                    ErrorType.POWER_EXCEEDS_MAXIMUM,
                    "steps[$index].powerLevel",
                    "Step '${step.id}' has invalid power level $power"
                )
            )
        }
    }

    private fun validateDuration(step: CookingStepDto, index: Int, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        if (step.durationSeconds < 0) {
            errors.add(
                ValidationError(
                    ErrorType.NEGATIVE_DURATION,
                    "steps[$index].durationSeconds",
                    "Step '${step.id}' has negative duration"
                )
            )
        } else if (step.durationSeconds == 0) {
            errors.add(
                ValidationError(
                    ErrorType.ZERO_DURATION,
                    "steps[$index].durationSeconds",
                    "Step '${step.id}' has zero duration"
                )
            )
        } else if (step.durationSeconds > maxDurationSeconds) {
            errors.add(
                ValidationError(
                    ErrorType.EXCESSIVE_DURATION,
                    "steps[$index].durationSeconds",
                    "Step '${step.id}' duration ${step.durationSeconds}s exceeds max $maxDurationSeconds"
                )
            )
        }

        val temp = step.targetTemperatureC
        if (temp != null && temp > maxTemperatureC) {
            warnings.add("Step '${step.id}' has temperature $temp°C which exceeds $maxTemperatureC°C")
        } else if (temp != null && temp < -20) {
            warnings.add("Step '${step.id}' has unusually low temperature $temp°C")
        }
    }

    private fun validateDependencies(step: CookingStepDto, index: Int, stepMap: Map<String, CookingStepDto>, errors: MutableList<ValidationError>) {
        for (depId in step.dependsOn) {
            if (step.id == depId) {
                errors.add(
                    ValidationError(
                        ErrorType.DEPENDENCY_CYCLE,
                        "steps[$index].dependsOn",
                        "Step '${step.id}' depends on itself"
                    )
                )
            } else if (depId !in stepMap) {
                errors.add(
                    ValidationError(
                        ErrorType.MISSING_DEPENDENCY,
                        "steps[$index].dependsOn",
                        "Step '${step.id}' depends on '$depId' which does not exist"
                    )
                )
            }
        }
    }

    private fun validateStepType(step: CookingStepDto, index: Int, errors: MutableList<ValidationError>) {
        if (step.type !in setOf("prep", "cook", "rest", "serve", "combine", "heat", "cool")) {
            errors.add(
                ValidationError(
                    ErrorType.INVALID_STEP_TYPE,
                    "steps[$index].type",
                    "Unknown step type '${step.type}' in step '${step.id}'"
                )
            )
        }
    }

    private fun validateDependencyCycle(steps: List<CookingStepDto>, errors: MutableList<ValidationError>) {
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (step in steps) {
            adjacency.getOrPut(step.id) { mutableListOf() }
            for (dep in step.dependsOn) {
                adjacency.getOrPut(dep) { mutableListOf() }.add(step.id)
            }
        }

        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun hasCycle(node: String): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited.add(node)
            inStack.add(node)
            for (neighbor in adjacency[node].orEmpty()) {
                if (hasCycle(neighbor)) return true
            }
            inStack.remove(node)
            return false
        }

        for (node in adjacency.keys) {
            if (hasCycle(node)) {
                errors.add(
                    ValidationError(
                        ErrorType.DEPENDENCY_CYCLE,
                        "dependency_graph",
                        "Circular dependency detected in step graph"
                    )
                )
                return
            }
        }
    }

    private fun validateParallelResourceConflicts(steps: List<CookingStepDto>, errors: MutableList<ValidationError>) {
        val sameResourceSteps = steps.groupBy { it.resource }
        for ((resource, resourceSteps) in sameResourceSteps) {
            if (resource in setOf("stove", "oven", "airfryer") && resourceSteps.size > 1) {
                val noDependency = resourceSteps.filter { s -> resourceSteps.none { s.id in it.dependsOn } }
                if (noDependency.size > 1) {
                    errors.add(
                        ValidationError(
                            ErrorType.PARALLEL_RESOURCE_CONFLICT,
                            resource,
                            "Multiple steps use $resource concurrently: ${noDependency.map { it.id }}"
                        )
                    )
                }
            }
        }
    }

    private fun validateDietAndAllergens(plan: CookingPlanResponse, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        if (dietType == "vegan" && plan.ingredients.any { it.name.lowercase() in setOf("meat", "chicken", "fish", "egg", "egg", "milk", "cheese", "butter", "cream", "yogurt", "honey") }) {
            errors.add(ValidationError(ErrorType.DIET_CONFLICT, "ingredients", "Plan conflicts with vegan diet"))
        }
        if (dietType == "vegetarian" && plan.ingredients.any { it.name.lowercase() in setOf("meat", "chicken", "fish", "beef", "pork", "lamb") }) {
            errors.add(ValidationError(ErrorType.DIET_CONFLICT, "ingredients", "Plan conflicts with vegetarian diet"))
        }

        val recipeIngredientNames = plan.ingredients.map { it.name.lowercase() }.toSet()
        for (allergen in allergens) {
            val allergenLower = allergen.lowercase()
            if (allergenLower in recipeIngredientNames) {
                errors.add(
                    ValidationError(
                        ErrorType.ALLERGEN_CONFLICT,
                        "ingredients",
                        "Plan contains '$allergen' which user reported as allergen"
                    )
                )
            }
            val knownAllergenIngredients = knownAllergenMap[allergenLower].orEmpty()
            val conflict = knownAllergenIngredients.intersect(recipeIngredientNames)
            if (conflict.isNotEmpty()) {
                errors.add(
                    ValidationError(
                        ErrorType.ALLERGEN_CONFLICT,
                        "ingredients",
                        "Plan contains '$conflict' which may contain allergen '$allergen'"
                    )
                )
            }
        }
    }

    private fun validateSafety(plan: CookingPlanResponse, warnings: MutableList<String>) {
        val hasRawMeat = plan.ingredients.any {
            it.name.lowercase() in setOf("chicken", "tavuk", "beef", "et", "pork", "domuz", "fish", "balık", "lamb", "kuzu")
        }
        if (hasRawMeat) {
            val cookSteps = plan.steps.filter { it.type == "cook" }
            if (cookSteps.isEmpty()) {
                warnings.add("Recipe contains raw meat but has no cooking steps")
            }
        }
    }

    companion object {
        private val knownAllergenMap = mapOf(
            "gluten" to setOf("un", "flour", "bread", "pasta", "noodle", "wheat", "bulgur"),
            "süt" to setOf("süt", "milk", "cheese", "peynir", "cream", "krema", "butter", "tereyağı", "yogurt", "yoğurt"),
            "milk" to setOf("milk", "cheese", "cream", "butter", "yogurt"),
            "yumurta" to setOf("yumurta", "egg"),
            "egg" to setOf("egg"),
            "fındık" to setOf("fındık", "nut", "almond", "badem", "ceviz", "walnut"),
            "tree nuts" to setOf("almond", "badem", "walnut", "ceviz", "hazelnut", "fındık", "cashew", "kaju", "pistachio", "antep fıstığı"),
            "yer fıstığı" to setOf("peanut", "yer fıstığı"),
            "peanut" to setOf("peanut"),
            "soya" to setOf("soya", "soy", "tofu"),
            "soy" to setOf("soy", "tofu"),
            "deniz ürünü" to setOf("shrimp", "karides", "crab", "yengeç", "mussel", "midye"),
            "shellfish" to setOf("shrimp", "crab", "mussel", "lobster", "prawn"),
        )
    }
}
