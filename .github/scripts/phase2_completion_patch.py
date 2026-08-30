from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


gemini_path = Path("app-android/src/main/java/com/agentickitchen/android/ai/GeminiProvider.kt")
gemini = gemini_path.read_text(encoding="utf-8")
gemini = replace_once(
    gemini,
    '''            prompt = PromptFactory.recipeOptionsPrompt(
                request.ingredients,
                request.equipment,
                request.dietType,
                request.allergies,
                request.language
            ) + if (request.inventoryLines.isEmpty()) {
                ""
            } else {
                """

Available pantry quantities:
${request.inventoryLines.joinToString("\\n")}
Strict stock only: ${request.strictStock}
Maximum missing staples: ${request.maxMissingStaples}
Servings: ${request.servings}
Prioritize: ${request.prioritizedIngredients.joinToString(", ")}
Include exact proposedIngredients for every option. Never exceed available quantities when strict stock only is true.
                """.trimIndent()
            },
''',
    '''            prompt = PromptFactory.recipeOptionsPrompt(
                request.ingredients,
                request.equipment,
                request.dietType,
                request.allergies,
                request.language
            ) + PromptFactory.inventoryRecipeOptionsContext(
                inventoryLines = request.inventoryLines,
                strictStock = request.strictStock,
                maxMissingStaples = request.maxMissingStaples,
                servings = request.servings,
                prioritizedIngredients = request.prioritizedIngredients
            ),
''',
    "Gemini pantry candidate context",
)
gemini_path.write_text(gemini, encoding="utf-8")


firebase_path = Path("app-android/src/main/java/com/agentickitchen/android/ai/FirebaseAiProvider.kt")
firebase = firebase_path.read_text(encoding="utf-8")
firebase = replace_once(
    firebase,
    '''            prompt = PromptFactory.recipeOptionsPrompt(
                request.ingredients,
                request.equipment,
                request.dietType,
                request.allergies,
                request.language
            ) + inventoryRecipeContext(request),
''',
    '''            prompt = PromptFactory.recipeOptionsPrompt(
                request.ingredients,
                request.equipment,
                request.dietType,
                request.allergies,
                request.language
            ) + PromptFactory.inventoryRecipeOptionsContext(
                inventoryLines = request.inventoryLines,
                strictStock = request.strictStock,
                maxMissingStaples = request.maxMissingStaples,
                servings = request.servings,
                prioritizedIngredients = request.prioritizedIngredients
            ),
''',
    "Firebase shared pantry candidate context",
)
firebase = replace_once(
    firebase,
    '''    private fun inventoryRecipeContext(request: RecipeOptionsRequest): String =
        if (request.inventoryLines.isEmpty()) "" else """

Available pantry quantities:
${request.inventoryLines.joinToString("\\n")}
Strict stock only: ${request.strictStock}
Maximum missing staples: ${request.maxMissingStaples}
Servings: ${request.servings}
Prioritize: ${request.prioritizedIngredients.joinToString(", ")}
Include exact proposedIngredients for every option. Never exceed available quantities when strict stock only is true.
""".trimEnd()

''',
    "",
    "remove Firebase duplicate pantry context",
)
firebase_path.write_text(firebase, encoding="utf-8")
