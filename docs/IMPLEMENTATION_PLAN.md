# Implementation Plan

Status terms are strict: **Implemented and integrated** means a production entry point invokes the feature. A class without that call path is **Foundation only**. Automated verification and physical-device verification remain separate evidence classes.

## Current truth

| Area | Status | Evidence / gap |
|---|---|---|
| Core Compose screens and application flow | Implemented and integrated | `MainActivity` creates the injected `AppViewModel`; it drives setup, pantry, recipe options, cooking, consumption, history, and settings screens. Exact-current-SHA physical UI acceptance remains pending. |
| AppContainer and dependency ownership | Implemented and integrated | `AgenticKitchenApp` → `AppContainer` → `AppViewModelFactory` → injected `AppViewModel`; fake-dependency tests cover loading and delegation. |
| Secure credentials | Implemented and integrated | Runtime Gemini and legacy HF credentials use Android Keystore AES-GCM. Legacy plaintext keys migrate only after a verified secure round trip, then are removed. Ciphertext preferences are excluded from backup and device transfer. Prior physical testing exercised the migration path; exact-current-SHA regression acceptance remains separate. |
| Logging policy | Implemented and integrated | Release logging is disabled. Debug diagnostics emit metadata event codes only and discard credentials, prompts, responses, ingredients, questions, image data, exception messages, stack traces, paths, and payload lengths. |
| Typed target time | Implemented and integrated | `RecipeRequestSelection` carries `TargetTimeChoice`; `TargetTimeResolver.resolve` produces the ready time before scheduling and persistence. |
| Structured AI and cooking-plan validation | Implemented and integrated | Runtime recipe-option and cooking-plan requests use typed requests/results and DTOs; `CookingPlanValidator` runs before scheduling, persistence, and display. Automated coverage includes every currently emitted validator error type plus safety warnings. |
| Pantry inventory and consumption | Implemented and integrated | SQLDelight inventory, reservations, planned/actual consumption, cancellation-without-consumption, pending groups, and adjustment records are wired through `AppViewModel`. Restored legacy numeric count units are canonicalized to `adet`/COUNT on read. Automated repository and ViewModel tests pass; final-SHA physical before/after consumption and cancellation evidence remains pending. |
| Cooking-session controller and persistence | Implemented and integrated | `CookingSessionController` and SQLDelight `ActiveCookingSession` persistence are invoked by runtime start/pause/resume/step/end and process-restoration paths. One canonical active session is enforced. Automated recovery tests pass; prior-SHA running/paused recovery was physically exercised and final-SHA regression evidence remains pending. |
| Cooking history | Implemented and integrated | History is exposed in primary navigation with empty/list states and accessible entries. Started/completed/cancelled/ended transitions are persisted and kept live in-session. Prior-SHA history UI was physically exercised; final-SHA regression evidence remains pending. |
| Android resource localization | Foundation only | Persistent English/Turkish selection exists, but `L` and hardcoded Compose text remain; migration to Android resources is incomplete. Known recipe difficulty values are now localized at runtime. |
| Vision privacy and safety | Experimental, runtime-hardened | Ingredient image capture requires explicit disclosure/consent and user review. Provider-boundary policy removes low-confidence detections and prevents cooking photos from producing definitive heat, serving, doneness, or safety decisions without manual confirmation. Real-Gemini fail-closed and pan-safety behaviour were observed on the prior physical SHA; successful detection → review → confirmation and exact-final-SHA acceptance remain pending. |
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
- [x] Correct restored legacy numeric count-unit rendering and normalization.
- [x] Localize known recipe difficulty labels in Turkish UI.
- [x] Clear Assistant input focus after successful send.
- [ ] Complete exact-final-SHA planned/actual pantry consumption with visible before/after quantities and cold-restart persistence.
- [ ] Complete exact-final-SHA cancellation-without-consumption with visible unchanged stock and no stale active session.
- [ ] Complete a successful real-Gemini ingredient detection → review → confirmation flow; fail-closed evidence alone does not prove confirmation.
- [ ] Run a concise exact-final-SHA regression pass for process recovery/history, the three corrected P2 states, real Gemini, and font scale `0.8`.
- [ ] Add notification support only after session behaviour is physically accepted.
- [ ] Complete app-wide final-SHA screenshot/accessibility acceptance at font scale `0.8`.
- [ ] Reconcile every completion claim with a production entry point, automated evidence, and visible physical flow before marking the project physically verified.

## Current physical-verification boundary

A substantial physical pass was performed against source SHA `011c7f6ba0519f180e27fd7dfc943e63139c32fa`. It provides historical physical evidence for real Gemini recipe/assistant behaviour, running/paused process recovery, history, fail-closed ingredient vision, and cooking-photo safety. It also exposed three P2 issues that were subsequently corrected in source: legacy numeric count-unit rendering, Turkish difficulty labels, and Assistant focus retention after send.

The branch head is now newer than that physical pass. Prior-SHA screenshots and device results therefore remain historical evidence and do not establish exact-final-SHA acceptance. The next physical pass must use an APK built from the exact current head and should focus on the remaining unproved flows plus regressions introduced by the narrow fixes above.

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

Passing these commands establishes automated verification only. Installation, UI interaction, real-provider behaviour, pantry consumption/cancellation, vision confirmation, and process-death recovery require exact-final-SHA physical-device evidence.
