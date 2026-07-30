package com.agentickitchen.shared.validator

import java.text.Normalizer
import java.util.Locale

enum class IngredientSafetyGroup {
    MEAT,
    FISH,
    SHELLFISH,
    EGG,
    DAIRY,
    HONEY,
    GLUTEN,
    TREE_NUTS,
    PEANUT,
    SOY,
    SESAME
}

object IngredientSafety {
    fun groups(name: String): Set<IngredientSafetyGroup> {
        val value = normalize(name)
        return groupTerms.mapNotNullTo(linkedSetOf()) { (group, terms) ->
            group.takeIf { terms.any { term -> value.containsTerm(term) } }
        }
    }

    fun conflictsWithDiet(name: String, dietType: String): Boolean = when (dietType.lowercase(Locale.ROOT)) {
        "vegetarian" -> groups(name).any { it in vegetarianConflicts }
        "vegan" -> groups(name).any { it in veganConflicts }
        else -> false
    }

    fun conflictsWithAllergen(name: String, allergen: String): Boolean {
        val ingredient = normalize(name)
        val normalizedAllergen = normalize(allergen)
        if (normalizedAllergen.isBlank()) return false
        if (ingredient.containsTerm(normalizedAllergen)) return true
        val allergenGroups = allergenAliases.entries
            .filter { (_, aliases) -> aliases.any { normalizedAllergen.containsTerm(it) } }
            .mapTo(linkedSetOf(), Map.Entry<IngredientSafetyGroup, Set<String>>::key)
        return groups(name).any(allergenGroups::contains)
    }

    private fun normalize(value: String): String = Normalizer.normalize(
        value.lowercase(Locale.ROOT).replace('ı', 'i'),
        Normalizer.Form.NFD
    ).replace(Regex("""\p{Mn}+"""), "")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()

    private fun String.containsTerm(term: String): Boolean =
        this == term || " $this ".contains(" $term ")

    private val vegetarianConflicts = setOf(
        IngredientSafetyGroup.MEAT,
        IngredientSafetyGroup.FISH,
        IngredientSafetyGroup.SHELLFISH
    )
    private val veganConflicts = vegetarianConflicts + setOf(
        IngredientSafetyGroup.EGG,
        IngredientSafetyGroup.DAIRY,
        IngredientSafetyGroup.HONEY
    )

    private val groupTerms = mapOf(
        IngredientSafetyGroup.MEAT to setOf(
            "meat", "et", "chicken", "chicken breast", "chicken thighs", "chicken wings", "tavuk", "tavuk gogsu",
            "tavuk but", "tavuk kanadi", "turkey", "hindi", "ground beef", "kiyma", "beef", "dana eti",
            "lamb", "kuzu eti", "steak", "biftek", "meatballs", "kofte", "sausage", "sosis", "bacon",
            "pastirma", "liver", "ciger", "deli meat", "sarkuteri eti", "pork", "domuz"
        ),
        IngredientSafetyGroup.FISH to setOf(
            "fish", "balik", "salmon", "somon", "white fish", "beyaz balik", "tuna", "ton baligi",
            "anchovy", "hamsi", "sardine", "sardalya", "sea bass", "levrek", "sea bream", "cipura",
            "cod", "morina"
        ),
        IngredientSafetyGroup.SHELLFISH to setOf(
            "shellfish", "seafood", "deniz urunu", "shrimp", "prawn", "prawns", "karides", "mussel",
            "mussels", "midye", "squid", "kalamar", "octopus", "ahtapot", "crab", "yengec", "lobster"
        ),
        IngredientSafetyGroup.EGG to setOf("egg", "eggs", "yumurta"),
        IngredientSafetyGroup.DAIRY to setOf(
            "dairy", "sut urunu", "milk", "sut", "yoghurt", "yogurt", "yogurt", "greek yoghurt",
            "suzme yogurt", "cheese", "peynir", "beyaz peynir", "kasar peyniri", "mozzarella",
            "cheddar", "cedar peyniri", "parmesan", "soft cheese", "krem peynir", "double cream",
            "cream", "krema", "butter", "tereyagi", "sour cream", "eksi krema"
        ),
        IngredientSafetyGroup.HONEY to setOf("honey", "bal"),
        IngredientSafetyGroup.GLUTEN to setOf(
            "gluten", "wheat", "bugday", "flour", "un", "wholemeal flour", "tam bugday unu", "bread",
            "ekmek", "pita", "pide", "pasta", "makarna", "spaghetti", "spagetti", "noodles", "eriste",
            "breadcrumbs", "galeta unu", "bulgur", "couscous", "kuskus", "barley", "arpa"
        ),
        IngredientSafetyGroup.TREE_NUTS to setOf(
            "tree nuts", "nuts", "kuruyemis", "walnut", "walnuts", "ceviz", "hazelnut", "hazelnuts",
            "findik", "almond", "almonds", "badem", "pistachio", "pistachios", "antep fistigi",
            "cashew", "cashews", "kaju", "pine nuts", "cam fistigi"
        ),
        IngredientSafetyGroup.PEANUT to setOf("peanut", "peanuts", "yer fistigi"),
        IngredientSafetyGroup.SOY to setOf("soy", "soya", "soybeans", "soya fasulyesi", "tofu", "edamame"),
        IngredientSafetyGroup.SESAME to setOf("sesame", "sesame seeds", "tahini", "tahin", "susam", "susam tohumu")
    )

    private val allergenAliases = mapOf(
        IngredientSafetyGroup.MEAT to setOf("meat", "et"),
        IngredientSafetyGroup.FISH to setOf("fish", "balik"),
        IngredientSafetyGroup.SHELLFISH to setOf("shellfish", "seafood", "deniz urunu", "kabuklu deniz urunu"),
        IngredientSafetyGroup.EGG to setOf("egg", "yumurta"),
        IngredientSafetyGroup.DAIRY to setOf("milk", "dairy", "sut", "sut urunu"),
        IngredientSafetyGroup.HONEY to setOf("honey", "bal"),
        IngredientSafetyGroup.GLUTEN to setOf("gluten", "wheat", "bugday"),
        IngredientSafetyGroup.TREE_NUTS to setOf("tree nuts", "nuts", "kuruyemis", "findik"),
        IngredientSafetyGroup.PEANUT to setOf("peanut", "yer fistigi"),
        IngredientSafetyGroup.SOY to setOf("soy", "soya"),
        IngredientSafetyGroup.SESAME to setOf("sesame", "susam")
    )
}
