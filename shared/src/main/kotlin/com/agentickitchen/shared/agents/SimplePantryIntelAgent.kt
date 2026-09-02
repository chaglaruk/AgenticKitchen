package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.PantryCategorySummary
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.PantryIntelSignal
class SimplePantryIntelAgent : PantryIntelAgent {
    private data class CategoryRule(
        val id: String,
        val label: String,
        val keywords: Set<String>,
        val focusWeight: Double
    )

    private val categoryRules = listOf(
        CategoryRule("vegetation", "Vegetables", setOf("onion", "garlic", "tomato", "potato", "mushroom", "lemon", "broccoli", "pepper", "biber", "patates", "sogan", "sarimsak", "domates", "mantar", "limon"), 1.0),
        CategoryRule("protein_aqua", "Fish and seafood", setOf("salmon", "fish", "tuna", "shrimp", "prawn", "somon", "balik", "karides"), 2.4),
        CategoryRule("protein_land", "Meat, poultry and eggs", setOf("chicken", "beef", "lamb", "turkey", "egg", "yogurt", "cheese", "tavuk", "kiyma", "dana", "et", "yumurta", "peynir"), 2.2),
        CategoryRule("carb_matrix", "Grains and legumes", setOf("rice", "pasta", "bread", "noodle", "potato", "chickpea", "bean", "pirinc", "makarna", "ekmek", "nohut", "fasulye"), 1.4),
        CategoryRule("spice_payload", "Seasonings", setOf("salt", "pepper", "paprika", "cumin", "oregano", "thyme", "oil", "salt", "tuz", "karabiber", "kimyon", "kekik", "yag", "zeytinyagi"), 1.1),
        CategoryRule("liquids", "Liquids", setOf("water", "milk", "cream", "stock", "broth", "wine", "su", "sut", "krema", "bulyon", "et suyu"), 0.9)
    )

    private val aromaticKeywords = setOf("onion", "garlic", "lemon", "ginger", "shallot", "sogan", "sarimsak", "limon", "zencefil")
    private val animalProteinKeywords = setOf("chicken", "beef", "lamb", "turkey", "fish", "salmon", "tuna", "shrimp", "tavuk", "dana", "et", "balik", "somon", "karides")

    override fun analyze(
        ingredients: List<String>,
        equipment: Set<String>,
        dietType: String
    ): PantryIntelReport {
        val normalizedIngredients = ingredients.map(::normalize).filter { it.isNotBlank() }
        val weightedRules = categoryRules.associateBy { it.id }
        val breakdown = categoryRules.map { rule ->
            PantryCategorySummary(
                id = rule.id,
                label = rule.label,
                count = normalizedIngredients.count { ingredient -> rule.keywords.any { keyword -> ingredient.contains(keyword) } }
            )
        }

        val focus = breakdown
            .maxByOrNull { summary -> (summary.count * (weightedRules[summary.id]?.focusWeight ?: 1.0)) }
            ?.takeIf { it.count > 0 }
            ?: PantryCategorySummary("unknown", "Unclassified", 0)

        val warnings = mutableListOf<PantryIntelSignal>()
        val tactics = mutableListOf<PantryIntelSignal>()

        val uniqueCategoryCount = breakdown.count { it.count > 0 }
        val hasProtein = breakdown.any { it.id.startsWith("protein_") && it.count > 0 }
        val hasLiquid = breakdown.any { it.id == "liquids" && it.count > 0 }
        val hasAromatic = normalizedIngredients.any { ingredient -> aromaticKeywords.any { ingredient.contains(it) } }
        val hasDietConflict = hasDietConflict(normalizedIngredients, dietType)

        if (hasDietConflict) {
            warnings += PantryIntelSignal("diet_conflict", "Current inventory conflicts with the selected diet profile.")
        }
        if (!hasLiquid) {
            warnings += PantryIntelSignal("needs_liquid", "Add a little liquid to avoid a dry finish.")
            tactics += PantryIntelSignal("add_liquid_support", "Add water, stock, cream, or sauce before the final heating step.")
        }
        if (!hasAromatic) {
            warnings += PantryIntelSignal("needs_aromatic", "Add onion, garlic, citrus, or herbs for more depth.")
        }
        if (!hasProtein) {
            warnings += PantryIntelSignal("needs_protein", "No main protein is present; the result may work better as a side dish.")
            tactics += PantryIntelSignal("add_protein_anchor", "Add legumes, eggs, fish, or meat before generating the recipe.")
        } else {
            tactics += PantryIntelSignal("protein_forward_plan", "Cook the main protein first, then build the rest of the dish around it.")
        }
        if (uniqueCategoryCount >= 4) {
            tactics += PantryIntelSignal("balanced_payload", "The ingredients are balanced enough for a complete main dish.")
        }

        val equipmentLane = when {
            equipment.contains("oven") && equipment.contains("pan") -> "hybrid_finish"
            equipment.contains("oven") || equipment.contains("airfryer") -> "controlled_roast"
            equipment.contains("pan") || equipment.contains("elec") || equipment.contains("gas") -> "rapid_pan"
            else -> "adaptive_lane"
        }

        tactics += when (equipmentLane) {
            "hybrid_finish" -> PantryIntelSignal("hybrid_finish_lane", "Use stovetop for flavor buildup, then finish with stable oven heat.")
            "controlled_roast" -> PantryIntelSignal("controlled_roast_lane", "Use steady oven cooking for an even result.")
            "rapid_pan" -> PantryIntelSignal("rapid_pan_lane", "Favor fast pan work and short hold times.")
            else -> PantryIntelSignal("adaptive_lane", "Stay with short reversible steps until the pantry improves.")
        }

        val rawScore = buildList {
            add(normalizedIngredients.size.coerceAtMost(6) * 8)
            add(uniqueCategoryCount * 10)
            add(equipment.size.coerceAtMost(3) * 4)
            if (hasProtein) add(8)
            if (hasLiquid) add(6)
            if (hasAromatic) add(6)
            if (hasDietConflict) add(-35)
            if (!hasLiquid) add(-10)
            if (!hasAromatic) add(-10)
            if (!hasProtein) add(-10)
        }.sum()

        val readinessScore = rawScore.coerceIn(12, 96)

        return PantryIntelReport(
            readinessScore = readinessScore,
            focusCategoryId = focus.id,
            focusCategoryLabel = focus.label,
            categoryBreakdown = breakdown,
            warnings = warnings,
            tactics = tactics.distinctBy { it.code },
            equipmentLane = equipmentLane
        )
    }

    private fun hasDietConflict(ingredients: List<String>, dietType: String): Boolean {
        val hasAnimalProtein = ingredients.any { ingredient -> animalProteinKeywords.any { ingredient.contains(it) } }
        return when (dietType.lowercase()) {
            "vegan" -> hasAnimalProtein || ingredients.any { it.contains("egg") || it.contains("yumurta") || it.contains("cheese") || it.contains("peynir") || it.contains("milk") || it.contains("sut") }
            "vegetarian" -> hasAnimalProtein
            else -> false
        }
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
