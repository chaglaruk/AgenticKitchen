package com.agentickitchen.shared.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HistoryRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var repository: HistoryRepository

    @BeforeEach
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = HistoryRepository(AppDatabase(driver))
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `status transition updates the already exposed history list`() {
        repository.insertRecipe("session", "Soup", "tomato", "2026-08-01T12:00:00Z", "started")
        val exposed = repository.getAllHistory()

        repository.updateStatus("session", "completed")

        assertEquals("completed", exposed.single().status)
        assertEquals("completed", repository.getAllHistory().single().status)
    }

    @Test
    fun `delete and clear update the exposed history list`() {
        repository.insertRecipe("one", "One", "a", "2026-08-01T12:00:00Z", "started")
        repository.insertRecipe("two", "Two", "b", "2026-08-01T13:00:00Z", "started")
        val exposed = repository.getAllHistory()

        repository.deleteRecipe("one")
        assertEquals(listOf("two"), exposed.map { it.id })

        repository.clearHistory()
        assertEquals(emptyList<String>(), exposed.map { it.id })
    }
}
