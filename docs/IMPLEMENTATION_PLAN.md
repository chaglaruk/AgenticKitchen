# Implementation Plan

Status terms are strict: **Implemented and integrated** means a production entry point invokes the feature. A class without that call path is **Foundation only**. Automated verification and physical-device verification remain separate evidence classes.

## Current truth

| Area | Status | Evidence / gap |
|---|---|---|
| Core Compose screens and application flow | Implemented and integrated | `MainActivity` creates the injected `AppViewModel`; it drives setup, pantry, recipe options, cooking, consumption, history, and settings screens. Physical UI acceptance remains pending for the current SHA. |
| AppContainer and dependency ownership | Implemented and integrated | `AgenticKitchenApp` → `AppContainer` → `AppViewModelFactory` → injected `AppViewModel`; fake-dependency tests cover loading and delegation. |
| Secure credentials | Implemented and integrated; physical migration pending | Runtime Gemini and legacy HF credentials use Android Keystore AES-GCM. Legacy plaintext keys migrate only after a verified secure round trip, then are removed. Ciphertext preferences are excluded from backup and device transfer. Existing-device migration still requires final-SHA physical acceptance. |
| Logging policy | Implemented and integrated | Release logging is disabled. Debug diagnostics emit metadata event codes only and discard credentials, prompts, responses, ingredients, questions, image data, exception messages, stack traces, paths, and payload lengths. |
| Typed target time | Implemented and integrated | `RecipeRequestSelection` carries `TargetTimeChoice`; `TargetTimeResolver.resolve` produces the ready time before scheduling and persistence. |
| Structured AI and cooking-plan validation | Implemented and integrated | Runtime recipe-option and cooking-plan requests use typed requests/results and DTOs; `CookingPlanValidator` runs before scheduling, persistence, and display. Automated coverage includes every currently emitted validator error type plus safety warnings. |
| Pantry inventory and consumption | Implemented and integrated | SQLDelight inventory, reservations, planned/actual consumption, cancellation, pending groups, and adjustment records are wired through `AppViewModel`. Automated repository and ViewModel tests pass; physical acceptance is pending. |
| Cooking-session controller and persistence | Implemented and integrated | `CookingSessionController` and SQLDelight `ActiveCookingSession` persistence are invoked by runtime start/pause/resume/step/end and process-restoration paths. One canonical active session is enforced. Automated recovery tests pass; physical process-death acceptance is pending. |
| Cooking history | Implemented and integrated | History is exposed in primary navigation with empty/list states and accessible entries. Started/completed/cancelled/ended transitions are persisted and kept live in-session. Physical UI acceptance is pending. |
| Android resource localization | Foundation only | Persistent English/Turkish selection exists, but `L` and hardcoded Compose text remain; migration to Android resources is incomplete. |
| Vision privacy and safety | Experimental, runtime-hardened | Ingredient image capture now requires explicit disclosure/consent and user review. Provider-boundary policy removes low-confidence detections and prevents cooking photos from producing definitive heat, serving, doneness, or safety decisions without manual confirmation. Physical UI and real-Gemini acceptance remain pending. |
| Notifications | Planned | Notification support remains intentionally deferred until cooking-session behaviour passes final-SHA physical acceptance. |

## Ordered work

- [x] Replace the unsupported API-36 build matrix; remove the SDK-warning suppression and document the verified matrix.
- [x] Modernize CI with PR-range whitespace checking, supported Gradle setup, mandatory build stages, reports, and no broad lint baseline.
- [ ] Triage remaining non-blocking compiler/API deprecation warnings with narrow fixes or documented justification.
- [x] Make `AppContainer` the single owner of runtime preferences, database, repositories, agents, providers, and closeable resources; inject `AppViewModel` through a factory.
- [x] Replace plaintext credential runtime use with Android-Keystore AES-GCM storage and fail-safe one-time migration.
- [x] Define and implement a debug-only metadata logging policy with no sensitive release logging.
- [x] Wire typed target-time choices from UI through `TargetTimeResolver`; remove string parsing.
- [x] Replace legacy pipe parsing in the recipe runtime with typed provider results, structured parsing, validation, scheduling, and reader-safe UI errors.
- [x] Add automated coverage for every currently emitted validator error type and safety warning.
- [ ] Migrate remaining display text to English/Turkish Android resources and remove the global `L`/hardcoded-text dependency.
- [x] Add explicit ingredient-image consent, result confirmation, low-confidence rejection, and fail-closed cooking-photo policy.
- [x] Add persisted cooking-session state and automated recovery coverage.
- [x] Add persistent pantry reservation, planned/actual consumption, cancellation, and automated coverage.
- [x] Add a dedicated cooking-history screen and lifecycle status tracking.
- [ ] Complete final-SHA physical-device acceptance for preserved-data upgrade, Keystore migration, pantry consumption/cancellation, running/paused process recovery, history, and vision consent/safety UI.
- [ ] Add notification support only after session behaviour is physically accepted.
- [ ] Run a real-Gemini final-SHA acceptance pass; mocks, offline providers, fixtures, and static responses do not prove this item.
- [ ] Complete an app-wide final-SHA screenshot and accessibility review at font scale `0.8`.
- [ ] Reconcile every completion claim with a production entry point, automated evidence, and visible physical flow before marking the project physically verified.

## Current physical-installation blocker

The GitHub Actions debug APK cannot update the existing phone installation because CI and the laptop use different debug signing keys. Android correctly rejects that APK with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

The safe acceptance path is:

1. Check out the exact final source SHA on the original laptop.
2. Build `:app-android:assembleDebug` locally so the APK uses the same local debug key as the installed app.
3. Record the local APK SHA-256; it is expected to differ from the CI artifact because signing bytes differ.
4. Install only with `adb install -r`, without uninstalling or clearing data.
5. If certificate equality still fails, remain blocked; never commit, export, replace, or bypass private signing keys.

## Verification required before completion

```bash
./gradlew clean
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
git diff --check origin/main...HEAD
```

Passing these commands establishes automated verification only. Installation, preserved-data upgrade, credential migration, UI interaction, real-provider behaviour, and process-death recovery require final-SHA physical-device evidence.
