package com.agentickitchen.shared.ai.prompt

import com.agentickitchen.shared.ai.SubstitutionPlanRequest

object PromptFactory {

    fun recipeOptionsPrompt(
        ingredients: List<String>,
        equipment: Set<String>,
        dietType: String,
        allergies: Set<String>,
        language: String
    ): String {
        val langInstr = if (language == "Türkçe") "Yanıtını Türkçe ver." else "Respond in English."
        return """You are a professional chef AI. Generate exactly 3 different recipe options using the ingredients and equipment available.

Ingredients: ${ingredients.joinToString(", ")}
Available equipment: ${equipment.joinToString(", ")}
Diet: $dietType
Allergies: ${allergies.joinToString(", ") { if (it.isEmpty()) "none" else it }}

$langInstr

Return ONLY valid JSON in this exact schema:
{
  "options": [
    {
      "id": "option_1",
      "name": "Recipe name",
      "summary": "Brief description",
      "difficulty": "easy|medium|hard",
      "estimatedMinutes": 30,
      "requiredEquipment": ["pan", "oven"],
      "missingIngredients": ["salt"],
      "proposedIngredients": [{"name": "...", "quantity": 200.0, "unit": "g"}]
    }
  ]
}"""
    }

    /**
     * Extra context for pantry-backed recipe candidate generation.
     *
     * The provider proposes candidates; the app remains authoritative for pantry comparison,
     * shortage classification and ranking. The user's missing-item allowance therefore must not
     * become an upstream hard filter except when strict-stock mode is explicitly enabled.
     */
    fun inventoryRecipeOptionsContext(
        inventoryLines: List<String>,
        strictStock: Boolean,
        maxMissingStaples: Int,
        servings: Int,
        prioritizedIngredients: List<String>
    ): String {
        if (inventoryLines.isEmpty()) return ""
        val candidateGuidance = if (strictStock) {
            """Strict stock mode is ON. Every proposed option must be fully preparable from the listed pantry quantities. Do not introduce a broader missing-item idea."""
        } else {
            """Strict stock mode is OFF.
The app will compare every structured option against the pantry locally and is authoritative for Ready Now / Missing 1 / Missing 2 / AI Ideas classification.
The user's pantry-preparation allowance is at most $maxMissingStaples missing item(s). Treat that as a preparation allowance, NOT as a hard generation filter.
Favor high pantry coverage for the first options. When a safe, relevant broader idea genuinely adds value, option 3 may exceed the preparation allowance; the app will show it as an AI Idea and will block Prepare until its shortage is resolved."""
        }
        return """

Available pantry quantities:
${inventoryLines.joinToString("\n")}
Servings: $servings
Prioritize: ${prioritizedIngredients.joinToString(", ")}
$candidateGuidance
Include exact proposedIngredients for every option so the app can compare quantities deterministically.
Never claim an ingredient is available unless the pantry list supports it.
Respect diet and allergies strictly for every option.
""".trimEnd()
    }

    fun cookingPlanPrompt(
        recipeName: String,
        ingredients: List<String>,
        equipment: Set<String>,
        servings: Int,
        stoveType: String,
        stoveMaxLevel: Int,
        ovenAvailable: Boolean,
        ovenHasFan: Boolean,
        airfryerAvailable: Boolean,
        dietType: String,
        allergies: Set<String>,
        language: String
    ): String {
        val langInstr = if (language == "Türkçe") "Yanıtını Türkçe ver." else "Respond in English."
        val normalizedStoveType = stoveType.lowercase()
        val stoveGuidance = when (normalizedStoveType) {
            "gas" -> """Stove type: gas
Gas flame guidance: use only low, medium-low, medium, medium-high, or high flame descriptions. Do not use numeric electric-style power levels. Set powerLevel to null for gas stove steps."""
            "electric" -> """Stove type: electric
Electric stove maximum level: $stoveMaxLevel. Use numeric powerLevel values only from 1 to $stoveMaxLevel."""
            else -> """Stove type: none
No stove is available. Do not include stove-heating steps or numeric powerLevel values. Use only the available oven, airfryer, and preparation resources."""
        }
        val examplePowerLevel = if (normalizedStoveType == "electric") "7" else "null"
        val powerLevelRule = when (normalizedStoveType) {
            "gas" -> "For this gas stove, use qualitative flame guidance and leave powerLevel null."
            "electric" -> "For this electric stove, powerLevel must be from 1 to $stoveMaxLevel."
            else -> "No stove is available: do not use the stove resource and leave powerLevel null."
        }
        return """You are an experienced home-cooking assistant. Create a detailed cooking plan for "$recipeName".

Ingredients: ${ingredients.joinToString(", ")}
Available equipment: ${equipment.joinToString(", ")}
Servings: $servings
$stoveGuidance
Oven available: $ovenAvailable
Oven has fan: $ovenHasFan
Airfryer available: $airfryerAvailable
Diet: $dietType
Allergies: ${allergies.joinToString(", ") { if (it.isEmpty()) "none" else it }}

$langInstr

Return ONLY valid JSON in this exact schema:
{
  "recipeName": "...",
  "servings": $servings,
  "ingredients": [{"name": "...", "quantity": 200.0, "unit": "g"}],
  "steps": [
    {
      "id": "step_1",
      "type": "prep|cook|rest|serve",
      "instruction": "Cook with the appropriate heat for 4 minutes",
      "resource": "stove|oven|airfryer|counter|knife|bowl",
      "durationSeconds": 240,
      "targetTemperatureC": null,
      "powerLevel": $examplePowerLevel,
      "dependsOn": [],
      "visionCheckpointRecommended": false
    }
  ],
  "safetyNotes": ["Watch for burning"]
}

Rules:
- Give exact oven temperatures and practical stove guidance
- Resource must be one of: stove, oven, airfryer, counter, knife, bowl
- $powerLevelRule
- Oven steps only if ovenAvailable is true
- Airfryer steps only if airfryerAvailable is true
- Duration must be reasonable (30-3600 seconds)
- Each step ID must be unique
- Dependencies must refer to existing step IDs
- Respect diet and allergies strictly"""
    }

    fun substitutionPrompt(
        recipeName: String,
        originalIngredient: String,
        candidateIngredient: String,
        language: String
    ): String {
        val langInstr = if (language == "Türkçe") "Yanıtını Türkçe ver." else "Respond in English."
        return """You are a strict ingredient substitution advisor for the recipe "$recipeName".
The user wants to replace "$originalIngredient" with "$candidateIngredient".

$langInstr

Analyze if the substitution is chemically and flavor-profile compatible.
If it would break the dish, explain why and suggest a better alternative if possible.
Keep your response concise and authoritative."""
    }

    fun substitutionPlanPrompt(request: SubstitutionPlanRequest): String {
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

    fun visionAssessmentPrompt(
        stepDescription: String,
        language: String
    ): String {
        val langInstr = if (language == "Türkçe") "Yanıtını Türkçe ver." else "Respond in English."
        return """You are a helpful home-cooking assistant inspecting a cooking photo. The current step is: "$stepDescription".

$langInstr

Examine the image and determine:
1. Is the food properly cooked at this stage?
2. Should the user continue, adjust heat, or stop?
3. Any specific visual cues to look for?

Provide clear, actionable feedback."""
    }
}
