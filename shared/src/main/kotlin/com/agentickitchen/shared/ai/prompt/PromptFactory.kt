package com.agentickitchen.shared.ai.prompt

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
      "missingIngredients": ["salt"]
    }
  ]
}"""
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
        val stoveGuidance = if (stoveType == "gas") {
            """Stove type: gas
Gas flame guidance: use only low, medium-low, medium, medium-high, or high flame descriptions. Do not use numeric electric-style power levels. Set powerLevel to null for gas stove steps."""
        } else {
            """Stove type: electric
Electric stove maximum level: $stoveMaxLevel. Use numeric powerLevel values only from 1 to $stoveMaxLevel."""
        }
        val examplePowerLevel = if (stoveType == "gas") "null" else "7"
        val powerLevelRule = if (stoveType == "gas") {
            "For this gas stove, use qualitative flame guidance and leave powerLevel null."
        } else {
            "For this electric stove, powerLevel must be from 1 to $stoveMaxLevel."
        }
        return """You are a military-precision chef AI. Create a detailed cooking plan for "$recipeName".

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

    fun visionAssessmentPrompt(
        stepDescription: String,
        language: String
    ): String {
        val langInstr = if (language == "Türkçe") "Yanıtını Türkçe ver." else "Respond in English."
        return """You are a military chef inspecting a cooking photo. The current step is: "$stepDescription".

$langInstr

Examine the image and determine:
1. Is the food properly cooked at this stage?
2. Should the user continue, adjust heat, or stop?
3. Any specific visual cues to look for?

Provide clear, actionable feedback."""
    }
}
