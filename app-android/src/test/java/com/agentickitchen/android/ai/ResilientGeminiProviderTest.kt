package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResilientGeminiProviderTest {
    @Test
    fun invalidImageResponseUsesGenerateContentFallback() = runBlocking {
        val primary = StubProvider(failure(AiFailureType.InvalidResponse))
        val fallback = RecordingFallback(successfulShopping())
        val provider = ResilientGeminiProvider(primary, fallback)

        val result = provider.scanShoppingPhoto(photoRequest())

        assertTrue(result is AiResult.Success)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun quotaAndSafetyFailuresNeverTriggerFallback() = runBlocking {
        listOf(AiFailureType.RateLimited, AiFailureType.QuotaExceeded, AiFailureType.SafetyBlocked).forEach { type ->
            val fallback = RecordingFallback(successfulShopping())
            val provider = ResilientGeminiProvider(StubProvider(failure(type)), fallback)

            val result = provider.scanShoppingPhoto(photoRequest())

            assertTrue(result is AiResult.Failure)
            assertEquals(type, (result as AiResult.Failure).type)
            assertEquals(0, fallback.calls)
        }
    }

    @Test
    fun generateContentFallbackUsesInlineImageAndStructuredSchema() = runBlocking {
        var url = ""
        var keyHeader: String? = null
        var body = ""
        val diagnostics = mutableListOf<GenerateContentDiagnostic>()
        val responseValue = successfulShopping().value
        val responseText = Json.encodeToString(responseValue)
        val responseBody =
            """{"candidates":[{"content":{"parts":[{"text":${Json.encodeToString(responseText)}}]}}]}"""
        val engine = MockEngine { request ->
            url = request.url.toString()
            keyHeader = request.headers["x-goog-api-key"]
            body = request.body.toByteArray().decodeToString()
            respond(
                responseBody,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val fallback = GenerateContentShoppingPhotoFallback(
            apiKey = "test-key-not-real",
            client = client,
            ownsClient = true,
            diagnosticSink = diagnostics::add
        )

        val result = fallback.scan(photoRequest())

        assertTrue(result is AiResult.Success)
        assertEquals(GenerateContentShoppingPhotoFallback.ENDPOINT, url)
        assertEquals("test-key-not-real", keyHeader)
        assertFalse(url.contains("test-key-not-real"))
        val request = Json.parseToJsonElement(body).jsonObject
        val parts = request["contents"]!!.jsonArray.first().jsonObject["parts"]!!.jsonArray
        val inline = parts.first().jsonObject["inlineData"]!!.jsonObject
        assertEquals("image/jpeg", inline["mimeType"]!!.jsonPrimitive.content)
        assertEquals("AQID", inline["data"]!!.jsonPrimitive.content)
        assertTrue(parts[1].jsonObject["text"]!!.jsonPrimitive.content.contains("visibly supported"))
        val generationConfig = request["generationConfig"]!!.jsonObject
        assertEquals("application/json", generationConfig["responseMimeType"]!!.jsonPrimitive.content)
        assertNotNull(generationConfig["responseJsonSchema"])
        assertFalse("responseFormat" in generationConfig)
        assertEquals("SUCCESS", diagnostics.single().category)
    }

    private fun photoRequest() =
        ShoppingPhotoRequest(KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"), "English")

    private fun successfulShopping() = AiResult.Success(
        ShoppingImportResponse(
            listOf(
                ShoppingCandidate(
                    canonicalIngredientId = "tomato",
                    displayName = "Tomato",
                    quantity = 2.0,
                    unit = "count",
                    unitDimension = "count",
                    confidence = 0.98,
                    estimated = false
                )
            )
        ),
        AiProviderId.GEMINI,
        GeminiProvider.MODEL
    )

    private fun failure(type: AiFailureType) =
        AiResult.Failure(type, type in setOf(AiFailureType.RateLimited, AiFailureType.QuotaExceeded), type.userMessageRes)

    private class RecordingFallback(
        private val result: AiResult<ShoppingImportResponse>
    ) : ShoppingPhotoFallback {
        var calls = 0

        override suspend fun scan(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> {
            calls++
            return result
        }

        override fun close() = Unit
    }

    private class StubProvider(
        private val photoResult: AiResult<ShoppingImportResponse>
    ) : KitchenAiProvider {
        override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> = unavailable()
        override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> = unavailable()
        override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> = unavailable()
        override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> = photoResult
        override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> = unavailable()
        override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> = unavailable()
        override suspend fun testConnection(): AiResult<Unit> = unavailable()

        private fun unavailable() =
            AiResult.Failure(AiFailureType.ProviderUnavailable, false, AiFailureType.ProviderUnavailable.userMessageRes)
    }
}
