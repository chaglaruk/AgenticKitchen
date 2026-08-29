package com.agentickitchen.android

import com.agentickitchen.android.ui.IngredientVisualKind
import java.util.Locale

internal data class IngredientDefinition(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val aliasesTr: List<String> = emptyList(),
    val aliasesEn: List<String> = emptyList(),
    val categoryId: String,
    val visualKind: IngredientVisualKind
) {
    fun name(isTurkish: Boolean) = if (isTurkish) nameTr else nameEn
}

data class IngredientCategory(
    val id: String,
    val labelTr: String,
    val labelEn: String,
    val items: List<Pair<String, String>>
) {
    val label get() = if (L.isTr) labelTr else labelEn
    fun displayItems() = items
}

private fun catalogCategory(
    id: String,
    tr: String,
    en: String,
    visual: IngredientVisualKind,
    rows: String
): Pair<IngredientCategory, List<IngredientDefinition>> {
    val definitions = rows.trimIndent().lines().filter(String::isNotBlank).map { row ->
        val (ingredientId, nameTr, nameEn) = row.split('|', limit = 3)
        IngredientDefinition(
            id = ingredientId,
            nameTr = nameTr,
            nameEn = nameEn,
            aliasesTr = listOf(nameTr) + ingredientAliases(ingredientId, true),
            aliasesEn = listOf(nameEn) + ingredientAliases(ingredientId, false),
            categoryId = id,
            visualKind = visualKindFor(ingredientId, visual)
        )
    }
    return IngredientCategory(id, tr, en, definitions.map { it.nameTr to it.nameEn }) to definitions
}

private fun ingredientAliases(id: String, isTurkish: Boolean): List<String> = when (id) {
    "whole-chicken" -> if (isTurkish) listOf("tavuk", "bütün tavuk") else listOf("chicken", "whole chicken")
    "chicken-breast" -> if (isTurkish) listOf("tavuk", "tavuk fileto") else listOf("chicken", "chicken fillet")
    "chicken-thigh" -> if (isTurkish) listOf("tavuk", "kalçalı but") else listOf("chicken", "chicken thigh")
    "chicken-drumstick" -> if (isTurkish) listOf("tavuk", "baget") else listOf("chicken", "drumstick")
    "chicken-wing" -> if (isTurkish) listOf("tavuk") else listOf("chicken")
    "trout", "mackerel", "bonito", "bluefish", "salmon", "tuna", "anchovy", "sardine", "sea-bass", "bream", "cod", "haddock" ->
        if (isTurkish) listOf("balık") else listOf("fish")
    "potato" -> if (isTurkish) listOf("patates") else listOf("potato", "potatoes")
    "sweet-potato" -> if (isTurkish) listOf("patates", "tatlı patates") else listOf("potato", "sweet potato", "sweet potatoes")
    "button-mushroom", "oyster-mushroom", "chestnut-mushroom" ->
        if (isTurkish) listOf("mantar") else listOf("mushroom", "mushrooms")
    "cream-cheese" -> if (isTurkish) emptyList() else listOf("cream cheese")
    "bicarbonate-soda" -> if (isTurkish) emptyList() else listOf("baking soda")
    "coriander" -> if (isTurkish) emptyList() else listOf("cilantro")
    "aubergine" -> if (isTurkish) emptyList() else listOf("eggplant")
    "courgette" -> if (isTurkish) emptyList() else listOf("zucchini")
    "rocket" -> if (isTurkish) emptyList() else listOf("arugula")
    "prawns" -> if (isTurkish) listOf("karides") else listOf("shrimp")
    else -> emptyList()
}

private fun visualKindFor(id: String, fallback: IngredientVisualKind): IngredientVisualKind = when (id) {
    "potato", "sweet-potato", "new-potato" -> IngredientVisualKind.POTATO
    "button-mushroom", "oyster-mushroom", "chestnut-mushroom" -> IngredientVisualKind.MUSHROOM
    "turkey" -> IngredientVisualKind.TURKEY
    "minced-beef", "beef", "lamb", "steak" -> IngredientVisualKind.RED_MEAT
    "meatballs", "sausage", "bacon", "liver", "deli-meat" -> IngredientVisualKind.MEAT
    "prawns", "mussels", "squid", "octopus", "crab", "scallops" -> IngredientVisualKind.SEAFOOD
    "milk", "double-cream", "sour-cream" -> IngredientVisualKind.MILK_CREAM
    "yoghurt", "greek-yoghurt" -> IngredientVisualKind.YOGHURT
    "feta", "kasar", "mozzarella", "cheddar", "parmesan", "cream-cheese" -> IngredientVisualKind.CHEESE
    "butter" -> IngredientVisualKind.BUTTER
    "red-pepper", "green-pepper", "chilli-pepper" -> IngredientVisualKind.PEPPER
    "cucumber" -> IngredientVisualKind.CUCUMBER
    "aubergine", "courgette" -> IngredientVisualKind.SQUASH
    "leek", "celery", "carrot", "beetroot", "ginger", "parsnip", "turnip", "radish" -> IngredientVisualKind.ROOT_VEGETABLE
    "cauliflower", "broccoli", "green-beans", "spinach", "lettuce", "rocket", "kale", "chard" -> IngredientVisualKind.LEAFY
    "onion", "spring-onion" -> IngredientVisualKind.ONION
    "garlic" -> IngredientVisualKind.GARLIC
    "parsley", "dill", "mint", "basil", "coriander", "thyme", "rosemary", "oregano", "bay-leaf" -> IngredientVisualKind.HERBS
    "lemon", "lime", "orange" -> IngredientVisualKind.CITRUS
    "rice", "brown-rice", "basmati-rice" -> IngredientVisualKind.RICE
    "pasta", "spaghetti", "noodles", "orzo", "vermicelli" -> IngredientVisualKind.PASTA
    "bread", "pita", "tortilla", "breadcrumbs", "sourdough", "flatbread" -> IngredientVisualKind.BREAD
    "olive-oil", "sunflower-oil", "vegetable-oil" -> IngredientVisualKind.OIL
    "tahini", "molasses", "tomato-paste", "pepper-paste", "soy-sauce", "vinegar", "balsamic", "mustard", "mayonnaise", "ketchup" -> IngredientVisualKind.SAUCE
    "flour", "wholemeal-flour", "cornflour", "semolina", "cocoa", "baking-powder", "bicarbonate-soda", "vanilla", "yeast", "chocolate", "stock" -> IngredientVisualKind.FLOUR_BAKING
    "sugar", "brown-sugar", "honey" -> IngredientVisualKind.SUGAR_HONEY
    else -> fallback
}

private val catalogGroups = listOf(
    catalogCategory("meat_poultry", "Et ve tavuk", "Meat and poultry", IngredientVisualKind.CHICKEN, """
        chicken-breast|Tavuk göğsü|Chicken breast
        chicken-thigh|Tavuk but|Chicken thighs
        chicken-drumstick|Tavuk baget|Chicken drumsticks
        chicken-wing|Tavuk kanadı|Chicken wings
        whole-chicken|Bütün tavuk|Whole chicken
        turkey|Hindi|Turkey
        minced-beef|Kıyma|Ground beef
        beef|Dana eti|Beef
        lamb|Kuzu eti|Lamb
        steak|Biftek|Steak
        meatballs|Köfte|Meatballs
        sausage|Sosis|Sausage
        bacon|Pastırma|Bacon
        liver|Ciğer|Liver
        deli-meat|Şarküteri eti|Deli meat
    """),
    catalogCategory("fish_seafood", "Balık ve deniz ürünleri", "Fish and seafood", IngredientVisualKind.FISH, """
        salmon|Somon|Salmon
        tuna|Ton balığı|Tuna
        anchovy|Hamsi|Anchovy
        sardine|Sardalya|Sardines
        sea-bass|Levrek|Sea bass
        bream|Çipura|Sea bream
        trout|Alabalık|Trout
        mackerel|Uskumru|Mackerel
        bonito|Palamut|Bonito
        bluefish|Lüfer|Bluefish
        cod|Morina|Cod
        haddock|Mezgit|Haddock
        prawns|Karides|Prawns
        mussels|Midye|Mussels
        squid|Kalamar|Squid
        octopus|Ahtapot|Octopus
        crab|Yengeç|Crab
        scallops|Deniz tarağı|Scallops
    """),
    catalogCategory("eggs_dairy", "Yumurta ve süt ürünleri", "Eggs and dairy", IngredientVisualKind.EGG, """
        egg|Yumurta|Egg
        milk|Süt|Milk
        yoghurt|Yoğurt|Yoghurt
        greek-yoghurt|Süzme yoğurt|Greek yoghurt
        feta|Beyaz peynir|Feta
        kasar|Kaşar peyniri|Kashar cheese
        mozzarella|Mozzarella|Mozzarella
        cheddar|Çedar peyniri|Cheddar
        parmesan|Parmesan|Parmesan
        cream-cheese|Krem peynir|Soft cheese
        double-cream|Krema|Double cream
        butter|Tereyağı|Butter
        sour-cream|Ekşi krema|Sour cream
    """),
    catalogCategory("vegetables", "Sebzeler", "Vegetables", IngredientVisualKind.TOMATO, """
        tomato|Domates|Tomato
        red-pepper|Kırmızı biber|Red pepper
        green-pepper|Yeşil biber|Green pepper
        cucumber|Salatalık|Cucumber
        aubergine|Patlıcan|Aubergine
        courgette|Kabak|Courgette
        leek|Pırasa|Leek
        celery|Kereviz|Celery
        carrot|Havuç|Carrot
        beetroot|Pancar|Beetroot
        cauliflower|Karnabahar|Cauliflower
        broccoli|Brokoli|Broccoli
        green-beans|Taze fasulye|Green beans
        peas-fresh|Taze bezelye|Fresh peas
        sweetcorn|Mısır|Sweetcorn
        artichoke|Enginar|Artichoke
        okra|Bamya|Okra
        cabbage|Lahana|Cabbage
        brussels-sprouts|Brüksel lahanası|Brussels sprouts
        asparagus|Kuşkonmaz|Asparagus
        chilli-pepper|Acı biber|Chilli pepper
    """),
    catalogCategory("roots_mushrooms", "Patates, kök sebzeler ve mantarlar", "Potatoes, roots and mushrooms", IngredientVisualKind.ROOT_VEGETABLE, """
        potato|Patates|Potato
        sweet-potato|Tatlı patates|Sweet potato
        new-potato|Taze patates|New potatoes
        button-mushroom|Kültür mantarı|Button mushrooms
        oyster-mushroom|İstiridye mantarı|Oyster mushrooms
        chestnut-mushroom|Kestane mantarı|Chestnut mushrooms
        parsnip|Yaban havucu|Parsnip
        turnip|Şalgam|Turnip
        radish|Turp|Radish
    """),
    catalogCategory("greens_herbs", "Yeşillikler ve taze otlar", "Leafy greens and fresh herbs", IngredientVisualKind.HERBS, """
        spinach|Ispanak|Spinach
        lettuce|Marul|Lettuce
        rocket|Roka|Rocket
        kale|Kara lahana|Kale
        chard|Pazı|Chard
        parsley|Maydanoz|Parsley
        dill|Dereotu|Dill
        mint|Nane|Mint
        basil|Fesleğen|Basil
        coriander|Kişniş|Coriander
        thyme|Kekik|Thyme
        rosemary|Biberiye|Rosemary
        spring-onion|Taze soğan|Spring onion
    """),
    catalogCategory("fruits_citrus", "Meyveler ve narenciye", "Fruits and citrus", IngredientVisualKind.FRUIT, """
        lemon|Limon|Lemon
        lime|Misket limonu|Lime
        orange|Portakal|Orange
        apple|Elma|Apple
        pear|Armut|Pear
        banana|Muz|Banana
        strawberry|Çilek|Strawberry
        raspberry|Ahududu|Raspberry
        blueberry|Yaban mersini|Blueberry
        pomegranate|Nar|Pomegranate
        grape|Üzüm|Grapes
        peach|Şeftali|Peach
        avocado|Avokado|Avocado
        cherry|Kiraz|Cherries
        apricot|Kayısı|Apricot
        plum|Erik|Plum
        fig|İncir|Fig
        melon|Kavun|Melon
        watermelon|Karpuz|Watermelon
    """),
    catalogCategory("grains_bread", "Tahıllar, pirinç, makarna ve ekmek", "Grains, rice, pasta and bread", IngredientVisualKind.PASTA, """
        rice|Pirinç|Rice
        bulgur|Bulgur|Bulgur
        couscous|Kuskus|Couscous
        quinoa|Kinoa|Quinoa
        oats|Yulaf|Oats
        pasta|Makarna|Pasta
        spaghetti|Spagetti|Spaghetti
        noodles|Erişte|Noodles
        bread|Ekmek|Bread
        pita|Pide|Pita bread
        tortilla|Tortilla|Tortilla
        breadcrumbs|Galeta unu|Breadcrumbs
        barley|Arpa|Pearl barley
        brown-rice|Esmer pirinç|Brown rice
        basmati-rice|Basmati pirinci|Basmati rice
        orzo|Arpa şehriye|Orzo
        vermicelli|Tel şehriye|Vermicelli
        polenta|Mısır irmiği|Polenta
        semolina|İrmik|Semolina
        sourdough|Ekşi mayalı ekmek|Sourdough bread
        flatbread|Lavaş|Flatbread
    """),
    catalogCategory("legumes", "Bakliyatlar", "Legumes", IngredientVisualKind.LEGUMES, """
        red-lentils|Kırmızı mercimek|Red lentils
        green-lentils|Yeşil mercimek|Green lentils
        chickpeas|Nohut|Chickpeas
        dried-beans|Kuru fasulye|Dried beans
        kidney-beans|Barbunya|Kidney beans
        black-beans|Siyah fasulye|Black beans
        white-beans|Beyaz fasulye|Cannellini beans
        peas|Bezelye|Peas
        split-peas|Kırık bezelye|Split peas
        broad-beans|Bakla|Broad beans
        mung-beans|Maş fasulyesi|Mung beans
        edamame|Edamame|Edamame
        soybeans|Soya fasulyesi|Soybeans
        butter-beans|İri kuru fasulye|Butter beans
    """),
    catalogCategory("nuts_seeds", "Kuruyemişler ve tohumlar", "Nuts and seeds", IngredientVisualKind.NUTS_SEEDS, """
        walnuts|Ceviz|Walnuts
        hazelnuts|Fındık|Hazelnuts
        almonds|Badem|Almonds
        pistachios|Antep fıstığı|Pistachios
        cashews|Kaju|Cashews
        peanuts|Yer fıstığı|Peanuts
        sesame|Susam|Sesame
        chia|Chia tohumu|Chia seeds
        flax|Keten tohumu|Flaxseed
        sunflower-seeds|Ay çekirdeği|Sunflower seeds
        pumpkin-seeds|Kabak çekirdeği|Pumpkin seeds
        poppy-seeds|Haşhaş|Poppy seeds
        pine-nuts|Çam fıstığı|Pine nuts
        pecans|Pekan cevizi|Pecans
        hemp-seeds|Kenevir tohumu|Hemp seeds
    """),
    catalogCategory("spices_aromatics", "Baharatlar ve aromatikler", "Spices and aromatics", IngredientVisualKind.SPICES, """
        onion|Soğan|Onion
        garlic|Sarımsak|Garlic
        ginger|Zencefil|Ginger
        turmeric|Zerdeçal|Turmeric
        cumin|Kimyon|Cumin
        paprika|Toz kırmızı biber|Paprika
        black-pepper|Karabiber|Black pepper
        chilli-flakes|Pul biber|Chilli flakes
        cinnamon|Tarçın|Cinnamon
        oregano|Kuru kekik|Oregano
        bay-leaf|Defne yaprağı|Bay leaf
        curry|Köri|Curry powder
        salt|Tuz|Salt
        allspice|Yenibahar|Allspice
        sumac|Sumak|Sumac
        cardamom|Kakule|Cardamom
        cloves|Karanfil|Cloves
        nutmeg|Muskat|Nutmeg
        fennel-seeds|Rezene tohumu|Fennel seeds
    """),
    catalogCategory("oils_sauces", "Yağlar, soslar ve çeşniler", "Oils, sauces and condiments", IngredientVisualKind.SAUCE, """
        olive-oil|Zeytinyağı|Olive oil
        sunflower-oil|Ayçiçek yağı|Sunflower oil
        vegetable-oil|Bitkisel yağ|Vegetable oil
        tahini|Tahin|Tahini
        molasses|Pekmez|Molasses
        tomato-paste|Domates salçası|Tomato paste
        pepper-paste|Biber salçası|Pepper paste
        soy-sauce|Soya sosu|Soy sauce
        vinegar|Sirke|Vinegar
        balsamic|Balzamik sirke|Balsamic vinegar
        mustard|Hardal|Mustard
        mayonnaise|Mayonez|Mayonnaise
        ketchup|Ketçap|Ketchup
        pomegranate-molasses|Nar ekşisi|Pomegranate molasses
        hot-sauce|Acı sos|Hot sauce
        fish-sauce|Balık sosu|Fish sauce
        coconut-milk|Hindistan cevizi sütü|Coconut milk
    """),
    catalogCategory("baking_pantry", "Fırıncılık ve kiler", "Baking and pantry staples", IngredientVisualKind.FLOUR_BAKING, """
        flour|Un|Flour
        wholemeal-flour|Tam buğday unu|Wholemeal flour
        cornflour|Mısır nişastası|Cornflour
        sugar|Şeker|Sugar
        brown-sugar|Esmer şeker|Brown sugar
        honey|Bal|Honey
        cocoa|Kakao|Cocoa
        baking-powder|Kabartma tozu|Baking powder
        bicarbonate-soda|Karbonat|Bicarbonate of soda
        vanilla|Vanilya|Vanilla
        yeast|Maya|Yeast
        chocolate|Çikolata|Chocolate
        stock|Et suyu|Stock
        icing-sugar|Pudra şekeri|Icing sugar
        cornmeal|Mısır unu|Cornmeal
        desiccated-coconut|Hindistan cevizi rendesi|Desiccated coconut
    """)
)

val INGREDIENT_CATEGORIES: List<IngredientCategory> = catalogGroups.map(Pair<IngredientCategory, List<IngredientDefinition>>::first)
internal val INGREDIENT_CATALOG: List<IngredientDefinition> = catalogGroups.flatMap(Pair<IngredientCategory, List<IngredientDefinition>>::second)

private fun normalizedIngredientText(value: String): String = value.lowercase(Locale.ROOT)
    .replace('ı', 'i').replace('İ', 'i').replace('ş', 's').replace('Ş', 's')
    .replace('ğ', 'g').replace('Ğ', 'g').replace('ü', 'u').replace('Ü', 'u')
    .replace('ö', 'o').replace('Ö', 'o').replace('ç', 'c').replace('Ç', 'c')

internal fun catalogIngredientForName(name: String): IngredientDefinition? {
    val normalizedName = normalizedIngredientText(name)
    return INGREDIENT_CATALOG.firstOrNull { ingredient ->
        ingredient.id == name || listOf(ingredient.nameTr, ingredient.nameEn).plus(ingredient.aliasesTr).plus(ingredient.aliasesEn)
            .any { normalizedIngredientText(it) == normalizedName }
    }
}

internal fun canonicalIngredientName(name: String, isTurkish: Boolean): String {
    val trimmed = name.trim()
    return catalogIngredientForName(trimmed)?.name(isTurkish) ?: trimmed
}

internal fun searchIngredientCatalog(query: String, alreadyAdded: Collection<String>, isTurkish: Boolean, limit: Int = 5): List<IngredientDefinition> {
    val normalizedQuery = normalizedIngredientText(query.trim())
    if (normalizedQuery.isBlank()) return emptyList()
    val added = alreadyAdded.map(::normalizedIngredientText).toSet()
    return INGREDIENT_CATALOG.mapIndexedNotNull { index, ingredient ->
        val currentFields = if (isTurkish) listOf(ingredient.nameTr) + ingredient.aliasesTr else listOf(ingredient.nameEn) + ingredient.aliasesEn
        val fallbackFields = if (isTurkish) listOf(ingredient.nameEn) + ingredient.aliasesEn else listOf(ingredient.nameTr) + ingredient.aliasesTr
        val normalizedCurrent = currentFields.map(::normalizedIngredientText)
        val normalizedFallback = fallbackFields.map(::normalizedIngredientText)
        val alreadyPresent = listOf(ingredient.nameTr, ingredient.nameEn).map(::normalizedIngredientText).any { it in added }
        val score = when {
            normalizedCurrent.any { it.startsWith(normalizedQuery) } -> 0
            normalizedCurrent.any { normalizedQuery in it } -> 1
            normalizedFallback.any { it.startsWith(normalizedQuery) } -> 2
            normalizedFallback.any { normalizedQuery in it } -> 3
            else -> return@mapIndexedNotNull null
        }
        if (alreadyPresent) null else Triple(score, index, ingredient)
    }.sortedWith(compareBy<Triple<Int, Int, IngredientDefinition>> { it.first }.thenBy { it.second })
        .take(limit)
        .map { it.third }
}
