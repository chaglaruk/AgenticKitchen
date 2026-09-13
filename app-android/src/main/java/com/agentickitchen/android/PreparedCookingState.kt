package com.agentickitchen.android

import com.agentickitchen.shared.cooking.CookingSessionState
import com.agentickitchen.shared.cooking.CookingSessionStatus

internal fun canReplacePreparedRecipe(status: CookingSessionStatus): Boolean =
    status !in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)

internal fun preparedCookingState(recipeName: String): CookingSessionState = CookingSessionState(
    recipeName = recipeName,
    status = CookingSessionStatus.READY
)
