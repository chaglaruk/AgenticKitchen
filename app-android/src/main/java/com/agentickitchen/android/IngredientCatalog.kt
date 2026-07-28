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
            aliasesTr = listOf(nameTr),
            aliasesEn = listOf(nameEn) + when (ingredientId) {
                "cream-cheese" -> listOf("cream cheese")
                "bicarbonate-soda" -> listOf("baking soda")
                else -> emptyList()
            },
            categoryId = id,
            visualKind = visual
        )
    }
    return IngredientCategory(id, tr, en, definitions.map { it.nameTr to it.nameEn }) to definitions
}

private val catalogGroups = listOf(
    catalogCategory("meat_poultry", "Et ve tavuk", "Meat and poultry", IngredientVisualKind.CHICKEN, """
        chicken-breast|Tavuk göğsü|Chicken breast
        chicken-thigh|Tavuk but|Chicken thighs
        chicken-wing|Tavuk kanadı|Chicken wings
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
        white-fish|Beyaz balık|White fish
        tuna|Ton balığı|Tuna
        anchovy|Hamsi|Anchovy
        sardine|Sardalya|Sardines
        sea-bass|Levrek|Sea bass
        bream|Çipura|Sea bream
        cod|Morina|Cod
        shrimp|Karides|Prawns
        mussels|Midye|Mussels
        squid|Kalamar|Squid
        octopus|Ahtapot|Octopus
        crab|Yengeç|Crab
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
    """)
)

val INGREDIENT_CATEGORIES: List<IngredientCategory> = catalogGroups.map(Pair<IngredientCategory, List<IngredientDefinition>>::first)
internal val INGREDIENT_CATALOG: List<IngredientDefinition> = catalogGroups.flatMap(Pair<IngredientCategory, List<IngredientDefinition>>::second)

private fun normalizedIngredientText(value: String): String = value.lowercase(Locale.ROOT)
    .replace('ı', 'i').replace('İ', 'i').replace('ş', 's').replace('Ş', 's')
    .replace('ğ', 'g').replace('Ğ', 'g').replace('ü', 'u').replace('Ü', 'u')
    .replace('ö', 'o').replace('Ö', 'o').replace('ç', 'c').replace('Ç', 'c')

internal fun searchIngredientCatalog(query: String, alreadyAdded: Collection<String>, isTurkish: Boolean, limit: Int = 5): List<IngredientDefinition> {
    val normalizedQuery = normalizedIngredientText(query.trim())
    if (normalizedQuery.isBlank()) return emptyList()
    val added = alreadyAdded.map(::normalizedIngredientText).toSet()
    return INGREDIENT_CATALOG.mapIndexedNotNull { index, ingredient ->
        val fields = listOf(ingredient.nameTr, ingredient.nameEn) + ingredient.aliasesTr + ingredient.aliasesEn
        val normalizedFields = fields.map(::normalizedIngredientText)
        val alreadyPresent = listOf(ingredient.nameTr, ingredient.nameEn).map(::normalizedIngredientText).any { it in added }
        val score = when {
            normalizedFields.any { it.startsWith(normalizedQuery) } -> 0
            normalizedFields.any { normalizedQuery in it } -> 1
            else -> return@mapIndexedNotNull null
        }
        if (alreadyPresent) null else Triple(score, index, ingredient)
    }.sortedWith(compareBy<Triple<Int, Int, IngredientDefinition>> { it.first }.thenBy { it.second })
        .take(limit)
        .map { it.third }
}
