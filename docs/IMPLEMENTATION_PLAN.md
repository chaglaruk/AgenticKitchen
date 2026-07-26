# Implementation Plan — Agentic Kitchen Production Refactor

## Commit Structure

```
chore: establish verified build and repository baseline
fix: correct scheduling setup and localization flows
refactor: separate presentation domain data and ai layers
feat: add structured ai contracts and plan validation
feat: add cooking session execution and persistence
security: protect credentials logs backups and vision data
docs: align project state architecture and roadmap
```

---

## Phase 1: Verified Baseline

### 1.1 Repository State
- [x] Working tree clean at `db16344`
- [x] Remote `origin` = `https://github.com/chaglaruk/AgenticKitchen.git`
- [x] Branch: `refactor/agentic-kitchen-production-foundation`
- [ ] Verify no open PRs or conflicting branches

### 1.2 Actual Technology Stack
- [x] `shared` module uses `kotlin("jvm")` — NOT Kotlin Multiplatform
- [x] Android app module targets JVM via `project(":shared")`
- [x] No KMP plugin in any build file
- [x] `commonMain` directory name is misleading

## Phase 2: Repository and Build Health

### 2.1 Fix KMP Claims
- [ ] Update README: remove "KMP", "KMM", "Kotlin Multiplatform" claims
- [ ] Update CONTINUE.md: correct platform description
- [ ] Update docs/architecture.md: correct module type
- [ ] Move `shared/src/commonMain/kotlin` → `shared/src/main/kotlin`
- [ ] Move `shared/src/commonTest/kotlin` → `shared/src/test/kotlin`
- [ ] Update `shared/build.gradle.kts` source dirs

### 2.2 Version Consolidation
- [x] Current `versionName = "0.1.0"` in app-android/build.gradle.kts
- [ ] Use `BuildConfig.VERSION_NAME` in Settings UI
- [ ] Remove hardcoded version strings

### 2.3 App Name
- [ ] Change `AndroidManifest.xml` label from `"ChefGPT"` to `"Agentic Kitchen"`
- [ ] Use string resource for app name

### 2.4 SDK Compatibility
- [ ] Check available SDK platforms
- [ ] Update `compileSdk` and `targetSdk`
- [ ] Verify AGP/Kotlin/Gradle compatibility
- [ ] Verify Compose compiler compatibility

### 2.5 GitHub Actions CI
- [ ] Create `.github/workflows/android-ci.yml`
- [ ] Add: shared test, android unit test, lint, assembleDebug
- [ ] Add: Gradle wrapper validation, JDK 17, SDK setup, cache

**Verification:**
```bash
./gradlew :shared:test
./gradlew :app-android:assembleDebug
```

## Phase 3: Functional Bug Fixes

### 3.1 Target Time System
- [ ] Create `TargetTimeChoice` sealed interface
- [ ] Create `TargetTimeResolver`
- [ ] Handle: immediate, 20min, 45min, 1hr, evening, exact time
- [ ] Handle: past time → next day, DST, invalid input
- [ ] Unit tests for all cases

### 3.2 Setup Persistence
- [ ] Load saved values when opening Setup screen
- [ ] Don't overwrite with hardcoded defaults
- [ ] Separate first-time setup from edit mode
- [ ] Compose UI test for edit state

### 3.3 Localization
- [ ] Create `res/values/strings.xml`
- [ ] Create `res/values-tr/strings.xml`
- [ ] Move hardcoded Turkish/English from `L` object to resources
- [ ] Language switch actually restarts activity
- [ ] Remove unsupported languages from picker
- [ ] Remove placeholder CalibrationScreen and PlanView

### 3.4 Duration Formatting
- [ ] Create `DurationFormatter` utility
- [ ] Format: "45 sn", "5 dk", "1 sa 10 dk"
- [ ] Unit tests

### 3.5 Notification Toggle
- [ ] If notification system is incomplete, remove toggle from UI
- [ ] Mark in README as "Planned"

## Phase 4: Architecture Refactor

### 4.1 AppContainer
- [ ] Create `AgenticKitchenApp` Application subclass
- [ ] Create `AppContainer` for manual DI
- [ ] Move: SQLDelight driver, database, repositories
- [ ] Move: AI provider instances, HTTP clients
- [ ] Move: SharedPreferences wrapper

### 4.2 Package Restructure
```
app-android/src/main/java/com/agentickitchen/android/
├── app/
│   ├── AgenticKitchenApp.kt
│   └── AppContainer.kt
├── presentation/
│   ├── AppViewModel.kt (reduced)
│   ├── AppUiState.kt
│   └── AppAction.kt
├── feature/
│   ├── setup/
│   ├── pantry/
│   ├── options/
│   ├── cooking/
│   └── settings/
├── data/
│   ├── ai/
│   ├── preferences/
│   ├── database/
│   └── repository/
└── security/
```

### 4.3 Shared/Domain Module
```
shared/
├── model/
├── scheduler/
├── validator/
├── repository/
└── usecase/
```

### 4.4 ViewModel Cleanup
- [ ] Remove direct SQLDelight driver creation
- [ ] Remove direct provider creation
- [ ] Remove direct SharedPreferences access
- [ ] Remove long prompt strings
- [ ] Remove date/time parsing
- [ ] Move all to appropriate layers
- [ ] Each intermediate step must compile

## Phase 5: AI Provider Architecture

### 5.1 Typed Result Contract
- [ ] Create `AiResult<T>` sealed interface
- [ ] Define `AiFailureType` enum
- [ ] Implement in all providers

### 5.2 Production Provider
- [ ] Evaluate Gemini SDK currency
- [ ] If outdated, upgrade to current Google AI client
- [ ] Set Gemini as production provider
- [ ] Mark DuckDuckGo and Pollinations as experimental/debug-only
- [ ] Remove "GPT-4o-mini" and "Mistral" unverified claims

### 5.3 Provider Lifecycle
- [ ] Timeout per provider
- [ ] HTTP status checking
- [ ] Cancellation support
- [ ] Response size limits
- [ ] Retry policy
- [ ] Rate limit handling
- [ ] Client lifecycle management

## Phase 6: Structured AI Output

### 6.1 Recipe Options Schema
- [ ] Create `RecipeOptionsResponse` DTO
- [ ] Create `RecipeOptionDto` schema
- [ ] Remove pipe-delimited parsing
- [ ] Add JSON schema validation

### 6.2 Cooking Plan Schema
- [ ] Create `CookingPlanResponse` DTO
- [ ] Create `CookingStepDto` schema
- [ ] Remove raw string parsing

### 6.3 Prompt Factory
- [ ] Create `PromptFactory`
- [ ] `recipeOptionsPrompt()`
- [ ] `cookingPlanPrompt()`
- [ ] `substitutionPrompt()`
- [ ] `visionAssessmentPrompt()`
- [ ] Include: language, servings, equipment, diet, allergies, JSON schema

### 6.4 AI Output Validation
- [ ] Validate JSON structure
- [ ] Parse and check fields
- [ ] Fallback for malformed responses

## Phase 7: Deterministic Safety Validation

### 7.1 CookingPlanValidator
- [ ] Create `CookingPlanValidator`
- [ ] Equipment validation (unknown resource, unavailable equipment)
- [ ] Timing validation (unique IDs, valid dependencies, no cycles, sane durations)
- [ ] Diet/allergen validation
- [ ] Quantity/portion validation
- [ ] Safety warnings (raw meat, excessive temp, missing time)

### 7.2 UI Integration
- [ ] Show validation failures in UI
- [ ] Clear user messages for each failure type

### 7.3 Validator Unit Tests
- [ ] 15+ test cases covering all validations

## Phase 8: Ingredient Agent Enhancement

### 8.1 Integration
- [ ] Wire `SimpleIngredientAgent` into substitution flow
- [ ] Create `AllergenId` set model

### 8.2 Flow
- [ ] Resolve original/candidate ingredients
- [ ] Diet and allergen check
- [ ] Technical substitution rules
- [ ] LLM explanation (not authority)
- [ ] User-facing response

## Phase 9: Vision Safety

### 9.1 Typed Results
- [ ] Create `VisionAnalysisResult` sealed interface
- [ ] `Success`, `LowConfidence`, `Failure` variants
- [ ] Confidence tracking

### 9.2 Remove Text Fallback
- [ ] Remove: "HF failed → text model generates typical ingredients"
- [ ] Return proper error to user

### 9.3 Privacy
- [ ] Consent disclosure for external API calls
- [ ] Bitmap lifecycle management
- [ ] No logging of images
- [ ] No persistent image storage

## Phase 10: Cooking Session Runtime

### 10.1 CookingSessionController
- [ ] Create `CookingSessionState` data class
- [ ] Session actions: start, complete step, pause, resume, skip, delay
- [ ] Backward scheduling recalculation on delay
- [ ] Process recreation recovery

### 10.2 Operations UI
- [ ] Active step display
- [ ] Next steps
- [ ] Countdown timer
- [ ] Concurrent steps
- [ ] Delay reporting

### 10.3 Notification System
- [ ] Foreground service or notification channel
- [ ] Step timer notifications
- [ ] Pause/resume/complete actions
- [ ] `AppClock` abstraction
- [ ] Permission handling per SDK version

## Phase 11: Persistence

### 11.1 Schema Expansion
- [ ] Add tables: `CookingSession`, `CookingStepState`, `RecipePlan`
- [ ] Migration from old schema
- [ ] Preserve existing data

### 11.2 History Screen
- [ ] Completed recipes
- [ ] Abandoned recipes
- [ ] Date, name, status
- [ ] Re-open action
- [ ] Delete action

## Phase 12: Security and Privacy

### 12.1 Credential Storage
- [ ] Use `EncryptedSharedPreferences` for API keys
- [ ] Never log credentials
- [ ] Never show plaintext in UI
- [ ] Credential deletion support
- [ ] Provider-switch credential cleanup

### 12.2 Backup Protection
- [ ] `android:allowBackup="false"` or backup rules
- [ ] `dataExtractionRules` to exclude shared prefs
- [ ] Exclude: API keys, logs, images, session data

### 12.3 Logging Separation
- [ ] Debug: technical errors, provider name, model, request ID, duration
- [ ] Release: NO prompts, NO responses, NO ingredient lists, NO key lengths, NO image metadata
- [ ] File logger disabled in release

## Phase 13: Testing

### 13.1 Unit Tests
- [ ] TargetTimeResolver (6+ tests)
- [ ] AiResult parsing (6+ tests)
- [ ] CookingPlanValidator (15+ tests)
- [ ] CookingSessionController (8+ tests)
- [ ] DurationFormatter (4+ tests)
- [ ] Provider error handling (6+ tests)
- [ ] SQLDelight persistence (4+ tests)
- [ ] Setup screen state (2+ tests)

### 13.2 Compose UI Tests
- [ ] Setup edit state
- [ ] Option selection
- [ ] Cooking session controls
- [ ] Language switch
- [ ] Error state display

## Phase 14: Documentation

### 14.1 README
- [ ] Overview, Product Goal, Implemented, Experimental, Planned
- [ ] Architecture, Build Requirements, Local Setup
- [ ] External AI Setup, Testing, Security and Privacy
- [ ] Known Limitations, Roadmap, License

### 14.2 Doc Files
- [ ] `docs/PRODUCT.md`
- [ ] `docs/ARCHITECTURE.md`
- [ ] `docs/PROJECT_STATE.md`
- [ ] `docs/ROADMAP.md`
- [ ] `docs/SECURITY.md`
- [ ] `docs/EXTERNAL_SETUP.md`

### 14.3 Agent Instructions
- [ ] Update `AGENTS.md` for Android/Kotlin project
- [ ] Move `copilot-instructions.md` to `.github/copilot-instructions.md`

### 14.4 CONTINUE.md
- [ ] Update or archive to `docs/archive/CONTINUE.md`

## Phase 15: Final Validation

### 15.1 Build Commands
```bash
./gradlew clean
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
git diff --check
git status --short
```

### 15.2 APK Verification
- [ ] APK exists at build/outputs/apk/debug/

### 15.3 Smoke Test (if device available)
- [ ] Initial setup
- [ ] Ingredient input
- [ ] Recipe options generation
- [ ] Target time selection
- [ ] Cooking session start
- [ ] Step complete
- [ ] Pause/resume
- [ ] Session restore after process kill
- [ ] Settings modification
- [ ] Language switch
- [ ] Vision failure behavior
- [ ] API credential deletion

## Phase 16: Git and PR

### 16.1 Commits
- [ ] Sequential, readable commits per phase
- [ ] Each commit compiles and tests pass

### 16.2 Draft PR
- [ ] Title: `refactor: rebuild Agentic Kitchen production foundation`
- [ ] PR description with all sections
- [ ] No auto-merge
- [ ] No force push

---

## Progress Tracking

| Phase | Status | Verified |
|-------|--------|----------|
| 1. Verified Baseline | ✅ | ✅ |
| 2. Build Health | 🔲 | 🔲 |
| 3. Bug Fixes | 🔲 | 🔲 |
| 4. Architecture | 🔲 | 🔲 |
| 5-6. AI Architecture | 🔲 | 🔲 |
| 7. Validation | 🔲 | 🔲 |
| 8. Ingredient Agent | 🔲 | 🔲 |
| 9. Vision Safety | 🔲 | 🔲 |
| 10. Cooking Runtime | 🔲 | 🔲 |
| 11. Persistence | 🔲 | 🔲 |
| 12. Security | 🔲 | 🔲 |
| 13. Testing | 🔲 | 🔲 |
| 14. Documentation | 🔲 | 🔲 |
| 15. Final Validation | 🔲 | 🔲 |
| 16. Git/PR | 🔲 | 🔲 |
