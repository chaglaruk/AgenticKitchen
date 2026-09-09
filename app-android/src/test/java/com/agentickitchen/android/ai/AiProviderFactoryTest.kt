package com.agentickitchen.android.ai

import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderFactoryTest {

    private class TestAiProvider(val id: AiProviderId) : KitchenAiProvider {
        override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> =
            AiResult.Success(RecipeOptionsResponse(emptyList()), id, "test-model")

        override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> =
            AiResult.Success(CookingPlanResponse("Plan", 2, emptyList(), emptyList(), emptyList()), id, "test-model")

        override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =
            AiResult.Success(ShoppingImportResponse(emptyList()), id, "test-model")

        override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
            AiResult.Success(ShoppingImportResponse(emptyList()), id, "test-model")

        override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
            AiResult.Success(
                CookingPhotoResponse("Good", "Observation", "Action", uncertainty = "None"),
                id,
                "test-model"
            )

        override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> =
            AiResult.Success(CookingChatResponse("Answer"), id, "test-model")

        override suspend fun testConnection(): AiResult<Unit> =
            AiResult.Success(Unit, id, "test-model")
    }

    @Test
    fun firebaseProviderReturnsManagedProviderWhenPresent() {
        val managed = TestAiProvider(AiProviderId.FIREBASE)
        val offline = TestAiProvider(AiProviderId.FREE)
        val factory = DefaultAiProviderFactory(
            managedProvider = managed,
            offlineProvider = offline,
            enforceVisionSafety = false
        )

        val resolved = factory.provider(HardwareSettings(aiProvider = "FIREBASE"))

        assertSame(managed, resolved)
    }

    @Test
    fun firebaseProviderReturnsNullWhenManagedProviderAbsentAndNeverFallsBackToOffline() {
        val offline = TestAiProvider(AiProviderId.FREE)
        val factory = DefaultAiProviderFactory(
            managedProvider = null,
            offlineProvider = offline,
            enforceVisionSafety = false
        )

        val resolved = factory.provider(HardwareSettings(aiProvider = "FIREBASE"))

        assertNull("FIREBASE provider must be null when managed runtime is absent", resolved)
        assertNotEquals(offline, resolved)
    }

    @Test
    fun freeProviderAlwaysReturnsOfflineProvider() {
        val managed = TestAiProvider(AiProviderId.FIREBASE)
        val offline = TestAiProvider(AiProviderId.FREE)
        val factory = DefaultAiProviderFactory(
            managedProvider = managed,
            offlineProvider = offline,
            enforceVisionSafety = false
        )

        val resolved = factory.provider(HardwareSettings(aiProvider = "FREE"))

        assertSame(offline, resolved)
    }

    @Test
    fun geminiProviderBehavesConsistentlyWithKeyPresence() {
        val gemini = TestAiProvider(AiProviderId.GEMINI)
        var createdKey: String? = null
        val factory = DefaultAiProviderFactory(
            geminiFactory = { key ->
                createdKey = key
                gemini
            }
        )

        val noKey = factory.provider(HardwareSettings(aiProvider = "GEMINI", geminiApiKey = ""))
        assertNull("GEMINI provider must be null when API key is blank", noKey)

        val withKey = factory.provider(HardwareSettings(aiProvider = "GEMINI", geminiApiKey = "test-key-123"))
        assertSame(gemini, withKey)
        assertEquals("test-key-123", createdKey)
    }

    @Test
    fun firebaseRequestNeverReturnsSuccessWithFreeProvider() = runBlocking {
        val managed = TestAiProvider(AiProviderId.FIREBASE)
        val offline = TestAiProvider(AiProviderId.FREE)
        val factoryWithManaged = DefaultAiProviderFactory(
            managedProvider = managed,
            offlineProvider = offline,
            enforceVisionSafety = false
        )

        val provider = factoryWithManaged.provider(HardwareSettings(aiProvider = "FIREBASE"))
        assertNotNull(provider)
        val result = provider!!.generateRecipeOptions(
            RecipeOptionsRequest(listOf("Egg"), emptySet(), "none", emptySet(), "en")
        )
        assertTrue(result is AiResult.Success)
        val success = result as AiResult.Success<*>
        assertEquals(AiProviderId.FIREBASE, success.provider)
        assertNotEquals(AiProviderId.FREE, success.provider)

        val factoryWithoutManaged = DefaultAiProviderFactory(
            managedProvider = null,
            offlineProvider = offline,
            enforceVisionSafety = false
        )
        assertNull(factoryWithoutManaged.provider(HardwareSettings(aiProvider = "FIREBASE")))
    }
}
