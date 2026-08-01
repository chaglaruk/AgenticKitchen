package com.agentickitchen.shared.inventory

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.db.AppDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlDelightPantryInventoryRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: SqlDelightPantryInventoryRepository

    @BeforeEach
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        repository = SqlDelightPantryInventoryRepository(database)
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun savingANewerActiveSessionReplacesThePreviousCanonicalSession() {
        repository.saveActiveSession(session("older", "2026-08-01T10:00:00Z"))
        repository.saveActiveSession(session("newer", "2026-08-01T11:00:00Z"))

        assertEquals(listOf("newer"), repository.getAllActiveSessions().map { it.sessionId })
        assertNull(repository.getActiveSession("older"))
    }

    @Test
    fun readingLegacyMultipleRowsKeepsOnlyTheNewestSession() {
        insertDirectly(session("older", "2026-08-01T10:00:00Z"))
        insertDirectly(session("newer", "2026-08-01T11:00:00Z"))

        assertEquals(listOf("newer"), repository.getAllActiveSessions().map { it.sessionId })
        assertNull(repository.getActiveSession("older"))
        assertEquals("newer", repository.getActiveSession("newer")?.sessionId)
    }

    private fun insertDirectly(session: ActiveCookingSessionRecord) {
        database.appDatabaseQueries.upsertActiveCookingSession(
            sessionId = session.sessionId,
            recipeOptionId = session.recipeOptionId,
            recipeName = session.recipeName,
            recipeType = session.recipeType,
            description = session.description,
            sourceLabel = session.sourceLabel,
            servings = session.servings.toLong(),
            resolvedReadyTimeIso = session.resolvedReadyTimeIso,
            cookingPlanJson = session.cookingPlanJson,
            eventsJson = session.eventsJson,
            plannedUsageJson = session.plannedUsageJson,
            status = session.status,
            startedAtMillis = session.startedAtMillis,
            accumulatedElapsedSeconds = session.accumulatedElapsedSeconds,
            lastRunningStartMillis = session.lastRunningStartMillis,
            pausedAtMillis = session.pausedAtMillis,
            completedStepIds = session.completedStepIdsJson,
            skippedStepIds = session.skippedStepIdsJson,
            recentChatTurnsJson = session.recentChatTurnsJson,
            updatedAtIso = session.updatedAtIso
        )
    }

    private fun session(id: String, updatedAt: String) = ActiveCookingSessionRecord(
        sessionId = id,
        recipeOptionId = "option-$id",
        recipeName = "Recipe $id",
        recipeType = "test",
        description = "Test recipe",
        sourceLabel = null,
        servings = 2,
        resolvedReadyTimeIso = "2026-08-01T12:00:00Z",
        cookingPlanJson = "{}",
        eventsJson = "[]",
        plannedUsageJson = "[]",
        status = "READY",
        startedAtMillis = 0L,
        accumulatedElapsedSeconds = 0L,
        lastRunningStartMillis = null,
        pausedAtMillis = null,
        completedStepIdsJson = "[]",
        skippedStepIdsJson = "[]",
        recentChatTurnsJson = "[]",
        updatedAtIso = updatedAt
    )
}
