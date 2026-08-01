# Implementation Plan

Status terms are strict: **Implemented and integrated** means a production entry point invokes the feature. A class without that call path is **Foundation only**. Automated verification and physical-device verification remain separate evidence classes.

## Current truth

| Area | Status | Evidence / gap |
|---|---|---|
| Core Compose screens and application flow | Implemented and integrated | `MainActivity` creates the injected `AppViewModel`; it drives setup, pantry, recipe options, cooking, consumption, and settings screens. |
| AppContainer and dependency ownership | Implemented and integrated | `AgenticKitchenApp` → `AppContainer` → `AppViewModelFactory` → injected `AppViewModel`; fake-dependency tests cover loading and delegation. |
| Secure credentials | Foundation only | `SecureCredentialStore` is not the credential runtime path; plaintext compatibility remains. |
| Typed target time | Implemented and integrated | `RecipeRequestSelection` carries `TargetTimeChoice`; `TargetTimeResolver.resolve` produces the ready time before scheduling and persistence. |
| Structured AI and cooking-plan validation | Implemented and integrated | Runtime recipe-option and cooking-plan requests use typed requests/results and DTOs; `CookingPlanValidator` runs before scheduling, persistence, and display. |
| Pantry inventory and consumption | Implemented and integrated | SQLDelight inventory, reservations, planned/actual consumption, cancellation, pending groups, and adjustment records are wired through `AppViewModel`. Automated repository and ViewModel tests pass; physical acceptance is pending. |
| Cooking-session controller and persistence | Implemented and integrated | `CookingSessionController` and SQLDelight `ActiveCookingSession` persistence are invoked by the runtime start/pause/resume/step/end and process-restoration paths. Automated recovery tests pass; physical process-death acceptance is pending. |
| Android resource localization | Foundation only | Persistent English/Turkish selection exists, but `L` and hardcoded UI text remain; migration to Android resources is incomplete. |
| Vision privacy flow | Experimental | Runtime integration exists but consent, confidence handling, and fail-closed behaviour are not yet fully hardened or physically accepted. |
| Dedicated history UI and notifications | Planned | Do not infer these features from history storage or cooking-session persistence alone. |

## Ordered work

- [x] Replace the unsupported API-36 build matrix; remove the SDK-warning suppression and document the verified matrix.
- [x] Modernize CI with PR-range whitespace checking, supported Gradle setup, mandatory build stages, reports, and no broad lint baseline.
- [ ] Triage each visible compiler/lint warning: fix it or add a narrow, documented justification.
- [x] Make `AppContainer` the single owner of runtime preferences, database, repositories, agents, providers, and closeable resources; inject `AppViewModel` through a factory.
- [ ] Replace plaintext credential use with Android-Keystore AEAD storage and one-time migration.
- [ ] Define an explicit logging policy; make release logs metadata-only and non-sensitive.
- [x] Wire typed target-time choices from UI through `TargetTimeResolver`; remove string parsing.
- [x] Replace legacy pipe parsing in the recipe runtime with typed provider results, structured parsing, validation, scheduling, and reader-safe UI errors.
- [ ] Tighten validator rules and add coverage for every emitted validation type.
- [ ] Restore all saved setup values while editing and migrate display text to English/Turkish Android resources.
- [ ] Make vision failure and low-confidence results fail closed with explicit consent and confirmation.
- [x] Add persisted cooking-session state and automated recovery coverage.
- [x] Add persistent pantry reservation, planned/actual consumption, cancellation, and automated coverage.
- [ ] Complete final-SHA physical-device acceptance for preserved-data upgrade, pantry consumption/cancellation, and running/paused process recovery.
- [ ] Add a dedicated history UI.
- [ ] Add notification support only after session behaviour is physically accepted.
- [ ] Reconcile every claim with a production entry point, test, and visible user flow before marking it physically verified.

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

Passing these commands establishes automated verification only. Installation, preserved-data upgrade, UI interaction, and process-death behaviour require final-SHA physical-device evidence.
