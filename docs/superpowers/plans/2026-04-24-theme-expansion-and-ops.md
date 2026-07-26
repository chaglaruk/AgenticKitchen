# Theme Expansion And Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand Agentic Kitchen into a 4-tab experience that uses the two Stitch design directions, adds a third original theme, and ships new operational intelligence features.

**Architecture:** Keep the shared module responsible for pure domain intelligence and move new UI behavior into theme-aware Compose screens. Split intake, options, operations, and configuration into clearer navigation surfaces while preserving the existing ViewModel as the orchestration hub.

**Tech Stack:** Kotlin, Jetpack Compose Material 2, Kotlin shared module tests, SharedPreferences-backed Android state.

---

### Task 1: Pantry Intel Domain

**Files:**
- Create: `shared/src/commonMain/kotlin/com/agentickitchen/shared/agents/PantryIntelAgent.kt`
- Create: `shared/src/commonMain/kotlin/com/agentickitchen/shared/agents/SimplePantryIntelAgent.kt`
- Create: `shared/src/commonMain/kotlin/com/agentickitchen/shared/models/PantryIntelReport.kt`
- Create: `shared/src/commonTest/kotlin/com/agentickitchen/shared/agents/SimplePantryIntelAgentTest.kt`

- [ ] **Step 1: Write the failing test**
- [ ] **Step 2: Run `.\gradlew.bat :shared:test --tests com.agentickitchen.shared.agents.SimplePantryIntelAgentTest` and verify failure**
- [ ] **Step 3: Implement the minimal pantry analysis code**
- [ ] **Step 4: Re-run the target test and verify pass**

### Task 2: Theme System Expansion

**Files:**
- Modify: `app-android/src/main/java/com/agentickitchen/android/ui/Theme.kt`

- [ ] **Step 1: Add theme metadata and the third original palette**
- [ ] **Step 2: Add theme-specific gradients and typography helpers**
- [ ] **Step 3: Wire theme selection IDs so existing settings persistence remains compatible**

### Task 3: Multi-Screen Compose Refresh

**Files:**
- Modify: `app-android/src/main/java/com/agentickitchen/android/MainActivity.kt`
- Modify: `app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt`
- Modify: `app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt`
- Create: `app-android/src/main/java/com/agentickitchen/android/ui/OptionsScreen.kt`
- Create: `app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt`
- Modify: `app-android/src/main/java/com/agentickitchen/android/ui/SettingsScreen.kt`

- [ ] **Step 1: Add `PantryIntelReport` state to `AppViewModel`**
- [ ] **Step 2: Split navigation into Intelligence, Options, Operations, Configuration**
- [ ] **Step 3: Rebuild the Intelligence screen with Heritage, Zen, and the new original theme variants**
- [ ] **Step 4: Move recipe options and active mission views into dedicated screens**
- [ ] **Step 5: Add theme gallery and operational insight cards**

### Task 4: Verification, Build, And Handoff

**Files:**
- Modify: `CONTINUE.md`

- [ ] **Step 1: Run shared tests**
- [ ] **Step 2: Run `.\gradlew.bat :app-android:assembleDebug`**
- [ ] **Step 3: Fix any build issues before making success claims**
- [ ] **Step 4: Update `CONTINUE.md` with the new themes, screens, and features**
- [ ] **Step 5: Install and launch the debug APK on the connected device**
