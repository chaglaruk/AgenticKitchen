package com.agentickitchen.shared.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
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

    @Test
    fun shoppingAddAndRecountUseLocalCompatibleUnitArithmetic() {
        val existing = item(quantity = 500.0, unit = "g", dimension = UnitDimension.WEIGHT)
            .copy(originalName = "Tavuk", canonicalIngredientId = "chicken")
        val candidate = candidate("Chicken", 1.0, "kg", "weight", canonicalId = "chicken")

        val added = InventoryWorkflow.planImport(
            listOf(existing),
            listOf(candidate),
            ShoppingImportMode.ADD,
            "later",
            sequenceIds()
        )
        val recounted = InventoryWorkflow.planImport(
            listOf(existing),
            listOf(candidate),
            ShoppingImportMode.RECOUNT,
            "later",
            sequenceIds()
        )

        assertTrue(added.conflicts.isEmpty())
        assertEquals(1500.0, added.mutations.single().item.quantity)
        assertEquals(1000.0, recounted.mutations.single().item.quantity)
        assertEquals(AdjustmentMode.REPLACE, recounted.mutations.single().adjustment.mode)
    }

    @Test
    fun recountProtectsUnseenItemsAndReviewExclusionStaysExcluded() {
        val eggs = item(6.0, "adet", UnitDimension.COUNT)
        val milk = item(1000.0, "ml", UnitDimension.VOLUME).copy(id = "milk", originalName = "Milk")
        val includedAfterReview = listOf(candidate("Eggs", 12.0, "adet", "count"))

        val plan = InventoryWorkflow.planImport(
            listOf(eggs, milk),
            includedAfterReview,
            ShoppingImportMode.RECOUNT,
            "later",
            sequenceIds()
        )

        assertEquals(listOf("item-1"), plan.mutations.map { it.item.id })
        assertEquals(12.0, plan.mutations.single().item.quantity)
        assertEquals(1000.0, milk.quantity)
    }

    @Test
    fun packageVisibleWeightConvertsAndIncompatibleUnitsConflict() {
        val pasta = item(500.0, "g", UnitDimension.WEIGHT).copy(originalName = "Pasta")
        val packages = candidate("Pasta", 2.0, "package", "package", packageLabel = "2 x 500 g")
        val incompatible = candidate("Pasta", 2.0, "adet", "count")

        val converted = InventoryWorkflow.planImport(
            listOf(pasta),
            listOf(packages),
            ShoppingImportMode.ADD,
            "later",
            sequenceIds()
        )
        val conflict = InventoryWorkflow.planImport(
            listOf(pasta),
            listOf(incompatible),
            ShoppingImportMode.ADD,
            "later",
            sequenceIds()
        )

        assertEquals(1500.0, converted.mutations.single().item.quantity)
        assertEquals("2 x 500 g", converted.mutations.single().item.packageLabel)
        assertEquals(listOf("Pasta"), conflict.conflicts)
    }

    @Test
    fun stockPlanningAccountsForReservationsAndShortages() {
        val chicken = item(1000.0, "g", UnitDimension.WEIGHT).copy(originalName = "Chicken")
        val plan = CookingPlanResponse(
            recipeName = "Chicken",
            servings = 2,
            ingredients = listOf(
                PlannedIngredientDto("Chicken", 650.0, "g"),
                PlannedIngredientDto("Milk", 200.0, "ml")
            ),
            steps = emptyList(),
            safetyNotes = emptyList()
        )

        val usage = InventoryWorkflow.planUsage(plan, listOf(chicken), mapOf(chicken.id to 400.0))

        assertTrue(usage.usages.isEmpty())
        assertEquals(listOf("Chicken", "Milk"), usage.shortages)
    }

    @Test
    fun reservationsPreventDoubleUseAndSurviveRepositoryRecreation() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)
        val repository = SqlDelightPantryInventoryRepository(database)
        val stock = item(1000.0, "g", UnitDimension.WEIGHT)
        repository.upsert(stock, adjustment(stock, 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD))

        assertTrue(repository.reserve(listOf(pending("session-1", stock, 700.0))))
        assertTrue(!repository.reserve(listOf(pending("session-2", stock, 400.0))))
        assertEquals("session-1", SqlDelightPantryInventoryRepository(database).allPendingUsage().single().sessionId)

        repository.deletePendingUsage("session-1")
        assertTrue(repository.reserve(listOf(pending("session-2", stock, 400.0))))
        driver.close()
    }

    @Test
    fun consumptionIsAtomicAndSupportsPlannedOrEditedAmounts() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val repository = SqlDelightPantryInventoryRepository(AppDatabase(driver))
        val chicken = item(1000.0, "g", UnitDimension.WEIGHT)
        val milk = item(500.0, "ml", UnitDimension.VOLUME).copy(id = "milk", originalName = "Milk")
        repository.upsert(chicken, adjustment(chicken, 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD))
        repository.upsert(milk, adjustment(milk, 500.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD))
        assertTrue(repository.reserve(listOf(pending("session", chicken, 300.0), pending("session", milk, 200.0))))

        assertTrue(!repository.consume("session", mapOf(chicken.id to 250.0, milk.id to 900.0)))
        assertEquals(listOf(1000.0, 500.0), repository.getAll().sortedBy { it.id }.map { it.quantity })
        assertTrue(repository.consume("session", mapOf(chicken.id to 250.0, milk.id to 100.0)))
        assertEquals(750.0, repository.getAll().first { it.id == chicken.id }.quantity)
        assertEquals(400.0, repository.getAll().first { it.id == milk.id }.quantity)
        assertTrue(repository.pendingUsage("session").isEmpty())
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

    private fun pending(sessionId: String, item: PantryStockItem, quantity: Double) =
        PendingRecipeUsageRecord(sessionId, item.id, quantity, item.unit, status = "reserved", timestamp = "now")

    private fun candidate(
        name: String,
        quantity: Double,
        unit: String,
        dimension: String,
        canonicalId: String? = null,
        packageLabel: String? = null
    ) = ShoppingCandidate(
        canonicalIngredientId = canonicalId,
        displayName = name,
        quantity = quantity,
        unit = unit,
        unitDimension = dimension,
        packageLabel = packageLabel,
        confidence = 0.9,
        estimated = false
    )

    private fun sequenceIds(): () -> String {
        var next = 0
        return { "generated-${next++}" }
    }
}
