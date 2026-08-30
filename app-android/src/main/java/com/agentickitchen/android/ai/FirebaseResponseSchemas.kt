package com.agentickitchen.android.ai

import com.google.firebase.ai.type.Schema

internal enum class FirebaseResponseKind(
    val task: FirebaseAiTask,
    val schema: Schema
) {
    RECIPE_OPTIONS(FirebaseAiTask.REASONING, FirebaseResponseSchemas.recipeOptions),
    COOKING_PLAN(FirebaseAiTask.REASONING, FirebaseResponseSchemas.cookingPlan),
    SUBSTITUTION_PLAN(FirebaseAiTask.REASONING, FirebaseResponseSchemas.substitutionPlan),
    SHOPPING_IMPORT(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.shoppingImport),
    COOKING_PHOTO(FirebaseAiTask.VISION, FirebaseResponseSchemas.cookingPhoto),
    COOKING_CHAT(FirebaseAiTask.REASONING, FirebaseResponseSchemas.cookingChat),
    CONNECTION_TEST(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.connectionTest)
}

private object FirebaseResponseSchemas {
    private val plannedIngredient = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Ingredient display name"),
            "quantity" to Schema.double(description = "Positive ingredient quantity"),
            "unit" to Schema.string(description = "Canonical or human-readable unit"),
            "canonicalIngredientId" to Schema.string(
                description = "Canonical ingredient identifier when confidently known",
                nullable = true
            )
        ),
        optionalProperties = listOf("canonicalIngredientId")
    )

    val recipeOptions = Schema.obj(
        properties = mapOf(
            "options" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "name" to Schema.string(),
                        "summary" to Schema.string(),
                        "difficulty" to Schema.string(),
                        "estimatedMinutes" to Schema.integer(),
                        "requiredEquipment" to Schema.array(Schema.string()),
                        "missingIngredients" to Schema.array(Schema.string()),
                        "proposedIngredients" to Schema.array(plannedIngredient)
                    )
                ),
                minItems = 3,
                maxItems = 3
            )
        )
    )

    val cookingPlan = Schema.obj(
        properties = mapOf(
            "recipeName" to Schema.string(),
            "servings" to Schema.integer(),
            "ingredients" to Schema.array(plannedIngredient, minItems = 1),
            "steps" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "type" to Schema.string(),
                        "instruction" to Schema.string(),
                        "resource" to Schema.string(),
                        "durationSeconds" to Schema.integer(),
                        "targetTemperatureC" to Schema.integer(nullable = true),
                        "powerLevel" to Schema.integer(nullable = true),
                        "dependsOn" to Schema.array(Schema.string()),
                        "visionCheckpointRecommended" to Schema.boolean()
                    ),
                    optionalProperties = listOf("targetTemperatureC", "powerLevel")
                ),
                minItems = 1
            ),
            "safetyNotes" to Schema.array(Schema.string())
        )
    )

    val substitutionPlan = Schema.obj(
        properties = mapOf(
            "originalIngredientName" to Schema.string(),
            "replacementIngredient" to plannedIngredient,
            "reason" to Schema.string(),
            "confidence" to Schema.double(),
            "mutatedPlan" to cookingPlan
        )
    )

    val shoppingImport = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "canonicalIngredientId" to Schema.string(nullable = true),
                        "displayName" to Schema.string(),
                        "quantity" to Schema.double(nullable = true),
                        "unit" to Schema.string(nullable = true),
                        "unitDimension" to Schema.string(),
                        "packageLabel" to Schema.string(nullable = true),
                        "confidence" to Schema.double(),
                        "estimated" to Schema.boolean(),
                        "uncertaintyReason" to Schema.string(nullable = true)
                    ),
                    optionalProperties = listOf(
                        "canonicalIngredientId",
                        "quantity",
                        "unit",
                        "packageLabel",
                        "uncertaintyReason"
                    )
                )
            )
        )
    )

    val cookingPhoto = Schema.obj(
        properties = mapOf(
            "assessment" to Schema.string(),
            "visibleObservation" to Schema.string(),
            "immediateAction" to Schema.string(),
            "heatAdjustment" to Schema.string(nullable = true),
            "recheckAfterSeconds" to Schema.integer(nullable = true),
            "safetyWarning" to Schema.string(nullable = true),
            "uncertainty" to Schema.string()
        ),
        optionalProperties = listOf("heatAdjustment", "recheckAfterSeconds", "safetyWarning")
    )

    val cookingChat = Schema.obj(
        properties = mapOf("answer" to Schema.string())
    )

    val connectionTest = Schema.obj(
        properties = mapOf(
            "status" to Schema.enumeration(listOf("ok"))
        )
    )
}
