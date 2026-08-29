package com.agentickitchen.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.agentickitchen.android.app.AgenticKitchenApp
import com.agentickitchen.android.app.AppViewModelFactory
import com.agentickitchen.android.ui.AgenticTheme
import com.agentickitchen.android.ui.ApiKeyOnboardingDialog
import com.agentickitchen.android.ui.EditorialBottomBar
import com.agentickitchen.android.ui.EditorialNavItem
import com.agentickitchen.android.ui.HistoryScreen
import com.agentickitchen.android.ui.HomeScreen
import com.agentickitchen.android.ui.LocalAppColors
import com.agentickitchen.android.ui.OperationsScreen
import com.agentickitchen.android.ui.OptionsScreen
import com.agentickitchen.android.ui.SettingsScreen
import com.agentickitchen.android.ui.SetupScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory.from((application as AgenticKitchenApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsState()
            AgenticTheme(themeName = theme) {
                val colors = LocalAppColors.current
                val isLight = androidx.compose.material.MaterialTheme.colors.isLight
                SideEffect {
                    window.statusBarColor = colors.background.toArgb()
                    window.navigationBarColor = colors.background.toArgb()
                    androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = isLight
                        isAppearanceLightNavigationBars = isLight
                    }
                }
                AppRoot(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    abstract fun title(): String

    data object Intelligence : Screen("intelligence", Icons.Filled.Psychology) {
        override fun title() = if (L.isTr) "Malzemeler" else "Kitchen"
    }

    data object Options : Screen("options", Icons.Filled.RestaurantMenu) {
        override fun title() = if (L.isTr) "Tarifler" else "Recipes"
    }

    data object Operations : Screen("operations", Icons.Filled.PendingActions) {
        override fun title() = if (L.isTr) "Pişir" else "Cook"
    }

    data object History : Screen("history", Icons.Filled.History) {
        override fun title() = if (L.isTr) "Geçmiş" else "History"
    }

    data object Settings : Screen("settings", Icons.Filled.Settings) {
        override fun title() = if (L.isTr) "Ayarlar" else "Settings"
    }
}

internal fun shouldHandleSetupBack(setupDone: Boolean, isEditingSetup: Boolean): Boolean =
    setupDone && isEditingSetup

internal fun backDestination(currentScreen: Screen, hasActiveRecipe: Boolean): Screen = when (currentScreen) {
    Screen.Options, Screen.History, Screen.Settings -> Screen.Intelligence
    Screen.Operations -> if (hasActiveRecipe) Screen.Options else Screen.Intelligence
    Screen.Intelligence -> Screen.Intelligence
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val setupDone by viewModel.setupDone.collectAsState()
    val isEditingSetup by viewModel.isEditingSetup.collectAsState()
    val hw by viewModel.hardwareSettings.collectAsState()
    val selectedEquipment by viewModel.selectedEquipment.collectAsState()
    val aiConnectionStatus by viewModel.aiConnectionStatus.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeySkipped by remember { mutableStateOf(false) }

    val needsApiKey = CookingProviderSelection.needsApiKey(hw)

    LaunchedEffect(setupDone, hw.aiProvider, hw.geminiApiKey, hw.hfApiKey) {
        if (setupDone && needsApiKey && !apiKeySkipped) {
            showApiKeyDialog = true
        }
    }

    if (!setupDone || isEditingSetup) {
        BackHandler(enabled = shouldHandleSetupBack(setupDone, isEditingSetup)) {
            viewModel.cancelEditingSetup()
        }
        SetupScreen(
            initialHw = hw,
            initialEquipment = selectedEquipment,
            canGoBack = setupDone,
            onBack = { viewModel.cancelEditingSetup() },
            onComplete = { equipment, updatedHw ->
                viewModel.completeSetup(equipment, updatedHw)
            }
        )
    } else {
        AppNavigation(viewModel, onConfigureGemini = {
            apiKeySkipped = false
            showApiKeyDialog = true
        })

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
                connectionStatus = aiConnectionStatus,
                onTest = { key -> viewModel.testAiConnection(hw.copy(geminiApiKey = key, aiProvider = CookingProviderSelection.Gemini)) },
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
fun AppNavigation(viewModel: AppViewModel, onConfigureGemini: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Intelligence) }

    val chips by viewModel.chips.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val inventoryAdjustments by viewModel.inventoryAdjustments.collectAsState()
    val shoppingImportState by viewModel.shoppingImportState.collectAsState()
    val pendingConsumption by viewModel.pendingConsumption.collectAsState()
    val planState by viewModel.planState.collectAsState()
    val hw by viewModel.hardwareSettings.collectAsState()
    val aiConnectionStatus by viewModel.aiConnectionStatus.collectAsState()
    val history by viewModel.history.collectAsState()

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
    LaunchedEffect(pendingConsumption?.sessionId) {
        if (pendingConsumption != null) currentScreen = Screen.Operations
    }

    BackHandler(enabled = currentScreen != Screen.Intelligence) {
        currentScreen = backDestination(currentScreen, planState is PlanState.RecipeActive)
    }

    val screens = listOf(
        Screen.Intelligence,
        Screen.Options,
        Screen.Operations,
        Screen.History,
        Screen.Settings
    )
    val colors = LocalAppColors.current
    val scaffoldState = rememberScaffoldState()
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> scaffoldState.snackbarHostState.showSnackbar(event.message)
                is UiEvent.DraftIngredientRemoved -> {
                    val result = scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = if (L.isTr) "Geri al" else "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreRemovedChip(event)
                    }
                }
            }
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = colors.background,
        bottomBar = {
            EditorialBottomBar(screens.map { screen ->
                EditorialNavItem(screen.title(), screen.icon, currentScreen == screen) {
                    currentScreen = screen
                }
            })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
        ) {
            when (currentScreen) {
                Screen.Intelligence -> HomeScreen(
                    chips = chips,
                    inventory = inventory,
                    inventoryAdjustments = inventoryAdjustments,
                    shoppingImportState = shoppingImportState,
                    scannedIngredients = scannedIngredients,
                    pantryIntel = pantryIntel,
                    onScanImage = viewModel::scanIngredients,
                    onClearScannedIngredients = viewModel::clearScannedIngredients,
                    onAddChip = viewModel::addChip,
                    onAddMultipleChips = viewModel::addMultipleChips,
                    onRemoveChip = viewModel::removeChip,
                    onSaveInventoryItem = viewModel::saveInventoryItem,
                    onDeleteInventoryItem = viewModel::deleteInventoryItem,
                    onImportShoppingText = viewModel::importShoppingText,
                    onImportShoppingPhoto = viewModel::importShoppingPhoto,
                    onConfirmShoppingImport = viewModel::confirmShoppingImport,
                    onClearShoppingImport = viewModel::clearShoppingImport,
                    onConfigureGemini = onConfigureGemini,
                    onStartInventorySession = { request ->
                        currentScreen = Screen.Options
                        viewModel.startInventorySession(request)
                    },
                    onClearAll = viewModel::clearAll,
                    onStart = {
                        currentScreen = Screen.Options
                        viewModel.startSession()
                    },
                    onEditSetup = viewModel::startEditingSetup
                )

                Screen.Options -> OptionsScreen(
                    chips = chips,
                    planState = planState,
                    pantryIntel = pantryIntel,
                    onStart = viewModel::startSession,
                    onRefresh = viewModel::refreshSession,
                    onUseOffline = viewModel::useOfflineMode,
                    onSelectOption = { option, selection ->
                        viewModel.selectRecipeOption(option, selection)
                    },
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
                    onBackToOptions = viewModel::backToOptions,
                    cookingState = viewModel.cookingState.collectAsState().value,
                    onStartCooking = viewModel::startCooking,
                    onPauseCooking = viewModel::pauseCooking,
                    onResumeCooking = viewModel::resumeCooking,
                    onCompleteCookingStep = viewModel::completeCookingStep,
                    onSkipCookingStep = viewModel::skipCookingStep,
                    onEndCooking = viewModel::endCooking,
                    pendingConsumption = pendingConsumption,
                    inventory = inventory,
                    onConsumePlanned = viewModel::consumePlannedInventory,
                    onConsumeActual = viewModel::consumeActualInventory,
                    onCancelConsumption = viewModel::cancelInventoryConsumption
                )

                Screen.History -> HistoryScreen(history) { ingredients ->
                    viewModel.reuseHistoryIngredients(ingredients)
                    currentScreen = Screen.Intelligence
                }

                Screen.Settings -> SettingsScreen(
                    hw = hw,
                    diet = diet,
                    theme = theme,
                    language = lang,
                    selectedEquipment = selectedEquipment,
                    aiConnectionStatus = aiConnectionStatus,
                    onSaveHardware = viewModel::saveHardwareSettings,
                    onTestAiConnection = viewModel::testAiConnection,
                    onSaveDiet = viewModel::saveDietSettings,
                    onSetLanguage = viewModel::setLanguage,
                    onSetTheme = viewModel::setTheme,
                    onEditSetup = viewModel::startEditingSetup
                )
            }
        }
    }
}
