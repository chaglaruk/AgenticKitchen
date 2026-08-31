from pathlib import Path


def replace_once(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Anchor not found in {path}: {old[:120]!r}')
    if text.count(old) != 1:
        raise SystemExit(f'Anchor not unique in {path}: {text.count(old)} matches')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')

replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''internal fun aiConnectionStatusFor(result: AiResult<*>): AiConnectionStatus = when (result) {
''',
    '''internal fun canStartPreparedCooking(shortages: List<String>): Boolean = shortages.isEmpty()

internal fun aiConnectionStatusFor(result: AiResult<*>): AiConnectionStatus = when (result) {
'''
)

replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''        if (_cookingState.value.status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
''',
    '''        if (!canStartPreparedCooking(active.shortages)) {
            _cookingState.value = _cookingState.value.copy(
                recipeName = active.recipe.name,
                status = CookingSessionStatus.READY,
                error = if (L.isTr) {
                    "Pişirmeye başlamadan önce eksik malzemeleri tamamla veya güvenli bir alternatif uygula."
                } else {
                    "Resolve missing ingredients or apply a safe substitution before starting cooking."
                }
            )
            return
        }
        if (_cookingState.value.status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
'''
)

replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt',
    'import com.agentickitchen.android.SubstitutionState\n',
    'import com.agentickitchen.android.SubstitutionState\nimport com.agentickitchen.android.canStartPreparedCooking\n'
)

replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt',
    '''        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(if (L.isTr) "Pişirmeye Başla" else "Start Cooking", color = colors.onPrimary)
        }
''',
    '''        Spacer(Modifier.height(18.dp))
        val canStart = activePlan?.let { canStartPreparedCooking(it.shortages) } ?: true
        if (!canStart) {
            Text(
                if (L.isTr) "Önce aşağıdaki eksikleri tamamla veya güvenli bir alternatif uygula." else "Resolve the shortages below or apply a safe substitution first.",
                color = colors.accent,
                style = MaterialTheme.typography.body2
            )
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(if (L.isTr) "Pişirmeye Başla" else "Start Cooking", color = colors.onPrimary)
        }
'''
)

Path('app-android/src/test/java/com/agentickitchen/android/CookingStartGuardTest.kt').write_text(r'''package com.agentickitchen.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookingStartGuardTest {
    @Test fun preparedRecipeCanStartOnlyAfterShortagesAreResolved() {
        assertTrue(canStartPreparedCooking(emptyList()))
        assertFalse(canStartPreparedCooking(listOf("Milk")))
        assertFalse(canStartPreparedCooking(listOf("Milk", "Onion")))
    }
}
''', encoding='utf-8')
