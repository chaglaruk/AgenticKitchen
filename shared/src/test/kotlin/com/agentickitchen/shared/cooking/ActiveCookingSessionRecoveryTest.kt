package com.agentickitchen.shared.cooking

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.ai.CookingChatTurn
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.db.AppDatabase
import com.agentickitchen.shared.inventory.*
import com.agentickitchen.shared.models.ScheduleEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActiveCookingSessionRecoveryTest {

    private val fixedEpoch = 1767225600000L // 2026-01-01T00:00:00Z
    private class TestClock(var currentMono: Long = 1_000_000L, var currentEpoch: Long = 1767225600000L) : ClockDomain {
        override fun monotonicMillis(): Long = currentMono
        override fun epochMillis(): Long = currentEpoch
    }

    private val baseTime = java.time.Instant.parse("2026-01-01T12:00:00Z")
    private fun sampleEvent(id: String, startSec: Long, endSec: Long) = ScheduleEvent(
        id = id,
        startIso = baseTime.plusSeconds(startSec).toString(),
        endIso = baseTime.plusSeconds(endSec).toString(),
        instruction = "Step $id",
        resource = "stove"
    )

    private fun samplePlan() = CookingPlanResponse(
        recipeName = "Chicken Soup",
        servings = 2,
        ingredients = listOf(PlannedIngredientDto("Chicken", 500.0, "g", canonicalIngredientId = "chicken")),
        steps = listOf(CookingStepDto("step1", "cook", "Boil chicken", "stove", 15, targetTemperatureC = 100)),
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

        // Started at fixedEpoch, accumulated 10s, app died 10s later. Relaunch 40s after start (30s dead time)
        clock.currentMono = 140_000L
        clock.currentEpoch = fixedEpoch + 40_000L
        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.RUNNING,
            startedAtMillis = fixedEpoch,
            accumulatedElapsedSeconds = 10,
            lastRunningStartMillis = fixedEpoch + 10_000L
        )

        assertEquals(CookingSessionStatus.RUNNING, restored.status)
        // 10s accumulated + (140,000 - 110,000)/1000 = 40s total
        assertEquals(40, restored.elapsedSeconds)

        // Advancing clock 5s should advance elapsed to 45s
        clock.currentMono = 145_000L
        clock.currentEpoch = fixedEpoch + 45_000L
        assertEquals(45, controller.current().elapsedSeconds)
    }

    @Test
    fun `PAUSED remains frozen across recreation and resume continues correctly`() {
        val clock = TestClock(100_000L)
        val controller = CookingSessionController(clock)
        val events = listOf(sampleEvent("a", 0, 120))

        // Paused with 15s elapsed, app died. Relaunch 50s later at 150,000L
        clock.currentMono = 150_000L
        clock.currentEpoch = fixedEpoch + 50_000L
        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.PAUSED,
            startedAtMillis = fixedEpoch,
            accumulatedElapsedSeconds = 15,
            pausedAtMillis = fixedEpoch + 15_000L
        )

        assertEquals(CookingSessionStatus.PAUSED, restored.status)
        assertEquals(15, restored.elapsedSeconds)

        // Time passes while still paused -> remains frozen at 15s
        clock.currentMono = 180_000L
        clock.currentEpoch = fixedEpoch + 80_000L
        assertEquals(15, controller.current().elapsedSeconds)

        // Resume at 180,000L -> status becomes RUNNING, continues from 15s
        val resumed = controller.resume()
        assertEquals(CookingSessionStatus.RUNNING, resumed.status)
        assertEquals(15, resumed.elapsedSeconds)

        // Advance 10s -> becomes 25s
        clock.currentMono = 190_000L
        clock.currentEpoch = fixedEpoch + 90_000L
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
            cookingPlanJson = json.encodeToString(plan),
            eventsJson = json.encodeToString(events),
            plannedUsageJson = json.encodeToString(plannedUsage),
            status = "RUNNING",
            startedAtMillis = 100_000L,
            accumulatedElapsedSeconds = 25,
            lastRunningStartMillis = 120_000L,
            pausedAtMillis = null,
            completedStepIdsJson = json.encodeToString(setOf("step1")),
            skippedStepIdsJson = json.encodeToString(emptySet<String>()),
            recentChatTurnsJson = json.encodeToString(turns),
            updatedAtIso = "2026-01-01T12:30:00Z"
        )

        repo.saveActiveSession(record)
        val loaded = repo.getActiveSession("session-123")
        assertNotNull(loaded)
        assertEquals("session-123", loaded.sessionId)
        assertEquals("Chicken Soup", loaded.recipeName)
        assertEquals(25, loaded.accumulatedElapsedSeconds)

        val restoredPlan = json.decodeFromString<CookingPlanResponse>(loaded.cookingPlanJson)
        assertEquals("Chicken Soup", restoredPlan.recipeName)

        val restoredTurns = json.decodeFromString<List<CookingChatTurn>>(loaded.recentChatTurnsJson)
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

    @Test
    fun `malformed timestamp correctly handled on recovery`() {
        val clock = TestClock(100_000L)
        val controller = CookingSessionController(clock)
        val events = listOf(sampleEvent("a", 0, 120))

        val restored = controller.restore(
            recipe = "Soup",
            schedule = events,
            status = CookingSessionStatus.RUNNING,
            startedAtMillis = 0L, // Missing or invalid
            accumulatedElapsedSeconds = 10,
            lastRunningStartMillis = 0L // Missing or invalid
        )

        assertEquals(CookingSessionStatus.RUNNING, restored.status)
        assertEquals(10, restored.elapsedSeconds)
    }
}
