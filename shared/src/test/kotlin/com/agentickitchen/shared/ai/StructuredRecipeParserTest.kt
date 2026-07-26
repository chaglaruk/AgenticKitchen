package com.agentickitchen.shared.ai

import kotlin.test.Test
import kotlin.test.assertTrue

class StructuredRecipeParserTest {
    @Test fun parsesRecipeOptionsJson() {
        assertTrue(StructuredRecipeParser.recipeOptions("""{"options":[{"id":"one","name":"Soup","summary":"Hot","difficulty":"easy","estimatedMinutes":20,"requiredEquipment":["pan"],"missingIngredients":[]}]}""").isSuccess)
    }

    @Test fun rejectsMalformedJson() {
        assertTrue(StructuredRecipeParser.recipeOptions("not json").isFailure)
    }

    @Test fun parsesCookingPlanJson() {
        assertTrue(StructuredRecipeParser.cookingPlan("""{"recipeName":"Soup","servings":2,"ingredients":[{"name":"tomato","quantity":300,"unit":"g"}],"steps":[{"id":"prep","type":"prep","instruction":"Chop tomato","resource":"knife","durationSeconds":60}],"safetyNotes":[]}""").isSuccess)
    }
}
