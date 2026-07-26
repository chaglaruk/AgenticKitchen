package com.agentickitchen.android.ai

import android.graphics.Bitmap
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

@Serializable
data class HFVisionResponse(val generated_text: String)

class HuggingFaceVisionService(private val apiKey: String = "") {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun analyzeImage(bitmap: Bitmap): String? {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()

            // Using Salesforce BLIP for free image captioning
            val response: List<HFVisionResponse> = client.post("https://api-inference.huggingface.co/models/Salesforce/blip-image-captioning-large") {
                if (apiKey.isNotEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                contentType(ContentType.Image.JPEG)
                setBody(byteArray)
            }.body()

            response.firstOrNull()?.generated_text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
