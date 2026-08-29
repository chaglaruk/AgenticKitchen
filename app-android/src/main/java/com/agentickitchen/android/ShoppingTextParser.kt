package com.agentickitchen.android

import com.agentickitchen.shared.ai.ShoppingCandidate
import java.util.Locale

internal fun parseShoppingTextLocally(text: String, isTurkish: Boolean): List<ShoppingCandidate>? {
    val parts = text.trim()
        .split(Regex("""\s*(?:,|\r?\n|\s+ve\s+|\s+and\s+)\s*""", RegexOption.IGNORE_CASE))
        .map(String::trim)
        .filter(String::isNotBlank)
    if (parts.isEmpty()) return null
    val parsed = parts.mapNotNull { parseShoppingLine(it, isTurkish) }
    return parsed.takeIf { it.size == parts.size }
}

private fun parseShoppingLine(line: String, isTurkish: Boolean): ShoppingCandidate? {
    val match = Regex("""^(\d+(?:[.,]\d+)?)\s*([\p{L}.]+(?:\s+[\p{L}.]+)?)?\s+(.+)$""").matchEntire(line.trim()) ?: return null
    val quantity = match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: return null
    var possibleUnit = match.groupValues[2].trim()
    var name = match.groupValues[3].trim().removePrefix("of ").trim()
    var unit = shoppingUnit(possibleUnit)
    if (unit == null) {
        name = "$possibleUnit $name".trim()
        possibleUnit = ""
        unit = "piece"
    }
    val ingredient = resolveShoppingIngredient(name) ?: return null
    return ShoppingCandidate(
        canonicalIngredientId = ingredient.id,
        displayName = ingredient.name(isTurkish),
        quantity = quantity,
        unit = unit,
        unitDimension = shoppingUnitDimension(unit),
        confidence = 1.0,
        estimated = false
    )
}

private fun resolveShoppingIngredient(name: String): IngredientDefinition? {
    val candidates = buildList {
        add(name)
        add(name.removeSuffix("s"))
        add(name.removeSuffix("es"))
        add(name.removeSuffix("lar"))
        add(name.removeSuffix("ler"))
    }
    return candidates.firstNotNullOfOrNull(::catalogIngredientForName)
}

private fun shoppingUnit(raw: String): String? = when (raw.trim().lowercase(Locale.ROOT).removeSuffix(".")) {
    "kg", "kilo", "kilogram", "kilograms" -> "kg"
    "g", "gr", "gram", "grams" -> "g"
    "l", "litre", "litres", "liter", "liters" -> "l"
    "ml", "millilitre", "millilitres", "milliliter", "milliliters" -> "ml"
    "paket", "pack", "packs", "package", "packages" -> "package"
    "demet", "bunch", "bunches" -> "bunch"
    "adet", "piece", "pieces", "pcs", "count", "" -> "piece"
    else -> null
}

private fun shoppingUnitDimension(unit: String): String = when (unit) {
    "kg", "g" -> "weight"
    "l", "ml" -> "volume"
    "package" -> "package"
    "bunch", "piece" -> "count"
    else -> "unknown"
}
