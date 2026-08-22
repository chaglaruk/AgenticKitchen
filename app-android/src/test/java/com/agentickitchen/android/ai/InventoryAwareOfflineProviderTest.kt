package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryAwareOfflineProviderTest {
    @Test
    fun inventoryRecipeOptionsIncludeDeterministicProposedIngredients() = runBlocking {
        val provider = InventoryAwareOfflineProvider(LocalRecipeProvider { })
        val request = RecipeOptionsRequest(
            ingredients = listOf("Domates", "Pirinç", "Soğan"),
            equipment = setOf("elec", "pan"),
            dietType = "none",
            allergies = emptySet(),
            language = "Türkçe",
            inventoryLines = listOf("500 g Domates", "500 g Pirinç", "5 adet Soğan"),
            strictStock = true,
            servings = 2
        )

        val result = provider.generateRecipeOptions(request)

        assertTrue(result is AiResult.Success)
        val options = (result as AiResult.Success).value.options
        assertEquals(3, options.size)
        assertTrue(options.all { it.proposedIngredients.isNotEmpty() })
        assertTrue(options.all { option ->
            option.proposedIngredients.all { it.quantity > 0.0 && it.unit.isNotBlank() }
        })
        assertTrue(options.all { option -> option.proposedIngredients.any { it.name == "Pirinç" && it.quantity == 150.0 && it.unit == "g" } })
    }

    @Test
    fun ordinaryOfflineRecipeOptionsRemainUnhydrated() = runBlocking {
        val provider = InventoryAwareOfflineProvider(LocalRecipeProvider { })
        val request = RecipeOptionsRequest(
            ingredients = listOf("Domates", "Pirinç", "Soğan"),
            equipment = setOf("elec", "pan"),
            dietType = "none",
            allergies = emptySet(),
            language = "Türkçe"
        )

        val result = provider.generateRecipeOptions(request)

        assertTrue(result is AiResult.Success)
        assertTrue((result as AiResult.Success).value.options.all { it.proposedIngredients.isEmpty() })
    }
}
