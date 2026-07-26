package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimpleIngredientAgentTest {
    private val agent = SimpleIngredientAgent()

    @Test
    fun `reject candidate with allergen`() {
        val original = Ingredient(id = "a", name = "X", category = "veg", allergen = false)
        val candidate = Ingredient(id = "b", name = "Y", category = "nut", allergen = true)
        val d = agent.evaluateSubstitution(original, candidate)
        assertEquals(Decision.REJECT, d.decision)
    }

    @Test
    fun `accept similar flavor`() {
        val original = Ingredient(id = "o", name = "O", category = "veg", flavorVector = FlavorVector(umami = 0.5, salt = 0.02, sweet = 0.01, bitter = 0.0), cookedMassLossPct = 10.0)
        val candidate = Ingredient(id = "c", name = "C", category = "veg", flavorVector = FlavorVector(umami = 0.48, salt = 0.021, sweet = 0.011, bitter = 0.0), cookedMassLossPct = 12.0)
        val d = agent.evaluateSubstitution(original, candidate)
        assertEquals(Decision.ALTERNATIVE, d.decision)
    }
}
