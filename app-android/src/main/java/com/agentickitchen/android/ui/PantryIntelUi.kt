package com.agentickitchen.android.ui

import com.agentickitchen.android.L
import com.agentickitchen.shared.models.PantryIntelSignal

fun pantryCategoryLabel(id: String): String = when (id) {
    "vegetation" -> if (L.isTr) "Bitkisel Hat" else "Vegetation"
    "protein_aqua" -> if (L.isTr) "Su Proteini" else "Protein Aqua"
    "protein_land" -> if (L.isTr) "Kara Proteini" else "Protein Land"
    "carb_matrix" -> if (L.isTr) "Karbon Matris" else "Carb Matrix"
    "spice_payload" -> if (L.isTr) "Baharat Yükü" else "Spice Payload"
    "liquids" -> if (L.isTr) "Sıvı Kanalı" else "Liquids"
    else -> if (L.isTr) "Sınıflandırılamadı" else "Unclassified"
}

fun pantrySignalText(signal: PantryIntelSignal): String = when (signal.code) {
    "diet_conflict" -> if (L.isTr) "Seçili diyet ile envanter çakışıyor." else "Current inventory conflicts with the selected diet profile."
    "needs_liquid" -> if (L.isTr) "Kuru bitişi önlemek için sıvı desteği ekle." else "Add a liquid support lane to avoid a dry finish."
    "needs_aromatic" -> if (L.isTr) "Derinlik için aromatik temel eksik." else "Add an aromatic anchor for depth and control."
    "needs_protein" -> if (L.isTr) "Plan ana yemekten çok garnitüre kayıyor; protein omurgası eksik." else "Protein anchor missing; plan will skew side-dish heavy."
    "add_liquid_support" -> if (L.isTr) "Son ısı aşamasından önce su, et suyu, krema veya sos tabanı ekle." else "Introduce water, stock, cream, or a sauce base before final heat."
    "add_protein_anchor" -> if (L.isTr) "Plan üretmeden önce bakliyat, yumurta, balık veya et ekle." else "Bring in legumes, eggs, fish, or meat before generating the plan."
    "protein_forward_plan" -> if (L.isTr) "Önce protein hattını kilitle, ardından yan bileşenleri bunun etrafına kur." else "Lead with the protein lane and build the rest of the operation around it."
    "balanced_payload" -> if (L.isTr) "Yük dengeli; katmanlı ana yemek üretimine uygun." else "Payload is balanced enough for a layered main dish."
    "hybrid_finish_lane" -> if (L.isTr) "Lezzeti ocakta kur, final stabilitesini fırında al." else "Use stovetop for flavor buildup, then finish with stable oven heat."
    "controlled_roast_lane" -> if (L.isTr) "Zaman oynaklığını azaltmak için kontrollü kızartma hattını tercih et." else "Favor controlled roasting to reduce timing volatility."
    "rapid_pan_lane" -> if (L.isTr) "Hızlı tava hattını seç; bekleme süresini kısa tut." else "Favor fast pan work and short hold times."
    "adaptive_lane" -> if (L.isTr) "Envanter güçlenene kadar kısa ve geri alınabilir adımlarla ilerle." else "Stay with short reversible steps until the pantry improves."
    else -> signal.message
}

fun equipmentLaneLabel(code: String): String = when (code) {
    "hybrid_finish" -> if (L.isTr) "Hibrit Bitiriş" else "Hybrid Finish"
    "controlled_roast" -> if (L.isTr) "Kontrollü Fırın Hattı" else "Controlled Roast"
    "rapid_pan" -> if (L.isTr) "Hızlı Tava Hattı" else "Rapid Pan"
    else -> if (L.isTr) "Uyarlanabilir Hat" else "Adaptive Lane"
}
