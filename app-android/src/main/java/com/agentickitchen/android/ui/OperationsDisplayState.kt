package com.agentickitchen.android.ui

import com.agentickitchen.shared.cooking.CookingSessionStatus

internal fun shouldShowCookingPanel(
    status: CookingSessionStatus,
    hasActivePlan: Boolean
): Boolean = when (status) {
    CookingSessionStatus.READY,
    CookingSessionStatus.COMPLETED,
    CookingSessionStatus.ENDED -> hasActivePlan

    CookingSessionStatus.RUNNING,
    CookingSessionStatus.PAUSED,
    CookingSessionStatus.ERROR -> true
}
