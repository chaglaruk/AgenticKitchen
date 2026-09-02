package com.agentickitchen.android.ai

import com.agentickitchen.android.AiConnectionStatus
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.aiConnectionStatusFor
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatTurn
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiProviderTest {
    @Test
    fun recipeRequestUsesInteractionsHeaderModelStoreAndStructuredSchema() = runBlocking {
        var url = ""
        var keyHeader: String? = null
        var body = ""
        val provider = provider { request ->
            url = request.url.toString()
            keyHeader = request.headers["x-goog-api-key"]
            body = request.body.toByteArray().decodeToString()
            respondJson(modelResponse(recipeOptionsJson))
        }

        val result = provider.generateRecipeOptions(recipeRequest())

        assertTrue(result is AiResult.Success<*>)
        assertEquals(GeminiProvider.ENDPOINT, url)
        assertEquals(API_KEY, keyHeader)
        assertFalse(url.contains(API_KEY))
        val request = Json.parseToJsonElement(body).jsonObject
        assertEquals(GeminiProvider.MODEL, request["model"]!!.jsonPrimitive.content)
        assertFalse(request["store"]!!.jsonPrimitive.boolean)
        assertTrue(request["input"]!!.jsonPrimitive.content.contains("Domates"))
        assertEquals("application/json", request["response_format"]!!.jsonObject["mime_type"]!!.jsonPrimitive.content)
        assertNotNull(request["response_format"]!!.jsonObject["schema"])
    }

    @Test
    fun inlineImageUsesDeclaredMimeTypeAndBase64Data() = runBlocking {
        var body = ""
        val provider = provider { request ->
            body = request.body.toByteArray().decodeToString()
            respondJson(modelResponse(shoppingJson))
        }

        val result = provider.scanShoppingPhoto(
            ShoppingPhotoRequest(KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"), "English")
        )

        assertTrue(result is AiResult.Success<*>)
        val input = Json.parseToJsonElement(body).jsonObject["input"]!!.jsonArray
        assertEquals("image", input[1].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("image/jpeg", input[1].jsonObject["mime_type"]!!.jsonPrimitive.content)
        assertEquals("AQID", input[1].jsonObject["data"]!!.jsonPrimitive.content)
    }

    @Test
    fun turkishAndEnglishShoppingTextUseTheTypedReviewResponse() = runBlocking {
        val prompts = mutableListOf<String>()
        val provider = provider { request ->
            prompts += request.body.toByteArray().decodeToString()
            respondJson(modelResponse(shoppingJson))
        }

        val turkish = provider.parseShoppingText(
            ShoppingTextRequest("2 paket makarna, 1 kilo tavuk ve 12 yumurta aldım", "Türkçe")
        )
        val english = provider.parseShoppingText(
            ShoppingTextRequest("I bought 2 litres of milk, six tomatoes and a loaf of bread", "English")
        )

        assertEquals("Tomato", (turkish as AiResult.Success).value.items.single().displayName)
        assertEquals("Tomato", (english as AiResult.Success).value.items.single().displayName)
        assertTrue(prompts[0].contains("2 paket makarna"))
        assertTrue(prompts[1].contains("2 litres of milk"))
    }

    @Test
    fun cookingChatSendsRecentTurnsAndCurrentRecipeContext() = runBlocking {
        var body = ""
        val provider = provider { request ->
            body = request.body.toByteArray().decodeToString()
            respondJson(modelResponse("""{"answer":"Reduce the heat."}"""))
        }
        val result = provider.askCookingAssistant(
            CookingChatRequest(
                recipeName = "Tomato pasta",
                plan = CookingPlanResponse("Tomato pasta", 2, emptyList(), emptyList(), emptyList()),
                currentStep = "Simmer the sauce",
                elapsedSeconds = 120,
                resource = "stovetop",
                recentTurns = listOf(
                    CookingChatTurn("user", "Is it bubbling?"),
                    CookingChatTurn("assistant", "Keep watching the edges.")
                ),
                question = "What now?",
                language = "English"
            )
        )

        assertTrue(result is AiResult.Success)
        assertTrue(body.contains("Tomato pasta"))
        assertTrue(body.contains("Is it bubbling?"))
        assertTrue(body.contains("Keep watching the edges."))
        assertTrue(body.contains("What now?"))
    }

    @Test
    fun oversizedImageIsRejectedBeforeNetworkTransmission() = runBlocking {
        var requests = 0
        val provider = provider {
            requests++
            respondJson(modelResponse(shoppingJson))
        }

        val result = provider.scanShoppingPhoto(
            ShoppingPhotoRequest(
                KitchenImage(ByteArray(GeminiProvider.MAX_INLINE_IMAGE_BYTES + 1), "image/jpeg"),
                "English"
            )
        )

        assertFailure(result, AiFailureType.InvalidResponse)
        assertEquals(0, requests)
    }

    @Test
    fun missingKeyIsRejectedBeforeNetworkTransmission() = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respondJson(modelResponse(recipeOptionsJson))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val provider = GeminiProvider("", client, true, {})

        assertFailure(provider.generateRecipeOptions(recipeRequest()), AiFailureType.MissingCredential)
        assertEquals(0, requests)
    }

    @Test
    fun httpFailuresMapToTypedReaderSafeCategories() = runBlocking {
        mapOf(
            HttpStatusCode.BadRequest to AiFailureType.InvalidResponse,
            HttpStatusCode.Unauthorized to AiFailureType.Unauthorized,
            HttpStatusCode.Forbidden to AiFailureType.Unauthorized,
            HttpStatusCode.TooManyRequests to AiFailureType.RateLimited,
            HttpStatusCode.InternalServerError to AiFailureType.ProviderUnavailable
        ).forEach { (status, type) ->
            val provider = provider { respondJson("""{"error":{"code":${status.value}}}""", status) }
            assertFailure(provider.generateRecipeOptions(recipeRequest()), type)
        }
    }

    @Test
    fun timeoutMalformedEmptySafetyAndSchemaFailuresRemainDistinct() = runBlocking {
        val timeout = provider { request -> throw HttpRequestTimeoutException(request) }
        assertFailure(timeout.generateRecipeOptions(recipeRequest()), AiFailureType.Timeout)

        val malformed = provider { respondJson("{") }
        assertFailure(malformed.generateRecipeOptions(recipeRequest()), AiFailureType.InvalidResponse)

        val empty = provider { respondJson("""{"status":"completed","steps":[]}""") }
        assertFailure(empty.generateRecipeOptions(recipeRequest()), AiFailureType.InvalidResponse)

        val safety = provider { respondJson("""{"status":"failed","error":{"message":"safety policy"}}""") }
        assertFailure(safety.generateRecipeOptions(recipeRequest()), AiFailureType.SafetyBlocked)

        val schemaInvalid = provider { respondJson(modelResponse("""{"options":[]}""")) }
        assertFailure(schemaInvalid.generateRecipeOptions(recipeRequest()), AiFailureType.InvalidResponse)
    }

    @Test
    fun successfulStructuredResponseIsParsedAndDiagnosticsContainMetadataOnly() = runBlocking {
        val diagnostics = mutableListOf<GeminiDiagnostic>()
        val promptSecret = "private-prompt-text"
        val responseSecret = "Secret Recipe"
        val provider = provider(
            diagnostics = diagnostics,
            response = modelResponse(recipeOptionsJson.replace("Domates Tavası", responseSecret))
        )

        val result = provider.generateRecipeOptions(recipeRequest(ingredients = listOf(promptSecret)))

        assertEquals(responseSecret, (result as AiResult.Success).value.options.first().name)
        val log = diagnostics.single().asLogMessage()
        assertFalse(log.contains(API_KEY))
        assertFalse(log.contains(promptSecret))
        assertFalse(log.contains(responseSecret))
        assertTrue(log.contains("feature=recipe_options"))
        assertTrue(log.contains("responseLength="))
    }

    @Test
    fun providerFactorySwitchesBetweenGeminiAndOfflineWithoutSilentFallback() {
        val created = mutableListOf<String>()
        val offline = RecordingProvider(AiProviderId.FREE)
        val factory = DefaultAiProviderFactory(
            geminiFactory = { key -> RecordingProvider(AiProviderId.GEMINI).also { created += key } },
            offlineProvider = offline
        )

        assertNull(factory.provider(HardwareSettings(aiProvider = "GEMINI")))
        assertEquals(AiProviderId.GEMINI, (factory.provider(HardwareSettings(aiProvider = "GEMINI", geminiApiKey = "one")) as RecordingProvider).id)
        assertEquals(AiProviderId.FREE, (factory.provider(HardwareSettings(aiProvider = "FREE")) as RecordingProvider).id)
        assertEquals(listOf("one"), created)
    }

    @Test
    fun connectionTestStatesAreReaderFacingAndDeterministic() {
        assertEquals(AiConnectionStatus.CONNECTED, aiConnectionStatusFor(AiResult.Success(Unit, AiProviderId.GEMINI, GeminiProvider.MODEL)))
        assertEquals(AiConnectionStatus.NOT_CONFIGURED, aiConnectionStatusFor(failure(AiFailureType.MissingCredential)))
        assertEquals(AiConnectionStatus.INVALID_KEY, aiConnectionStatusFor(failure(AiFailureType.Unauthorized)))
        assertEquals(AiConnectionStatus.QUOTA_UNAVAILABLE, aiConnectionStatusFor(failure(AiFailureType.RateLimited)))
        assertEquals(AiConnectionStatus.NETWORK_FAILURE, aiConnectionStatusFor(failure(AiFailureType.NetworkUnavailable)))
    }

    private fun provider(
        diagnostics: MutableList<GeminiDiagnostic> = mutableListOf(),
        response: String? = null,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData = {
            respondJson(requireNotNull(response))
        }
    ): GeminiProvider {
        val engine = MockEngine { request ->
            handler(this, request)
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return GeminiProvider(API_KEY, client, true, diagnostics::add)
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun modelResponse(output: String) =
        """{"status":"completed","steps":[{"type":"model_output","content":[{"type":"text","text":${Json.encodeToString(output)}}]}]}"""

    private fun recipeRequest(ingredients: List<String> = listOf("Domates")) = RecipeOptionsRequest(
        ingredients = ingredients,
        equipment = setOf("pan", "elec"),
        dietType = "none",
        allergies = emptySet(),
        language = "English"
    )

    private fun assertFailure(result: AiResult<*>, type: AiFailureType) {
        assertTrue("Expected failure but was $result", result is AiResult.Failure)
        assertEquals(type, (result as AiResult.Failure).type)
    }

    private fun failure(type: AiFailureType) = AiResult.Failure(type, false, type.userMessageRes)

    private class RecordingProvider(val id: AiProviderId) : KitchenAiProvider {
        override suspend fun generateRecipeOptions(request: com.agentickitchen.shared.ai.RecipeOptionsRequest) = failure()
        override suspend fun generateCookingPlan(request: com.agentickitchen.shared.ai.CookingPlanRequest) = failure()
        override suspend fun parseShoppingText(request: com.agentickitchen.shared.ai.ShoppingTextRequest) = failure()
        override suspend fun scanShoppingPhoto(request: com.agentickitchen.shared.ai.ShoppingPhotoRequest) = failure()
        override suspend fun inspectCookingPhoto(request: com.agentickitchen.shared.ai.CookingPhotoRequest) = failure()
        override suspend fun askCookingAssistant(request: com.agentickitchen.shared.ai.CookingChatRequest) = failure()
        override suspend fun testConnection() = AiResult.Success(Unit, id, "recording")
        private fun failure() = AiResult.Failure(AiFailureType.ProviderUnavailable, false, "offline")
    }

    private companion object {
        const val API_KEY = "test-key-not-real"
        const val recipeOptionsJson =
            """{"options":[{"id":"1","name":"Domates Tavası","summary":"Sade","difficulty":"easy","estimatedMinutes":20,"requiredEquipment":["pan"],"missingIngredients":[]},{"id":"2","name":"Domates Çorbası","summary":"Sıcak","difficulty":"easy","estimatedMinutes":30,"requiredEquipment":["pan"],"missingIngredients":[]},{"id":"3","name":"Domates Salatası","summary":"Taze","difficulty":"easy","estimatedMinutes":10,"requiredEquipment":[],"missingIngredients":[]}]}"""
        const val shoppingJson =
            """{"items":[{"canonicalIngredientId":"tomato","displayName":"Tomato","quantity":6.0,"unit":"count","unitDimension":"count","packageLabel":null,"confidence":0.98,"estimated":false,"uncertaintyReason":null}]}"""
    }
}
