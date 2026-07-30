package com.agentickitchen.android

import java.text.Normalizer
import java.util.Locale

data class AllergyDefinition(
    val id: String,
    val labelTr: String,
    val labelEn: String,
    val aliases: Set<String> = emptySet()
)

object AllergyCatalog {
    val definitions = listOf(
        AllergyDefinition("milk", "Süt ve süt ürünleri", "Milk and dairy", setOf("dairy", "süt", "sut")),
        AllergyDefinition("egg", "Yumurta", "Egg", setOf("eggs", "yumurta")),
        AllergyDefinition("gluten", "Glüten ve buğday", "Gluten and wheat", setOf("wheat", "buğday", "bugday")),
        AllergyDefinition("peanut", "Yer fıstığı", "Peanut", setOf("peanuts", "yer fıstığı", "yer fistigi")),
        AllergyDefinition("tree_nuts", "Sert kabuklu yemişler", "Tree nuts", setOf("nuts", "tree nuts", "kuruyemiş", "kuruyemis")),
        AllergyDefinition("soy", "Soya", "Soy", setOf("soya")),
        AllergyDefinition("fish", "Balık", "Fish", setOf("balık", "balik")),
        AllergyDefinition("shellfish", "Kabuklu deniz ürünleri", "Shellfish", setOf("seafood", "kabuklu deniz ürünü", "kabuklu deniz urunu")),
        AllergyDefinition("sesame", "Susam", "Sesame", setOf("susam"))
    )

    fun normalize(values: Set<String>): Set<String> =
        values.mapNotNullTo(linkedSetOf(), ::canonicalId)

    fun canonicalId(value: String): String? {
        val normalized = normalizeText(value.removePrefix("custom:"))
        if (normalized.isBlank()) return null
        definitions.firstOrNull { definition ->
            normalized == normalizeText(definition.id) ||
                definition.aliases.any { normalized == normalizeText(it) }
        }?.let { return it.id }
        return "custom:$normalized"
    }

    fun normalizeCustom(value: String): String? = canonicalId(value)

    fun label(id: String, isTurkish: Boolean): String {
        val canonical = canonicalId(id) ?: return id
        val definition = definitions.firstOrNull { it.id == canonical }
        if (definition != null) return if (isTurkish) definition.labelTr else definition.labelEn
        return canonical.removePrefix("custom:").replace('_', ' ')
    }

    private fun normalizeText(value: String): String = Normalizer.normalize(
        value.trim().lowercase(Locale.ROOT).replace('ı', 'i'),
        Normalizer.Form.NFD
    ).replace(Regex("""\p{Mn}+"""), "")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
        .replace(Regex("""\s+"""), "_")
}
