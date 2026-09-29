package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.RecipeImportResponse
import com.agentickitchen.shared.ai.RecipeImportSource
import com.agentickitchen.shared.ai.RecipePhotoImportRequest
import com.agentickitchen.shared.ai.RecipeTextImportRequest
import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyEnforcingAiProviderTest {
    @Test
    fun recipeTextSuccessIsForwardedExactlyOnce() = runTest {
        val expected = recipeSuccess(RecipeImportSource.AI_TEXT)
        val delegate = FakeProvider(recipeTextResult = expected)

        val actual = SafetyEnforcingAiProvider(delegate).parseRecipeText(
            RecipeTextImportRequest("prose", "English")
        )

        assertSame(expected, actual)
        assertEquals(1, delegate.recipeTextCalls)
    }

    @Test
    fun recipeTextFailureIsForwardedExactlyOnceWithoutUnsupportedFallback() = runTest {
        val expected = recipeFailure("upstream_text_failure")
        val delegate = FakeProvider(recipeTextResult = expected)

        val actual = SafetyEnforcingAiProvider(delegate).parseRecipeText(
            RecipeTextImportRequest("prose", "English")
        )

        assertSame(expected, actual)
        assertEquals(1, delegate.recipeTextCalls)
        assertEquals("upstream_text_failure", (actual as AiResult.Failure).technicalMessage)
    }

    @Test
    fun recipePhotoSuccessIsForwardedExactlyOnce() = runTest {
        val expected = recipeSuccess(RecipeImportSource.AI_PHOTO)
        val delegate = FakeProvider(recipePhotoResult = expected)

        val actual = SafetyEnforcingAiProvider(delegate).scanRecipePhoto(
            RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "English")
        )

        assertSame(expected, actual)
        assertEquals(1, delegate.recipePhotoCalls)
    }

    @Test
    fun recipePhotoFailureIsForwardedExactlyOnceWithoutUnsupportedFallback() = runTest {
        val expected = recipeFailure("upstream_photo_failure")
        val delegate = FakeProvider(recipePhotoResult = expected)

        val actual = SafetyEnforcingAiProvider(delegate).scanRecipePhoto(
            RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "English")
        )

        assertSame(expected, actual)
        assertEquals(1, delegate.recipePhotoCalls)
        assertEquals("upstream_photo_failure", (actual as AiResult.Failure).technicalMessage)
    }

    @Test
    fun cookingPhotoReplacesDefinitiveActionWithConfirmation() = runTest {
        val provider = SafetyEnforcingAiProvider(FakeProvider())

        val result = provider.inspectCookingPhoto(
            CookingPhotoRequest(
                recipeName = "Soup",
                plan = CookingPlanResponse("Soup", 2, emptyList(), emptyList(), emptyList()),
                currentStep = "Simmer",
                elapsedSeconds = 30,
                resource = "pot",
                recentTurns = emptyList(),
                question = "Ready?",
                image = KitchenImage(byteArrayOf(1), "image/jpeg"),
                language = "English"
            )
        ) as AiResult.Success

        assertTrue(result.value.immediateAction.contains("Do not change heat"))
        assertTrue(result.value.safetyWarning.orEmpty().contains("food thermometer"))
    }

    @Test
    fun shoppingPhotoReturnsOnlyConfidentCandidates() = runTest {
        val provider = SafetyEnforcingAiProvider(FakeProvider())

        val result = provider.scanShoppingPhoto(
            ShoppingPhotoRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "English")
        ) as AiResult.Success

        assertEquals(listOf("Tomato"), result.value.items.map { it.displayName })
    }

    @Test
    fun shoppingPhotoCanReturnNoConfidentCandidatesWithoutTurningIntoAnError() = runTest {
        val provider = SafetyEnforcingAiProvider(
            FakeProvider(
                shoppingCandidates = listOf(
                    ShoppingCandidate(displayName = "Maybe onion", confidence = 0.3, estimated = true)
                )
            )
        )

        val result = provider.scanShoppingPhoto(
            ShoppingPhotoRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "English")
        ) as AiResult.Success

        assertTrue(result.value.items.isEmpty())
    }

    private class FakeProvider(
        private val shoppingCandidates: List<ShoppingCandidate> = listOf(
            ShoppingCandidate(displayName = "Tomato", confidence = 0.95, estimated = false),
            ShoppingCandidate(displayName = "Maybe onion", confidence = 0.3, estimated = true)
        ),
        private val recipeTextResult: AiResult<RecipeImportResponse> = recipeSuccess(RecipeImportSource.AI_TEXT),
        private val recipePhotoResult: AiResult<RecipeImportResponse> = recipeSuccess(RecipeImportSource.AI_PHOTO)
    ) : KitchenAiProvider {
        var recipeTextCalls = 0
        var recipePhotoCalls = 0

        override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> = error("unused")
        override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> = error("unused")
        override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> = error("unused")

        override suspend fun parseRecipeText(request: RecipeTextImportRequest): AiResult<RecipeImportResponse> {
            recipeTextCalls++
            return recipeTextResult
        }

        override suspend fun scanRecipePhoto(request: RecipePhotoImportRequest): AiResult<RecipeImportResponse> {
            recipePhotoCalls++
            return recipePhotoResult
        }

        override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
            AiResult.Success(
                ShoppingImportResponse(shoppingCandidates),
                AiProviderId.GEMINI,
                "test"
            )

        override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
            AiResult.Success(
                CookingPhotoResponse(
                    assessment = "Looks cooked",
                    visibleObservation = "Brown surface",
                    immediateAction = "Serve now",
                    uncertainty = "Lighting may affect colour"
                ),
                AiProviderId.GEMINI,
                "test"
            )

        override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> = error("unused")
        override suspend fun testConnection(): AiResult<Unit> = AiResult.Success(Unit, AiProviderId.GEMINI, "test")
    }

    companion object {
        private fun recipeSuccess(source: RecipeImportSource): AiResult<RecipeImportResponse> =
            AiResult.Success(
                RecipeImportResponse(
                    recipe = ImportedRecipe(
                        name = "Soup",
                        servings = 2,
                        ingredients = emptyList(),
                        instructions = emptyList()
                    ),
                    confidence = 1.0,
                    source = source
                ),
                AiProviderId.GEMINI,
                "test"
            )

        private fun recipeFailure(message: String): AiResult<RecipeImportResponse> =
            AiResult.Failure(
                com.agentickitchen.shared.ai.AiFailureType.ProviderUnavailable,
                retryable = false,
                userMessage = com.agentickitchen.shared.ai.AiFailureType.ProviderUnavailable.userMessageRes,
                technicalMessage = message
            )
    }
}
