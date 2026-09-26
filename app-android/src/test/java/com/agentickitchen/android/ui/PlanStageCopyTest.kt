package com.agentickitchen.android.ui

import com.agentickitchen.android.PlanStage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stage-aware copy for the shared PlanState renderings: the Cooking Plan stage must not
 * reuse Recipe Options loading/error wording.
 */
class PlanStageCopyTest {

    @Test
    fun `recipe options keeps existing wording`() {
        assertEquals("Tariflere bakıyorum…", planLoadingText(PlanStage.RECIPE_OPTIONS, true))
        assertEquals("Finding a few good options…", planLoadingText(PlanStage.RECIPE_OPTIONS, false))
        assertEquals("Tarifler hazırlanamadı.", planErrorTitle(PlanStage.RECIPE_OPTIONS, true))
        assertEquals("Recipes could not be prepared.", planErrorTitle(PlanStage.RECIPE_OPTIONS, false))
    }

    @Test
    fun `cooking plan uses distinct stage wording`() {
        assertEquals("Pişirme planı hazırlanıyor…", planLoadingText(PlanStage.COOKING_PLAN, true))
        assertEquals("Preparing the cooking plan…", planLoadingText(PlanStage.COOKING_PLAN, false))
        assertEquals("Pişirme planı hazırlanamadı.", planErrorTitle(PlanStage.COOKING_PLAN, true))
        assertEquals("The cooking plan could not be prepared.", planErrorTitle(PlanStage.COOKING_PLAN, false))
    }

    @Test
    fun `cooking plan wording differs from recipe options wording`() {
        PlanStage.entries.forEach { stage ->
            PlanStage.entries.forEach { other ->
                if (stage != other) {
                    assert(planLoadingText(stage, true) != planLoadingText(other, true))
                    assert(planErrorTitle(stage, true) != planErrorTitle(other, true))
                }
            }
        }
    }
}
