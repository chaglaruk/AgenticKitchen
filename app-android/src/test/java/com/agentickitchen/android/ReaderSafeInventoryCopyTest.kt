package com.agentickitchen.android

import com.agentickitchen.android.ai.ProviderFailure
import com.agentickitchen.android.ai.ProviderFailureCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The INVENTORY constraint conflict needs its own reader-safe copy: the plan did not match the
 * selected recipe's ingredient and pantry plan, which is different from diet/allergy conflicts.
 */
class ReaderSafeInventoryCopyTest {

    @Test
    fun `inventory constraint conflict uses the inventory specific copy`() {
        val message = readerSafeAiError(
            ProviderFailure("INVENTORY", ProviderFailureCategory.CONSTRAINT_CONFLICT)
        )
        val expected = setOf(
            "Pişirme planı seçtiğin tarifin malzeme ve stok planıyla uyuşmadı.",
            "The cooking plan did not match the selected recipe's ingredient and pantry plan."
        )
        assert(message in expected) { "unexpected copy: $message" }
    }

    @Test
    fun `other constraint conflicts keep the diet allergy safety copy`() {
        val message = readerSafeAiError(
            ProviderFailure("DIET_GUARD", ProviderFailureCategory.CONSTRAINT_CONFLICT)
        )
        val expected = setOf(
            "Seçili malzemeler diyet, alerji veya güvenli pişirme koşullarıyla uyuşmuyor.",
            "The selected ingredients conflict with the diet, allergy, or safe cooking setup."
        )
        assert(message in expected) { "unexpected copy: $message" }
    }

    @Test
    fun `inventory failures never surface the generic constraint copy`() {
        val message = readerSafeAiError(
            ProviderFailure("INVENTORY", ProviderFailureCategory.CONSTRAINT_CONFLICT)
        )
        val generic = setOf(
            "Seçili malzemeler diyet, alerji veya güvenli pişirme koşullarıyla uyuşmuyor.",
            "The selected ingredients conflict with the diet, allergy, or safe cooking setup."
        )
        assert(message !in generic) { "inventory failure must not reuse the generic copy: $message" }
    }
}
