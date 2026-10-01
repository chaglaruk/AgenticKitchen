package com.agentickitchen.shared.recipes

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import com.agentickitchen.shared.db.AppDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlDelightSavedRecipeRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: SqlDelightSavedRecipeRepository

    @BeforeEach
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        repository = SqlDelightSavedRecipeRepository(database)
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun structuredRecipeRoundTripsWithoutLosingSourceOrUncertainty() {
        val saved = savedRecipe()

        repository.upsert(saved)

        assertEquals(saved, repository.getById(saved.id))
        assertEquals(listOf(saved), repository.getAll())
    }

    @Test
    fun recordCookedUpdatesPreferenceSignalsWithoutChangingRecipeContent() {
        val saved = savedRecipe()
        repository.upsert(saved)

        repository.recordCooked(saved.id, "2026-09-01T10:00:00Z")
        repository.recordCooked(saved.id, "2026-09-02T11:30:00Z")

        val loaded = repository.getById(saved.id)
        assertEquals(saved.recipe, loaded?.recipe)
        assertEquals(2, loaded?.cookCount)
        assertEquals("2026-09-02T11:30:00Z", loaded?.lastCookedAt)
        assertEquals("2026-09-02T11:30:00Z", loaded?.updatedAt)
    }

    @Test
    fun deleteRemovesSavedRecipe() {
        val saved = savedRecipe()
        repository.upsert(saved)

        repository.delete(saved.id)

        assertNull(repository.getById(saved.id))
        assertEquals(emptyList<SavedRecipe>(), repository.getAll())
    }

    @Test
    fun malformedPersistedRecipeIsSkippedInsteadOfCrashingTheLibrary() {
        database.appDatabaseQueries.upsertSavedRecipe(
            id = "broken",
            name = "Broken",
            recipeJson = "{not-json",
            sourceKind = SavedRecipeSource.IMPORTED.name,
            createdAt = "2026-09-01T08:00:00Z",
            updatedAt = "2026-09-01T08:00:00Z",
            lastCookedAt = null,
            cookCount = 0L
        )

        assertEquals(emptyList<SavedRecipe>(), repository.getAll())
        assertNull(repository.getById("broken"))
    }

    private fun savedRecipe() = SavedRecipe(
        id = "recipe-1",
        recipe = ImportedRecipe(
            name = "Lemon Rice",
            servings = 2,
            ingredients = listOf(
                ImportedRecipeIngredient(
                    displayName = "Rice",
                    quantity = 200.0,
                    unit = "g",
                    canonicalIngredientId = "rice",
                    rawText = "200 g rice",
                    confidence = 0.98
                ),
                ImportedRecipeIngredient(
                    displayName = "Lemon",
                    quantity = null,
                    unit = null,
                    canonicalIngredientId = "lemon",
                    rawText = "lemon to taste",
                    confidence = 0.72,
                    uncertaintyReason = "Source does not specify an amount"
                )
            ),
            instructions = listOf("Cook the rice.", "Finish with lemon."),
            sourceLabel = "Example recipe",
            sourceUrl = "https://example.com/lemon-rice"
        ),
        source = SavedRecipeSource.IMPORTED,
        createdAt = "2026-09-01T08:00:00Z",
        updatedAt = "2026-09-01T08:00:00Z"
    )
}
