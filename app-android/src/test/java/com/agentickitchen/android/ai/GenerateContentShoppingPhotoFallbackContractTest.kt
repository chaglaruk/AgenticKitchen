package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateContentShoppingPhotoFallbackContractTest {
    @Test
    fun requestUsesCompatibleGenerateContentStructuredOutputFields() = runBlocking {
        var body = ""
        val engine = MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond(
                """{"candidates":[{"content":{"parts":[{"text":"{\"items\":[{\"canonicalIngredientId\":\"tomato\",\"displayName\":\"Tomato\",\"quantity\":2.0,\"unit\":\"count\",\"unitDimension\":\"count\",\"packageLabel\":null,\"confidence\":0.98,\"estimated\":false,\"uncertaintyReason\":null}]}"}]}}]}""",
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
            diagnosticSink = { }
        )

        val result = fallback.scan(
            ShoppingPhotoRequest(KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"), "English")
        )

        assertTrue(result is AiResult.Success)
        val request = Json.parseToJsonElement(body).jsonObject
        val generationConfig = request["generationConfig"]!!.jsonObject
        assertEquals("application/json", generationConfig["responseMimeType"]!!.jsonPrimitive.content)
        assertFalse("responseFormat" in generationConfig)
        val schema = generationConfig["responseJsonSchema"]?.jsonObject
        assertNotNull(schema)
        val itemProperties = schema!!["properties"]!!.jsonObject["items"]!!.jsonObject["items"]!!.jsonObject["properties"]!!.jsonObject
        val nullableCanonicalIdTypes = itemProperties["canonicalIngredientId"]!!.jsonObject["type"]!!.jsonArray
            .map { it.jsonPrimitive.content }
        assertEquals(listOf("string", "null"), nullableCanonicalIdTypes)
    }
}
