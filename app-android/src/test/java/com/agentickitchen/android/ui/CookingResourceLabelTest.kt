package com.agentickitchen.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CookingResourceLabelTest {
    @Test
    fun translatesKnownCookingResourcesWithoutExposingInternalIds() {
        val resources = mapOf(
            "counter" to ("TEZGAH" to "COUNTER"),
            "stove" to ("OCAK" to "STOVE"),
            "oven" to ("FIRIN" to "OVEN"),
            "airfryer" to ("HAVA FRİTÖZÜ" to "AIR FRYER"),
            "pan" to ("TAVA" to "PAN"),
            "pot" to ("TENCERE" to "POT")
        )

        resources.forEach { (resource, labels) ->
            assertEquals(labels.first, cookingResourceLabel(resource, isTurkish = true))
            assertEquals(labels.second, cookingResourceLabel(resource, isTurkish = false))
        }
        assertEquals("MUTFAK ALANI", cookingResourceLabel("internal_resource_id", isTurkish = true))
        assertEquals("KITCHEN AREA", cookingResourceLabel("internal_resource_id", isTurkish = false))
    }
}
