package com.agentickitchen.shared.ai

object VisionSafetyPolicy {
    const val MIN_SHOPPING_CONFIDENCE = 0.65

    fun filterShoppingCandidates(response: ShoppingImportResponse): ShoppingImportResponse =
        response.copy(
            items = response.items.filter { candidate ->
                candidate.confidence.isFinite() &&
                    candidate.confidence >= MIN_SHOPPING_CONFIDENCE &&
                    candidate.displayName.isNotBlank()
            }
        )

    fun validateCookingPhoto(response: CookingPhotoResponse): Boolean =
        response.assessment.isNotBlank() &&
            response.visibleObservation.isNotBlank() &&
            response.immediateAction.isNotBlank() &&
            response.uncertainty.isNotBlank()

    fun requireUserConfirmation(
        response: CookingPhotoResponse,
        language: String
    ): CookingPhotoResponse {
        val isTurkish = language.equals("Türkçe", ignoreCase = true) ||
            language.startsWith("tr", ignoreCase = true)
        val confirmation = if (isTurkish) {
            "Yalnızca fotoğrafa dayanarak ısıyı değiştirme, yemeği servis etme veya güvenli kabul etme. Görünümü kendin doğrula; gerekiyorsa gıda termometresi kullan."
        } else {
            "Do not change heat, serve, or treat the food as safe from the photo alone. Confirm it yourself and use a food thermometer when appropriate."
        }
        val uncertainty = response.uncertainty.trim().takeIf { it.isNotEmpty() }
            ?: if (isTurkish) {
                "Fotoğraf sıcaklığı, iç pişme derecesini veya gıda güvenliğini doğrulayamaz."
            } else {
                "A photo cannot verify temperature, internal doneness, or food safety."
            }
        val safetyWarning = listOfNotNull(
            response.safetyWarning?.trim()?.takeIf(String::isNotEmpty),
            confirmation
        ).distinct().joinToString("\n")

        return response.copy(
            immediateAction = confirmation,
            recheckAfterSeconds = response.recheckAfterSeconds?.coerceIn(15, 600),
            safetyWarning = safetyWarning,
            uncertainty = uncertainty
        )
    }
}
