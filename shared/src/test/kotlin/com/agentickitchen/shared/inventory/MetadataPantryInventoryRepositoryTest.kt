package com.agentickitchen.shared.inventory

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.db.AppDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MetadataPantryInventoryRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: MetadataPantryInventoryRepository

    @BeforeEach
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        repository = MetadataPantryInventoryRepository(
            SqlDelightPantryInventoryRepository(database),
            database
        )
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun locationAndExpiryRoundTripWithInventoryItem() {
        repository.upsert(
            item = item(
                location = PantryLocation.FRIDGE,
                bestBefore = "2026-09-02",
                useBy = "2026-09-01"
            ),
            adjustment = adjustment()
        )

        val restored = repository.getAll().single()
        assertEquals(PantryLocation.FRIDGE, restored.location)
        assertNull(restored.customLocationLabel)
        assertEquals("2026-09-02", restored.bestBefore)
        assertEquals("2026-09-01", restored.useBy)
    }

    @Test
    fun customLocationRoundTrips() {
        repository.upsert(
            item = item(
                location = PantryLocation.OTHER,
                customLocationLabel = "Utility cupboard"
            ),
            adjustment = adjustment()
        )

        val restored = repository.getAll().single()
        assertEquals(PantryLocation.OTHER, restored.location)
        assertEquals("Utility cupboard", restored.customLocationLabel)
    }

    @Test
    fun missingMetadataDefaultsToPantry() {
        SqlDelightPantryInventoryRepository(database).upsert(item(), adjustment())

        val restored = repository.getAll().single()
        assertEquals(PantryLocation.PANTRY, restored.location)
        assertNull(restored.bestBefore)
        assertNull(restored.useBy)
    }

    @Test
    fun migrationFromVersionThreePreservesExistingPantryRows() {
        val legacyDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            legacyDriver.execute(
                null,
                """
                CREATE TABLE PantryItem (
                    id TEXT NOT NULL PRIMARY KEY,
                    canonicalIngredientId TEXT,
                    originalName TEXT NOT NULL,
                    displayNameTr TEXT,
                    displayNameEn TEXT,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    unitDimension TEXT NOT NULL,
                    packageLabel TEXT,
                    isEstimated INTEGER NOT NULL,
                    confidence REAL,
                    source TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent(),
                0
            ).value
            legacyDriver.execute(
                null,
                """
                INSERT INTO PantryItem(
                    id, canonicalIngredientId, originalName, displayNameTr, displayNameEn,
                    quantity, unit, unitDimension, packageLabel, isEstimated, confidence,
                    source, createdAt, updatedAt
                ) VALUES (
                    'legacy-item', NULL, 'Milk', NULL, NULL,
                    500.0, 'ml', 'VOLUME', NULL, 0, NULL,
                    'manual', '2026-08-29T00:00:00Z', '2026-08-29T00:00:00Z'
                )
                """.trimIndent(),
                0
            ).value

            val currentVersion = AppDatabase.Schema.version
            assertEquals(6L, currentVersion)
            AppDatabase.Schema.migrate(legacyDriver, 3L, currentVersion)

            val migratedDatabase = AppDatabase(legacyDriver)
            val migratedRepository = MetadataPantryInventoryRepository(
                SqlDelightPantryInventoryRepository(migratedDatabase),
                migratedDatabase
            )
            assertEquals(emptyList<ShoppingListItem>(), SqlDelightShoppingListRepository(migratedDatabase).getAll())
            assertEquals(0, migratedDatabase.appDatabaseQueries.selectAllSavedRecipes().executeAsList().size)
            val restored = migratedRepository.getAll().single()

            assertEquals("legacy-item", restored.id)
            assertEquals("Milk", restored.originalName)
            assertEquals(500.0, restored.quantity)
            assertEquals(PantryLocation.PANTRY, restored.location)
            assertNull(restored.bestBefore)
            assertNull(restored.useBy)

            assertEquals(
                true,
                migratedRepository.updateMetadata(
                    restored.copy(location = PantryLocation.FRIDGE, useBy = "2026-09-01")
                )
            )
            val enriched = migratedRepository.getAll().single()
            assertEquals(PantryLocation.FRIDGE, enriched.location)
            assertEquals("2026-09-01", enriched.useBy)
        } finally {
            legacyDriver.close()
        }
    }

    @Test
    fun regularQuantityUpsertPreservesExistingMetadata() {
        repository.upsert(
            item(PantryLocation.FRIDGE, bestBefore = "2026-09-02"),
            adjustment()
        )
        repository.updateMetadata(
            item(PantryLocation.FREEZER, useBy = "2026-09-01")
        )

        val quantityOnlyUpdate = item().copy(quantity = 350.0, updatedAt = "2026-08-31T00:00:00Z")
        repository.upsert(
            quantityOnlyUpdate,
            adjustment(id = "adjustment-2", amount = 350.0, reason = AdjustmentReason.RECOUNT)
        )

        val restored = repository.getAll().single()
        assertEquals(350.0, restored.quantity)
        assertEquals(PantryLocation.FREEZER, restored.location)
        assertNull(restored.bestBefore)
        assertEquals("2026-09-01", restored.useBy)
    }

    @Test
    fun explicitMetadataUpdateCanMoveItemBackToPantryAndClearDates() {
        repository.upsert(
            item(PantryLocation.FRIDGE, useBy = "2026-09-01"),
            adjustment()
        )

        val changed = repository.updateMetadata(item(PantryLocation.PANTRY))

        assertEquals(true, changed)
        val restored = repository.getAll().single()
        assertEquals(PantryLocation.PANTRY, restored.location)
        assertNull(restored.bestBefore)
        assertNull(restored.useBy)
    }

    private fun item(
        location: PantryLocation = PantryLocation.PANTRY,
        customLocationLabel: String? = null,
        bestBefore: String? = null,
        useBy: String? = null
    ) = PantryStockItem(
        id = "item-1",
        originalName = "Milk",
        quantity = 500.0,
        unit = "ml",
        unitDimension = UnitDimension.VOLUME,
        source = "test",
        createdAt = "2026-08-30T00:00:00Z",
        updatedAt = "2026-08-30T00:00:00Z",
        location = location,
        customLocationLabel = customLocationLabel,
        bestBefore = bestBefore,
        useBy = useBy
    )

    private fun adjustment(
        id: String = "adjustment-1",
        amount: Double = 500.0,
        reason: AdjustmentReason = AdjustmentReason.MANUAL_ADD
    ) = InventoryAdjustmentRecord(
        id = id,
        itemId = "item-1",
        amount = amount,
        mode = AdjustmentMode.DELTA,
        reason = reason,
        source = "test",
        timestamp = "2026-08-30T00:00:00Z"
    )
}
