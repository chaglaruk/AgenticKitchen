package com.agentickitchen.android.ai

import com.google.firebase.ai.type.Schema

/**
 * Controls whether a structured request sends an explicit responseSchema to the backend.
 *
 * STRICT_SCHEMA sends the kind's responseSchema together with the JSON mime type.
 * JSON_ONLY sends only the JSON mime type and relies on the prompt plus application-side
 * decoding/validation. Introduced as a controlled experiment for COOKING_PLAN after
 * reproducible ProviderUnavailable failures on the schema-bearing request.
 */
internal enum class FirebaseSchemaMode {
    STRICT_SCHEMA,
    JSON_ONLY
}

internal enum class FirebaseResponseKind(
    val task: FirebaseAiTask,
    val schema: Schema?,
    val schemaMode: FirebaseSchemaMode
) {
    RECIPE_OPTIONS(FirebaseAiTask.REASONING, FirebaseResponseSchemas.recipeOptions, FirebaseSchemaMode.STRICT_SCHEMA),
    COOKING_PLAN(FirebaseAiTask.REASONING, null, FirebaseSchemaMode.JSON_ONLY),
    SUBSTITUTION_PLAN(FirebaseAiTask.REASONING, FirebaseResponseSchemas.substitutionPlan, FirebaseSchemaMode.STRICT_SCHEMA),
    SHOPPING_IMPORT(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.shoppingImport, FirebaseSchemaMode.STRICT_SCHEMA),
    RECIPE_IMPORT_TEXT(FirebaseAiTask.EXTRACTION, FirebaseResponseSchemas.recipeImport, FirebaseSchemaMode.STRICT_SCHEMA),
    RECIPE_IMPORT_PHOTO(FirebaseAiTask.VISION, FirebaseResponseSchemas.recipeImport, FirebaseSchemaMode.STRICT_SCHEMA),
    COOKING_PHOTO(FirebaseAiTask.VISION, FirebaseResponseSchemas.cookingPhoto, FirebaseSchemaMode.STRICT_SCHEMA),
    COOKING_CHAT(FirebaseAiTask.REASONING, FirebaseResponseSchemas.cookingChat, FirebaseSchemaMode.STRICT_SCHEMA),
    CONNECTION_TEST(FirebaseAiTask.REASONING, FirebaseResponseSchemas.connectionTest, FirebaseSchemaMode.STRICT_SCHEMA)
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
