package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.inventory.InventoryWorkflow
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.UnitDimension
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryAwareOfflineProviderTest {
    @Test
    fun strictStockOptionsFitSmallInventoryInsteadOfFailingOnOfflineDefaultPortions() = runBlocking {
        val provider = InventoryAwareOfflineProvider(LocalRecipeProvider { })
        val inventoryLines = listOf("50 g Domates", "100 g Pirinç", "2 adet Soğan", "40 g Mantar")
        val request = RecipeOptionsRequest(
            ingredients = listOf("Domates", "Pirinç", "Soğan", "Mantar"),
            equipment = setOf("elec", "pan"),
            dietType = "none",
            allergies = emptySet(),
            language = "Türkçe",
            inventoryLines = inventoryLines,
            strictStock = true,
            servings = 2
        )

        val result = provider.generateRecipeOptions(request)

        assertTrue(result is AiResult.Success)
        val options = (result as AiResult.Success).value.options
        assertEquals(3, options.size)
        assertTrue(options.all { it.proposedIngredients.isNotEmpty() })
        assertTrue(options.all { option -> option.proposedIngredients.any { it.name == "Pirinç" && it.quantity == 100.0 && it.unit == "g" } })
        assertTrue(options.all { option ->
            InventoryWorkflow.planUsage(
                plan = com.agentickitchen.shared.ai.dto.CookingPlanResponse(
                    recipeName = option.name,
                    servings = 2,
                    ingredients = option.proposedIngredients,
                    steps = emptyList(),
                    safetyNotes = emptyList()
                ),
                inventory = pantry()
            ).shortages.isEmpty()
        })
    }

    @Test
    fun selectedOfflineInventoryPlanUsesTheSameStockFittedQuantities() = runBlocking {
        val provider = InventoryAwareOfflineProvider(LocalRecipeProvider { })
        val request = CookingPlanRequest(
            recipeName = "Pirinç ve Domates Tavası",
            ingredients = listOf("Domates", "Pirinç", "Soğan", "Mantar"),
            equipment = setOf("elec", "pan"),
            servings = 2,
            stoveType = "electric",
            stoveMaxLevel = 9,
            ovenAvailable = false,
            ovenHasFan = false,
            airfryerAvailable = false,
            dietType = "none",
            allergies = emptySet(),
            language = "Türkçe",
            inventoryLines = listOf("50 g Domates", "100 g Pirinç", "2 adet Soğan", "40 g Mantar")
        )

        val result = provider.generateCookingPlan(request)

        assertTrue(result is AiResult.Success)
        val plan = (result as AiResult.Success).value
        assertTrue(plan.ingredients.any { it.name == "Pirinç" && it.quantity == 100.0 && it.unit == "g" })
        assertTrue(InventoryWorkflow.planUsage(plan, pantry()).shortages.isEmpty())
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

    private fun pantry() = listOf(
        stock("domates", "Domates", 50.0, "g", UnitDimension.WEIGHT),
        stock("pirinc", "Pirinç", 100.0, "g", UnitDimension.WEIGHT),
        stock("sogan", "Soğan", 2.0, "adet", UnitDimension.COUNT),
        stock("mantar", "Mantar", 40.0, "g", UnitDimension.WEIGHT)
    )

    private fun stock(id: String, name: String, quantity: Double, unit: String, dimension: UnitDimension) = PantryStockItem(
        id = id,
        originalName = name,
        quantity = quantity,
        unit = unit,
        unitDimension = dimension,
        source = "test",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )
}
