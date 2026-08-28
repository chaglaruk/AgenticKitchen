package com.agentickitchen.android.ui

import com.agentickitchen.shared.cooking.CookingSessionStatus

internal fun shouldShowCookingPanel(
    status: CookingSessionStatus,
    recipeName: String,
    hasActivePlan: Boolean
): Boolean =
    status != CookingSessionStatus.READY || recipeName.isNotBlank() || hasActivePlan
