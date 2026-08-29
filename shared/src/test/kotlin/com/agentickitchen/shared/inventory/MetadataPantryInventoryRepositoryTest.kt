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

    private fun adjustment() = InventoryAdjustmentRecord(
        id = "adjustment-1",
        itemId = "item-1",
        amount = 500.0,
        mode = AdjustmentMode.DELTA,
        reason = AdjustmentReason.MANUAL_ADD,
        source = "test",
        timestamp = "2026-08-30T00:00:00Z"
    )
}
