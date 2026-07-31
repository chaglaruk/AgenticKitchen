package com.agentickitchen.shared.cooking

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.ai.CookingChatTurn
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.db.AppDatabase
import com.agentickitchen.shared.inventory.*
import com.agentickitchen.shared.models.ScheduleEvent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActiveCookingSessionRecoveryTest {

    private class TestClock(var current: Long = 1_000_000L) : MonotonicClock {
        override fun nowMillis(): Long = current
    }

    private fun sampleEvent(id: String, startSec: Long, endSec: Long) = ScheduleEvent(
        id = id,
        startIso = "2026-01-01T12:00:${startSec.toString().padStart(2, '0')}Z",
        endIso = "2026-01-01T12:00:${endSec.toString().padStart(2, '0')}Z",
        instruction = "Step $id",
        resource = "stove"
    )

    private fun samplePlan() = CookingPlanResponse(
        recipeName = "Chicken Soup",
        servings = 2,
        ingredients = listOf(PlannedIngredientDto("Chicken", 500.0, "g", canonicalIngredientId = "chicken")),
        steps = listOf(CookingStepDto("step1", "cook", "Boil chicken", 15, "stove", 100)),
        safetyNotes = listOf("Hot surface")
    )

    @Test
    fun `READY survives recreation`() {
        val clock = TestClock(100_000L)
        val controller = CookingSessionController(clock)
        val events = listOf(sampleEvent("a", 0, 60))
        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.READY,
            startedAtMillis = 100_000L,
            accumulatedElapsedSeconds = 0
        )
        assertEquals(CookingSessionStatus.READY, restored.status)
        assertEquals("Soup", restored.recipeName)
        assertEquals(0, restored.elapsedSeconds)
    }

    @Test
    fun `RUNNING survives recreation and includes process death time`() {
        val clock = TestClock(100_000L)
        val controller = CookingSessionController(clock)
        val events = listOf(sampleEvent("a", 0, 120))

        // Started at 100,000, accumulated 10s, app died at 110,000. Relaunch at 140,000 (30s dead time)
        clock.current = 140_000L
        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.RUNNING,
            startedAtMillis = 100_000L,
            accumulatedElapsedSeconds = 10,
            lastRunningStartMillis = 110_000L
        )

        assertEquals(CookingSessionStatus.RUNNING, restored.status)
        // 10s accumulated + (140,000 - 110,000)/1000 = 40s total
        assertEquals(40, restored.elapsedSeconds)

        // Advancing clock 5s should advance elapsed to 45s
        clock.current = 145_000L
        assertEquals(45, controller.current().elapsedSeconds)
    }

    @Test
    fun `PAUSED remains frozen across recreation and resume continues correctly`() {
        val clock = TestClock(100_000L)
        val controller = CookingSessionController(clock)
        val events = listOf(sampleEvent("a", 0, 120))

        // Paused with 15s elapsed, app died. Relaunch 50s later at 150,000L
        clock.current = 150_000L
        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.PAUSED,
            startedAtMillis = 100_000L,
            accumulatedElapsedSeconds = 15,
            pausedAtMillis = 115_000L
        )

        assertEquals(CookingSessionStatus.PAUSED, restored.status)
        assertEquals(15, restored.elapsedSeconds)

        // Time passes while still paused -> remains frozen at 15s
        clock.current = 180_000L
        assertEquals(15, controller.current().elapsedSeconds)

        // Resume at 180,000L -> status becomes RUNNING, continues from 15s
        val resumed = controller.resume()
        assertEquals(CookingSessionStatus.RUNNING, resumed.status)
        assertEquals(15, resumed.elapsedSeconds)

        // Advance 10s -> becomes 25s
        clock.current = 190_000L
        assertEquals(25, controller.current().elapsedSeconds)
    }

    @Test
    fun `completed and skipped steps survive restoration`() {
        val clock = TestClock(100_000L)
        val controller = CookingSessionController(clock)
        val events = listOf(sampleEvent("a", 0, 30), sampleEvent("b", 30, 60), sampleEvent("c", 60, 90))

        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.RUNNING,
            startedAtMillis = 100_000L,
            accumulatedElapsedSeconds = 40,
            completed = setOf("a"),
            skipped = setOf("b")
        )

        assertTrue(restored.completed.contains("a"))
        assertTrue(restored.skipped.contains("b"))
        assertFalse(restored.completed.contains("b"))
    }

    @Test
    fun `full plan round-trips through SQLDelight active session repository`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)
        val repo = SqlDelightPantryInventoryRepository(database)

        val json = Json { ignoreUnknownKeys = true }
        val plan = samplePlan()
        val events = listOf(sampleEvent("a", 0, 60))
        val plannedUsage = listOf(
            PlannedPantryUsage("item-1", "Chicken", 1000.0, 500.0, 500.0, "g")
        )
        val turns = listOf(CookingChatTurn("user", "Is it hot?"), CookingChatTurn("assistant", "Yes."))

        val record = ActiveCookingSessionRecord(
            sessionId = "session-123",
            recipeOptionId = "opt-1",
            recipeName = "Chicken Soup",
            recipeType = "Soup",
            description = "Hot soup",
            sourceLabel = "Gemini",
            servings = 2,
            resolvedReadyTimeIso = "2026-01-01T13:00:00Z",
            cookingPlanJson = json.encodeToString(CookingPlanResponse.serializer(), plan),
            eventsJson = json.encodeToString(ListSerializer(ScheduleEvent.serializer()), events),
            plannedUsageJson = json.encodeToString(ListSerializer(PlannedPantryUsage.serializer()), plannedUsage),
            status = "RUNNING",
            startedAtMillis = 100_000L,
            accumulatedElapsedSeconds = 25,
            lastRunningStartMillis = 120_000L,
            pausedAtMillis = null,
            completedStepIdsJson = json.encodeToString(SetSerializer(String.serializer()), setOf("step1")),
            skippedStepIdsJson = json.encodeToString(SetSerializer(String.serializer()), emptySet()),
            recentChatTurnsJson = json.encodeToString(ListSerializer(CookingChatTurn.serializer()), turns),
            updatedAtIso = "2026-01-01T12:30:00Z"
        )

        repo.saveActiveSession(record)
        val loaded = repo.getActiveSession("session-123")
        assertNotNull(loaded)
        assertEquals("session-123", loaded.sessionId)
        assertEquals("Chicken Soup", loaded.recipeName)
        assertEquals(25, loaded.accumulatedElapsedSeconds)

        val restoredPlan = json.decodeFromString(CookingPlanResponse.serializer(), loaded.cookingPlanJson)
        assertEquals("Chicken Soup", restoredPlan.recipeName)

        val restoredTurns = json.decodeFromString(ListSerializer(CookingChatTurn.serializer()), loaded.recentChatTurnsJson)
        assertEquals(2, restoredTurns.size)
        assertEquals("Is it hot?", restoredTurns.first().text)

        driver.close()
    }

    @Test
    fun `migration 2_sqm adds ActiveCookingSession table without data loss`() {
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
            );
            """.trimIndent(),
            0
        )
        // Run migration 1 and 2
        AppDatabase.Schema.migrate(driver, 1, 3)
        val database = AppDatabase(driver)

        assertTrue(database.appDatabaseQueries.selectAllActiveCookingSessions().executeAsList().isEmpty())
        driver.close()
    }

    @Test
    fun `reservation release creates balancing ledger records and deletes pending usage`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)
        val repository = SqlDelightPantryInventoryRepository(database)

        val item = PantryStockItem(
            id = "chicken-1",
            originalName = "Chicken",
            quantity = 1000.0,
            unit = "g",
            unitDimension = UnitDimension.WEIGHT,
            source = "manual",
            createdAt = "now",
            updatedAt = "now"
        )
        repository.upsert(
            item,
            InventoryAdjustmentRecord("adj-1", item.id, 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", "now")
        )

        val usage = PendingRecipeUsageRecord("session-99", item.id, 400.0, "g", status = "reserved", timestamp = "now")
        assertTrue(repository.reserve(listOf(usage)))

        // Ledger has MANUAL_ADD and RECIPE_RESERVATION
        assertEquals(2, repository.adjustments(item.id).size)

        // Cancel reservation
        assertTrue(repository.releaseReservation("session-99"))

        // Pending usage deleted
        assertTrue(repository.pendingUsage("session-99").isEmpty())

        // Ledger contains RECIPE_RESERVATION_RELEASE
        val ledger = repository.adjustments(item.id)
        assertEquals(3, ledger.size)
        val releaseEntry = ledger.first { it.reason == AdjustmentReason.RECIPE_RESERVATION_RELEASE }
        assertEquals(400.0, releaseEntry.amount)

        driver.close()
    }

    @Test
    fun `repeated recovery does not double deduct or duplicate ledger entries`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)
        val repository = SqlDelightPantryInventoryRepository(database)

        val item = PantryStockItem(
            id = "item-1",
            originalName = "Milk",
            quantity = 1000.0,
            unit = "ml",
            unitDimension = UnitDimension.VOLUME,
            source = "manual",
            createdAt = "now",
            updatedAt = "now"
        )
        repository.upsert(
            item,
            InventoryAdjustmentRecord("adj-1", item.id, 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", "now")
        )

        val usage = PendingRecipeUsageRecord("sess-1", item.id, 300.0, "ml", status = "reserved", timestamp = "now")
        assertTrue(repository.reserve(listOf(usage)))

        val pendingBefore = repository.allPendingUsage()
        assertEquals(1, pendingBefore.size)

        // Simulate app relaunching and reloading pending usage
        val pendingAfter = repository.allPendingUsage()
        assertEquals(1, pendingAfter.size)
        assertEquals(2, repository.adjustments(item.id).size)

        driver.close()
    }
}
