package com.agentickitchen.android.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseProviderCopyTest {

    @Test
    fun `english firebase explanatory copy establishes managed boundary without automatic fallback`() {
        val copy = firebaseProviderExplanation(isTr = false)

        assertTrue("Must indicate managed access", copy.contains("Managed Gemini access"))
        assertTrue("Must state no personal key is needed", copy.contains("No personal API key is required"))
        assertTrue("Must state requests fail explicitly", copy.contains("If Firebase is unavailable, requests fail explicitly"))
        assertTrue("Must state offline must be chosen manually", copy.contains("choose Offline manually"))

        assertFalse("Must not claim automatic fallback", copy.contains("falls back"))
        assertFalse("Must not claim fallback", copy.contains("fallback", ignoreCase = true))
        assertFalse("Must not claim automatic switch", copy.contains("automatically", ignoreCase = true))
    }

    @Test
    fun `turkish firebase explanatory copy establishes managed boundary without automatic fallback`() {
        val copy = firebaseProviderExplanation(isTr = true)

        assertTrue("Must indicate managed access in Turkish", copy.contains("Yönetilen Gemini erişimi"))
        assertTrue("Must state no personal key is needed in Turkish", copy.contains("Kişisel API anahtarı gerekmez"))
        assertTrue("Must state requests fail explicitly in Turkish", copy.contains("Firebase kullanılamıyorsa istek açıkça başarısız olur"))
        assertTrue("Must state offline must be chosen manually in Turkish", copy.contains("kendin seç"))

        assertFalse("Must not claim automatic fallback in Turkish", copy.contains("düşer"))
        assertFalse("Must not claim fallback in Turkish", copy.contains("çevrimdışı moda düşer"))
        assertFalse("Must not claim automatic switch in Turkish", copy.contains("otomatik", ignoreCase = true))
    }
}
