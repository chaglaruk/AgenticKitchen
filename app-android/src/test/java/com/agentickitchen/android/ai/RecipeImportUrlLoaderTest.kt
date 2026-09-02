package com.agentickitchen.android.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportUrlLoaderTest {
    @Test fun visibleTextDropsScriptAndKeepsRecipeContentBounded() {
        val html = "<html><script>secret()</script><body><h1>Rice</h1><p>200 g rice &amp; tomato</p></body></html>"
        val text = RecipeImportUrlLoader.visibleRecipeText(html)
        assertFalse(text.contains("secret"))
        assertTrue(text.contains("Rice"))
        assertTrue(text.contains("200 g rice & tomato"))
    }

    @Test fun rejectsNonHttpAndCredentialUrls() {
        assertTrue(runCatching { RecipeImportUrlLoader.validateUri("file:///tmp/recipe") }.isFailure)
        assertTrue(runCatching { RecipeImportUrlLoader.validateUri("https://user:pass@example.com/r") }.isFailure)
    }

    @Test fun followsBoundedPublicRedirectAndReturnsFinalSource() = runBlocking {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/start") {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/recipe"))
            } else {
                respond("<h1>Recipe</h1>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
            }
        }
        val client = HttpClient(engine) { followRedirects = false; install(HttpTimeout) }
        val loader = RecipeImportUrlLoader(client, ownsClient = false, hostAllowed = { true })
        val loaded = loader.load("https://example.com/start").getOrThrow()
        assertEquals("example.com", loaded.sourceLabel)
        assertTrue(loaded.finalUrl.endsWith("/recipe"))
        assertEquals("<h1>Recipe</h1>", loaded.body)
        client.close()
    }
}
