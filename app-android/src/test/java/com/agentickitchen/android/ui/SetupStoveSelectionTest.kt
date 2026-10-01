package com.agentickitchen.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupStoveSelectionTest {
    @Test
    fun gasAndElectricAreMutuallyExclusive() {
        assertEquals(setOf("gas"), toggledSetupEquipment(setOf("elec"), "gas"))
        assertEquals(setOf("elec"), toggledSetupEquipment(setOf("gas"), "elec"))
    }

    @Test
    fun gasUsesFlameGuidanceAndElectricUsesNumericScale() {
        assertEquals(SetupStoveGuidance.GasFlame, setupStoveGuidance(setOf("gas")))
        assertEquals(SetupStoveGuidance.ElectricScale, setupStoveGuidance(setOf("elec")))
        assertEquals(SetupStoveGuidance.None, setupStoveGuidance(emptySet()))
    }
}
