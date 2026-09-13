package com.agentickitchen.android.ui

import com.agentickitchen.android.L
import com.agentickitchen.shared.models.PantryIntelSignal

fun pantryCategoryLabel(id: String): String = when (id) {
    "vegetation" -> if (L.isTr) "Sebze ve yeşillikler" else "Vegetables and greens"
    "protein_aqua" -> if (L.isTr) "Balık ve deniz ürünleri" else "Fish and seafood"
    "protein_land" -> if (L.isTr) "Et, tavuk ve yumurta" else "Meat, poultry and eggs"
    "carb_matrix" -> if (L.isTr) "Tahıllar ve nişastalar" else "Grains and starches"
    "spice_payload" -> if (L.isTr) "Baharatlar ve aromatikler" else "Spices and aromatics"
    "liquids" -> if (L.isTr) "Sıvılar ve soslar" else "Liquids and sauces"
    else -> if (L.isTr) "Diğer" else "Other"
}

fun pantrySignalText(signal: PantryIntelSignal): String = when (signal.code) {
    "diet_conflict" -> if (L.isTr) "Seçili beslenme tercihi bazı malzemelerle uyuşmuyor." else "Some ingredients do not match the selected dietary preference."
    "needs_liquid" -> if (L.isTr) "Yemeğin kuru kalmaması için biraz sıvı ekleyebilirsin." else "Add a little liquid to keep the dish from becoming dry."
    "needs_aromatic" -> if (L.isTr) "Lezzeti derinleştirmek için soğan, sarımsak veya taze ot ekleyebilirsin." else "Add onion, garlic or fresh herbs for more depth."
    "needs_protein" -> if (L.isTr) "Daha doyurucu olması için yumurta, bakliyat, balık, tavuk veya et ekleyebilirsin." else "Add eggs, legumes, fish, poultry or meat for a more substantial dish."
    "add_liquid_support" -> if (L.isTr) "Su, et suyu, krema veya sos eklemek yemeği daha yumuşak yapabilir." else "Water, stock, cream or a sauce can make the dish more tender."
    "add_protein_anchor" -> if (L.isTr) "Bakliyat, yumurta, balık veya et eklemek tarifi daha doyurucu yapabilir." else "Legumes, eggs, fish or meat can make the recipe more substantial."
    "protein_forward_plan" -> if (L.isTr) "Önce proteini pişirip diğer malzemeleri onun etrafında hazırlayabilirsin." else "Cook the protein first, then build the rest of the dish around it."
    "balanced_payload" -> if (L.isTr) "Malzemeler dengeli bir ana yemek hazırlamak için uygun görünüyor." else "The ingredients look balanced enough for a complete main dish."
    "hybrid_finish_lane" -> if (L.isTr) "Lezzeti ocakta geliştirip yemeği fırında tamamlayabilirsin." else "Build flavour on the stovetop, then finish the dish in the oven."
    "controlled_roast_lane" -> if (L.isTr) "Dengeli pişirme için fırında kontrollü şekilde pişirebilirsin." else "Use steady oven cooking for an even result."
    "rapid_pan_lane" -> if (L.isTr) "Malzemeleri kısa süre tavada pişirmek iyi bir seçenek olabilir." else "A short pan cook can be a good option for these ingredients."
    "adaptive_lane" -> if (L.isTr) "Elindeki malzemelere göre kısa ve esnek adımlarla ilerleyebilirsin." else "Use short, flexible steps that suit the ingredients you have."
    else -> signal.message
}

fun equipmentLaneLabel(code: String): String = when (code) {
    "hybrid_finish" -> if (L.isTr) "Ocak ve fırın" else "Stovetop and oven"
    "controlled_roast" -> if (L.isTr) "Kontrollü fırın" else "Controlled oven cooking"
    "rapid_pan" -> if (L.isTr) "Hızlı tava" else "Quick pan cooking"
    else -> if (L.isTr) "Esnek pişirme" else "Flexible cooking"
}
