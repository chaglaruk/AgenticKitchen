package com.agentickitchen.shared.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.db.AppDatabase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PantryInventoryTest {
    @Test
    fun compatibleUnitsNormalizeAndIncompatibleUnitsAreRejected() {
        val kilograms = InventoryUnits.normalize(1.5, "kg")
        val grams = InventoryUnits.normalize(500.0, "g")
        val litres = InventoryUnits.normalize(1.0, "L")

        assertEquals(NormalizedAmount(1500.0, "g", UnitDimension.WEIGHT), kilograms)
        InventoryUnits.requireCompatible(kilograms, grams)
        assertFailsWith<IllegalArgumentException> {
            InventoryUnits.requireCompatible(kilograms, litres)
        }
        assertFailsWith<IllegalArgumentException> { InventoryUnits.normalize(0.0, "g") }
        assertFailsWith<IllegalArgumentException> { InventoryUnits.normalize(Double.NaN, "g") }
    }

    @Test
    fun migrationAddsInventoryTablesWithoutRecreatingHistory() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE RecipeHistory (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                ingredients TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent(),
            0
        )
        AppDatabase.Schema.migrate(driver, 1, 2)
        val database = AppDatabase(driver)

        assertTrue(database.appDatabaseQueries.selectAllPantryItems().executeAsList().isEmpty())
        database.appDatabaseQueries.insertRecipe("history", "Soup", "tomato", "now", "planned")
        assertEquals("Soup", database.appDatabaseQueries.selectAll().executeAsOne().name)
        driver.close()
    }

    @Test
    fun repositoryInsertsUpdatesDeletesAndKeepsLedger() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val repository = SqlDelightPantryInventoryRepository(AppDatabase(driver))
        val original = item(quantity = 6.0, unit = "adet", dimension = UnitDimension.COUNT)

        repository.upsert(original, adjustment(original, 6.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD))
        repository.upsert(
            original.copy(quantity = 8.0, updatedAt = "later"),
            adjustment(original, 8.0, AdjustmentMode.REPLACE, AdjustmentReason.RECOUNT)
        )

        assertEquals(8.0, repository.getAll().single().quantity)
        assertEquals(2, repository.adjustments(original.id).size)

        repository.delete(
            original,
            adjustment(original, 8.0, AdjustmentMode.REPLACE, AdjustmentReason.DELETION)
        )

        assertTrue(repository.getAll().isEmpty())
        assertEquals(3, repository.adjustments(original.id).size)
        driver.close()
    }

    private fun item(quantity: Double, unit: String, dimension: UnitDimension) = PantryStockItem(
        id = "item-1",
        originalName = "Eggs",
        quantity = quantity,
        unit = unit,
        unitDimension = dimension,
        source = "manual",
        createdAt = "now",
        updatedAt = "now"
    )

    private fun adjustment(
        item: PantryStockItem,
        amount: Double,
        mode: AdjustmentMode,
        reason: AdjustmentReason
    ) = InventoryAdjustmentRecord(
        id = "$reason-$amount",
        itemId = item.id,
        amount = amount,
        mode = mode,
        reason = reason,
        source = "test",
        timestamp = "now"
    )
}
