package com.agentickitchen.shared.inventory

import java.text.Normalizer
import java.util.Locale

data class CatalogIngredient(
    val canonicalId: String,
    val displayNameTr: String,
    val displayNameEn: String,
    val aliases: Set<String>
)

object LocalIngredientResolver {
    private val catalog = listOf(
        CatalogIngredient("chicken_breast", "Tavuk Göğsü", "Chicken Breast", setOf("tavuk göğsü", "tavuk gogsu", "chicken breast")),
        CatalogIngredient("chicken", "Tavuk", "Chicken", setOf("tavuk", "chicken", "tavuketi", "chicken meat")),
        CatalogIngredient("milk", "Süt", "Milk", setOf("süt", "sut", "milk")),
        CatalogIngredient("rice", "Pirinç", "Rice", setOf("pirinç", "pirinc", "rice")),
        CatalogIngredient("tomato", "Domates", "Tomato", setOf("domates", "tomato", "tomatoes")),
        CatalogIngredient("egg", "Yumurta", "Egg", setOf("yumurta", "yumurtalar", "egg", "eggs")),
        CatalogIngredient("onion", "Soğan", "Onion", setOf("soğan", "sogan", "soğanlar", "onion", "onions")),
        CatalogIngredient("garlic", "Sarımsak", "Garlic", setOf("sarımsak", "sarimsak", "garlic")),
        CatalogIngredient("pasta", "Makarna", "Pasta", setOf("makarna", "pasta")),
        CatalogIngredient("butter", "Tereyağı", "Butter", setOf("tereyağı", "tereyagi", "butter")),
        CatalogIngredient("cheese", "Peynir", "Cheese", setOf("peynir", "cheese", "cheeses")),
        CatalogIngredient("olive_oil", "Zeytinyağı", "Olive Oil", setOf("zeytinyağı", "zeytinyagi", "olive oil")),
        CatalogIngredient("salt", "Tuz", "Salt", setOf("tuz", "salt")),
        CatalogIngredient("black_pepper", "Karabiber", "Black Pepper", setOf("karabiber", "black pepper", "pepper")),
        CatalogIngredient("flour", "Un", "Flour", setOf("un", "flour"))
    )

    private val aliasMap: Map<String, CatalogIngredient> = buildMap {
        catalog.forEach { entry ->
            put(entry.canonicalId, entry)
            entry.aliases.forEach { alias ->
                put(alias.normalized(), entry)
            }
        }
    }

    fun resolveCanonicalId(rawNameOrId: String?): String? {
        if (rawNameOrId.isNullOrBlank()) return null
        val normalized = rawNameOrId.normalized()
        return aliasMap[normalized]?.canonicalId
    }

    fun findCatalogItem(rawNameOrId: String?): CatalogIngredient? {
        if (rawNameOrId.isNullOrBlank()) return null
        val normalized = rawNameOrId.normalized()
        return aliasMap[normalized]
    }

    fun isKnownCanonicalId(canonicalId: String?): Boolean {
        if (canonicalId.isNullOrBlank()) return false
        return catalog.any { it.canonicalId == canonicalId }
    }

    fun matches(
        firstName: String,
        firstCanonicalId: String?,
        secondName: String,
        secondCanonicalId: String?
    ): Boolean {
        val canonical1 = firstCanonicalId ?: resolveCanonicalId(firstName)
        val canonical2 = secondCanonicalId ?: resolveCanonicalId(secondName)

        if (canonical1 != null && canonical2 != null) {
            return canonical1 == canonical2
        }

        return firstName.normalized() == secondName.normalized()
    }

    fun localizeIngredientName(originalName: String, canonicalId: String?, isTr: Boolean): String {
        val resolvedId = canonicalId ?: resolveCanonicalId(originalName)
        val entry = resolvedId?.let { findCatalogItem(it) }
        return if (entry != null) {
            if (isTr) entry.displayNameTr else entry.displayNameEn
        } else {
            originalName
        }
    }

    fun localizeUnit(unit: String, isTr: Boolean): String = when (unit.trim().lowercase()) {
        "count", "adet", "piece", "pieces", "pcs" -> if (isTr) "adet" else "pieces"
        "package", "paket", "pack", "packs" -> if (isTr) "paket" else "packages"
        "bunch", "demet", "bunches" -> if (isTr) "demet" else "bunches"
        else -> unit.trim()
    }

    fun String.normalized(): String = Normalizer.normalize(
        trim().lowercase(Locale.ROOT).replace('ı', 'i'),
        Normalizer.Form.NFD
    ).replace(Regex("""\p{Mn}+"""), "")
}
