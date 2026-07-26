package com.agentickitchen.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import com.agentickitchen.android.ui.AgenticTheme
import com.agentickitchen.android.ui.ApiKeyOnboardingDialog
import com.agentickitchen.android.ui.HomeScreen
import com.agentickitchen.android.ui.LocalAppColors
import com.agentickitchen.android.ui.LocalThemeSpec
import com.agentickitchen.android.ui.OptionsScreen
import com.agentickitchen.android.ui.OperationsScreen
import com.agentickitchen.android.ui.SettingsScreen
import com.agentickitchen.android.ui.SetupScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsState()
            AgenticTheme(themeName = theme) {
                AppRoot(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    abstract fun title(): String

    data object Intelligence : Screen("intelligence", Icons.Filled.Psychology) {
        override fun title() = if (L.isTr) "İstihbarat" else "Intelligence"
    }

    data object Options : Screen("options", Icons.Filled.RestaurantMenu) {
        override fun title() = if (L.isTr) "Seçenekler" else "Options"
    }

    data object Operations : Screen("operations", Icons.Filled.PendingActions) {
        override fun title() = if (L.isTr) "Operasyon" else "Operations"
    }

    data object Settings : Screen("settings", Icons.Filled.Settings) {
        override fun title() = if (L.isTr) "Ayarlar" else "Configuration"
    }
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val setupDone by viewModel.setupDone.collectAsState()
    val isEditingSetup by viewModel.isEditingSetup.collectAsState()
    val hw by viewModel.hardwareSettings.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeySkipped by remember { mutableStateOf(false) }

    val needsApiKey = when (hw.aiProvider) {
        "GEMINI" -> hw.geminiApiKey.isBlank()
        "HUGGINGFACE" -> hw.hfApiKey.isBlank()
        "DUCKDUCKGO", "FREE" -> false  // No API key needed for free providers
        else -> false
    }

    LaunchedEffect(setupDone, hw.aiProvider, hw.geminiApiKey, hw.hfApiKey) {
        if (setupDone && needsApiKey && !apiKeySkipped) {
            showApiKeyDialog = true
        }
    }

    if (!setupDone || isEditingSetup) {
        SetupScreen(
            initialHw = hw,
            canGoBack = setupDone,
            onBack = { viewModel.cancelEditingSetup() },
            onComplete = { equipment, servings, mealTime, updatedHw ->
                viewModel.completeSetup(equipment, servings, mealTime, updatedHw)
            }
        )
    } else {
        AppNavigation(viewModel)

        val aiError by viewModel.aiError.collectAsState()
        if (aiError != null && !showApiKeyDialog) {
            if (aiError == "API_KEY_MISSING") {
                showApiKeyDialog = true
                apiKeySkipped = false
            }
            viewModel.clearAiError()
        }

        if (showApiKeyDialog) {
            ApiKeyOnboardingDialog(
                aiProvider = hw.aiProvider,
                onSave = { key ->
                    viewModel.saveApiKey(key)
                    showApiKeyDialog = false
                },
                onSkip = {
                    apiKeySkipped = true
                    showApiKeyDialog = false
                }
            )
        }
    }
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Intelligence) }

    val chips by viewModel.chips.collectAsState()
    val mealTime by viewModel.mealTime.collectAsState()
    val planState by viewModel.planState.collectAsState()
    val hw by viewModel.hardwareSettings.collectAsState()
    val notif by viewModel.notificationsEnabled.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val diet by viewModel.dietSettings.collectAsState()
    val lang by viewModel.language.collectAsState()
    val selectedEquipment by viewModel.selectedEquipment.collectAsState()
    val pantryIntel by viewModel.pantryIntel.collectAsState()
    val scannedIngredients by viewModel.scannedIngredients.collectAsState()

    LaunchedEffect(planState) {
        when (planState) {
            is PlanState.OptionsReady -> currentScreen = Screen.Options
            is PlanState.RecipeActive -> currentScreen = Screen.Operations
            else -> Unit
        }
    }

    val screens = listOf(Screen.Intelligence, Screen.Options, Screen.Operations, Screen.Settings)
    val colors = LocalAppColors.current
    val themeSpec = LocalThemeSpec.current
    val scaffoldState = rememberScaffoldState()
    val uiEvent by viewModel.uiEvent.collectAsState(initial = null)

    LaunchedEffect(uiEvent) {
        if (uiEvent is UiEvent.ShowSnackbar) {
            scaffoldState.snackbarHostState.showSnackbar((uiEvent as UiEvent.ShowSnackbar).message)
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = colors.background,
        bottomBar = {
            BottomNavigation(
                backgroundColor = if (themeSpec.id == "heritage") colors.surfaceAlt else colors.surface,
                contentColor = colors.onSurface,
                elevation = if (themeSpec.id == "signal") 10.dp else 0.dp
            ) {
                screens.forEach { screen ->
                    val selected = currentScreen == screen
                    BottomNavigationItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.title()) },
                        label = {
                            Text(
                                screen.title(),
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = colors.primary,
                        unselectedContentColor = colors.onSurfaceSub,
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentScreen) {
                Screen.Intelligence -> HomeScreen(
                    chips = chips,
                    scannedIngredients = scannedIngredients,
                    pantryIntel = pantryIntel,
                    onScanImage = viewModel::scanIngredients,
                    onClearScannedIngredients = viewModel::clearScannedIngredients,
                    onAddChip = viewModel::addChip,
                    onAddMultipleChips = viewModel::addMultipleChips,
                    onRemoveChip = viewModel::removeChip,
                    onClearAll = viewModel::clearAll,
                    onStart = viewModel::startSession,
                    onEditSetup = viewModel::startEditingSetup
                )

                Screen.Options -> OptionsScreen(
                    chips = chips,
                    planState = planState,
                    pantryIntel = pantryIntel,
                    onStart = viewModel::startSession,
                    onRefresh = viewModel::refreshSession,
                    onSelectOption = { opt, time -> viewModel.selectRecipeOption(opt, time) },
                    onBackToOptions = viewModel::backToOptions
                )

                Screen.Operations -> OperationsScreen(
                    planState = planState,
                    pantryIntel = pantryIntel,
                    hardwareSettings = hw,
                    selectedEquipment = selectedEquipment,
                    onAskAgent = viewModel::askIngredientAgent,
                    onClearChat = viewModel::clearAgentChat,
                    onCheckPan = viewModel::checkVisionAgent,
                    onClearVision = viewModel::clearVisionResponse,
                    onBackToOptions = viewModel::backToOptions
                )

                Screen.Settings -> SettingsScreen(
                    hw = hw,
                    diet = diet,
                    theme = theme,
                    notificationsEnabled = notif,
                    language = lang,
                    selectedEquipment = selectedEquipment,
                    mealTime = mealTime,
                    onSaveHardware = viewModel::saveHardwareSettings,
                    onSaveDiet = viewModel::saveDietSettings,
                    onToggleNotifications = viewModel::setNotifications,
                    onSetLanguage = viewModel::setLanguage,
                    onSetTheme = viewModel::setTheme,
                    onEditSetup = viewModel::startEditingSetup
                )
            }
        }
    }
}
