package com.agentickitchen.android.ui

import com.agentickitchen.android.PlanStage

/**
 * Stage-aware copy for the shared PlanState loading/error renderings on the recipe screen.
 * Keeps the Cooking Plan stage from reusing Recipe Options wording.
 */
internal object PlanStageCopy {
    internal fun loadingText(stage: PlanStage, isTr: Boolean): String = when (stage) {
        PlanStage.RECIPE_OPTIONS ->
            if (isTr) "Tariflere bakıyorum…" else "Finding a few good options…"
        PlanStage.COOKING_PLAN ->
            if (isTr) "Pişirme planı hazırlanıyor…" else "Preparing the cooking plan…"
    }

    internal fun errorTitle(stage: PlanStage, isTr: Boolean): String = when (stage) {
        PlanStage.RECIPE_OPTIONS ->
            if (isTr) "Tarifler hazırlanamadı." else "Recipes could not be prepared."
        PlanStage.COOKING_PLAN ->
            if (isTr) "Pişirme planı hazırlanamadı." else "The cooking plan could not be prepared."
    }
}

internal fun planLoadingText(stage: PlanStage, isTr: Boolean): String =
    PlanStageCopy.loadingText(stage, isTr)

internal fun planErrorTitle(stage: PlanStage, isTr: Boolean): String =
    PlanStageCopy.errorTitle(stage, isTr)
