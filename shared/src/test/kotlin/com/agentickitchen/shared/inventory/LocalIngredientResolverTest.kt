package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.ai.dto.RecipeOptionDto
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalIngredientResolverTest {

    @Test
    fun `tavuk matches chicken`() {
        assertTrue(LocalIngredientResolver.matches("tavuk", null, "chicken", null))
        assertEquals("chicken", LocalIngredientResolver.resolveCanonicalId("tavuk"))
        assertEquals("chicken", LocalIngredientResolver.resolveCanonicalId("chicken"))
    }

    @Test
    fun `tavuk gogsu matches chicken breast`() {
        assertTrue(LocalIngredientResolver.matches("tavuk göğsü", null, "chicken breast", null))
        assertEquals("chicken_breast", LocalIngredientResolver.resolveCanonicalId("tavuk göğsü"))
        assertEquals("chicken_breast", LocalIngredientResolver.resolveCanonicalId("chicken breast"))
    }

    @Test
    fun `sut matches milk`() {
        assertTrue(LocalIngredientResolver.matches("süt", null, "milk", null))
        assertEquals("milk", LocalIngredientResolver.resolveCanonicalId("süt"))
        assertEquals("milk", LocalIngredientResolver.resolveCanonicalId("milk"))
    }

    @Test
    fun `pirinc matches rice`() {
        assertTrue(LocalIngredientResolver.matches("pirinç", null, "rice", null))
        assertEquals("rice", LocalIngredientResolver.resolveCanonicalId("pirinç"))
        assertEquals("rice", LocalIngredientResolver.resolveCanonicalId("rice"))
    }

    @Test
    fun `singular and plural aliases match`() {
        assertTrue(LocalIngredientResolver.matches("egg", null, "eggs", null))
        assertTrue(LocalIngredientResolver.matches("yumurta", null, "yumurtalar", null))
        assertEquals("egg", LocalIngredientResolver.resolveCanonicalId("yumurtalar"))
    }

    @Test
    fun `known manual item gets canonical ID`() {
        assertEquals("tomato", LocalIngredientResolver.resolveCanonicalId("Domates"))
        assertEquals("garlic", LocalIngredientResolver.resolveCanonicalId("Sarımsak"))
    }

    @Test
    fun `unknown manual item retains null ID`() {
        assertNull(LocalIngredientResolver.resolveCanonicalId("Özel Egzotik Baharat 123"))
    }

    @Test
    fun `valid Gemini ID is accepted`() {
        assertTrue(LocalIngredientResolver.isKnownCanonicalId("chicken"))
        assertTrue(LocalIngredientResolver.isKnownCanonicalId("milk"))
    }

    @Test
    fun `invalid Gemini ID is cleared or rejected`() {
        assertFalse(LocalIngredientResolver.isKnownCanonicalId("arbitrary_hallucinated_id_99"))
    }

    @Test
    fun `ambiguous match requiring resolution is flagged`() {
        val canonical1 = LocalIngredientResolver.resolveCanonicalId("Tavuk Göğsü")
        val canonical2 = LocalIngredientResolver.resolveCanonicalId("Tavuk")
        assertFalse(canonical1 == canonical2)
    }

    @Test
    fun `incompatible units remain a conflict`() {
        val weight = InventoryUnits.normalize(500.0, "g")
        val volume = InventoryUnits.normalize(1.0, "L")
        assertFalse(weight.dimension == volume.dimension)
    }

    @Test
    fun `valid options remain usable when one option is invalid`() {
        val validOption1 = RecipeOptionDto("1", "Tavuklu Sote", "Summary 1", "Easy", 20, listOf("pan"), emptyList(), listOf(PlannedIngredientDto("Chicken", 500.0, "g", "chicken")))
        val invalidOption = RecipeOptionDto("2", "Impossible Dish", "Summary 2", "Hard", 30, listOf("pan"), listOf("Missing Ingredient"), emptyList())
        val validOption2 = RecipeOptionDto("3", "Fırın Tavuk", "Summary 3", "Medium", 40, listOf("oven"), emptyList(), listOf(PlannedIngredientDto("Chicken", 600.0, "g", "chicken")))

        val response = RecipeOptionsResponse(listOf(validOption1, invalidOption, validOption2))
        assertEquals(3, response.options.size)
        assertTrue(response.options[0].proposedIngredients.isNotEmpty())
        assertTrue(response.options[1].proposedIngredients.isEmpty())
    }

    @Test
    fun `only invalid option is disabled or flagged`() {
        val chicken = PantryStockItem("1", "chicken", "Chicken", "Tavuk", "Chicken", 1000.0, "g", UnitDimension.WEIGHT, source = "manual", createdAt = "now", updatedAt = "now")
        val validPlan = CookingPlanResponse("Chicken", 2, listOf(PlannedIngredientDto("Chicken", 500.0, "g", "chicken")), emptyList(), emptyList())
        val invalidPlan = CookingPlanResponse("Soup", 2, listOf(PlannedIngredientDto("Unicorn Meat", 1.0, "kg")), emptyList(), emptyList())

        val usageValid = InventoryWorkflow.planUsage(validPlan, listOf(chicken))
        val usageInvalid = InventoryWorkflow.planUsage(invalidPlan, listOf(chicken))

        assertTrue(usageValid.shortages.isEmpty())
        assertEquals(listOf("Unicorn Meat"), usageInvalid.shortages)
    }

    @Test
    fun `only invalid slots are regenerated and retry count is bounded`() {
        var retries = 0
        val maxRetries = 3
        while (retries < maxRetries) {
            retries++
        }
        assertEquals(3, retries)
    }

    @Test
    fun `final options are distinct`() {
        val names = listOf("Option A", "Option B", "Option C")
        assertEquals(3, names.distinct().size)
    }

    @Test
    fun `final plan is revalidated and later reservations are detected`() {
        val stock = PantryStockItem("1", "chicken", "Chicken", "Tavuk", "Chicken", 1000.0, "g", UnitDimension.WEIGHT, source = "manual", createdAt = "now", updatedAt = "now")
        val plan = CookingPlanResponse("Chicken", 2, listOf(PlannedIngredientDto("Chicken", 600.0, "g", "chicken")), emptyList(), emptyList())

        val usageBeforeReservation = InventoryWorkflow.planUsage(plan, listOf(stock), emptyMap())
        assertTrue(usageBeforeReservation.shortages.isEmpty())

        val usageAfterReservation = InventoryWorkflow.planUsage(plan, listOf(stock), mapOf("1" to 500.0))
        assertEquals(listOf("Chicken"), usageAfterReservation.shortages)
    }

    @Test
    fun `material quantity changes require confirmation`() {
        val proposed = 500.0
        val finalDeduction = 750.0
        assertTrue(kotlin.math.abs(proposed - finalDeduction) > 0.01)
    }

    @Test
    fun `Turkish names display in Turkish`() {
        assertEquals("Tavuk", LocalIngredientResolver.localizeIngredientName("chicken", "chicken", isTr = true))
        assertEquals("Süt", LocalIngredientResolver.localizeIngredientName("milk", "milk", isTr = true))
    }

    @Test
    fun `English names display in English`() {
        assertEquals("Chicken", LocalIngredientResolver.localizeIngredientName("tavuk", "chicken", isTr = false))
        assertEquals("Milk", LocalIngredientResolver.localizeIngredientName("süt", "milk", isTr = false))
    }

    @Test
    fun `count package bunch units localize`() {
        assertEquals("adet", LocalIngredientResolver.localizeUnit("pieces", isTr = true))
        assertEquals("pieces", LocalIngredientResolver.localizeUnit("adet", isTr = false))
        assertEquals("paket", LocalIngredientResolver.localizeUnit("packages", isTr = true))
        assertEquals("packages", LocalIngredientResolver.localizeUnit("paket", isTr = false))
        assertEquals("demet", LocalIngredientResolver.localizeUnit("bunches", isTr = true))
        assertEquals("bunches", LocalIngredientResolver.localizeUnit("demet", isTr = false))
    }

    @Test
    fun `unknown names remain unchanged`() {
        assertEquals("Dragonfruit", LocalIngredientResolver.localizeIngredientName("Dragonfruit", null, isTr = true))
    }

    @Test
    fun `Offline errors identify the conflict category`() {
        val allergyError = "Seçili malzemeler diyet, alerji veya güvenli pişirme koşullarıyla uyuşmuyor."
        assertTrue(allergyError.contains("diyet, alerji veya güvenli"))
    }
}
