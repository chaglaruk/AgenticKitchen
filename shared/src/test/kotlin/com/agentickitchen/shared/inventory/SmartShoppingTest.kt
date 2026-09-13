package com.agentickitchen.shared.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.db.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartShoppingTest {
    @Test fun shortageDetailsUseOnlyMissingAmountAfterStockAndReservation() {
        val plan = CookingPlanResponse(
            "Rice", 2,
            listOf(PlannedIngredientDto("Rice", 200.0, "g", "rice")),
            listOf(CookingStepDto("s", "cook", "Cook", "stovetop", 60)),
            emptyList()
        )
        val stock = PantryStockItem(
            id="rice-stock", canonicalIngredientId="rice", originalName="Rice", quantity=150.0,
            unit="g", unitDimension=UnitDimension.WEIGHT, source="manual", createdAt="t", updatedAt="t"
        )
        val usage = InventoryWorkflow.planUsage(plan, listOf(stock), mapOf("rice-stock" to 20.0))
        assertEquals(listOf("Rice"), usage.shortages)
        assertEquals(1, usage.shortageDetails.size)
        assertEquals(70.0, usage.shortageDetails.single().missingQuantity, 0.0001)
        assertEquals("g", usage.shortageDetails.single().unit)
    }

    @Test fun plannerReusesRecipeItemInsteadOfDuplicatingAndClassifiesLocally() {
        val shortage = PantryShortage("Tomato", "tomato", 3.0, 1.0, 2.0, "adet")
        val first = SmartShoppingPlanner.planForRecipe(emptyList(), listOf(shortage), "recipe", "Soup", "t1") { "id-1" }
        val second = SmartShoppingPlanner.planForRecipe(first, listOf(shortage.copy(missingQuantity = 1.0)), "recipe", "Soup", "t2") { "id-2" }
        assertEquals("id-1", second.single().id)
        assertEquals(1.0, second.single().quantity, 0.0001)
        assertEquals(ShoppingCategory.PRODUCE, second.single().category)
    }

    @Test fun sqlRepositoryReplacesRecipeRowsAndPersistsCheckedState() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            AppDatabase.Schema.create(driver)
            val repository = SqlDelightShoppingListRepository(AppDatabase(driver))
            val item = ShoppingListItem("1", "rice", "Rice", 100.0, "g", ShoppingCategory.PANTRY, "recipe", "Rice bowl", false, "t1", "t1")
            repository.replaceRecipeShortages("recipe", listOf(item))
            assertEquals(listOf("1"), repository.getAll().map { it.id })
            repository.setChecked("1", true)
            assertTrue(repository.getAll().single().checked)
            repository.replaceRecipeShortages("recipe", emptyList())
            assertTrue(repository.getAll().isEmpty())
        } finally {
            driver.close()
        }
    }
}
