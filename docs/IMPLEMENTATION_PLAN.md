# Implementation Plan

Status terms are strict: **Implemented and integrated** means a production entry point invokes the feature. A class without that call path is **Foundation only**. Automated verification and physical-device verification remain separate evidence classes.

## Current truth

| Area | Status | Evidence / gap |
|---|---|---|
| Core Compose screens and application flow | Implemented and integrated | `MainActivity` creates the injected `AppViewModel`; it drives setup, pantry, recipe options, cooking, consumption, history, and settings screens. Exact-current-SHA physical UI acceptance remains pending. |
| AppContainer and dependency ownership | Implemented and integrated | `AgenticKitchenApp` → `AppContainer` → `AppViewModelFactory` → injected `AppViewModel`; fake-dependency tests cover loading and delegation. |
| Secure credentials | Implemented and integrated | Runtime Gemini and legacy HF credentials use Android Keystore AES-GCM. Legacy plaintext keys migrate only after a verified secure round trip, then are removed. Ciphertext preferences are excluded from backup and device transfer. Prior physical testing exercised the migration path; exact-current-SHA regression acceptance remains separate. |
| Logging policy | Implemented and integrated | Release logging is disabled. Debug diagnostics discard credentials, prompts, responses, ingredients, questions, image data, exception messages, stack traces, paths, and payload lengths. Gemini diagnostics retain only whitelisted provider feature, HTTP status, and outcome-category event codes so physical QA can distinguish primary/fallback success and failure without logging user content. |
| Typed target time | Implemented and integrated | `RecipeRequestSelection` carries `TargetTimeChoice`; `TargetTimeResolver.resolve` produces the ready time before scheduling and persistence. |
| Structured AI and cooking-plan validation | Implemented and integrated | Runtime recipe-option and cooking-plan requests use typed requests/results and DTOs; `CookingPlanValidator` runs before scheduling, persistence, and display. Automated coverage includes every currently emitted validator error type plus safety warnings. |
| Pantry inventory and consumption | Implemented and integrated | SQLDelight inventory, reservations, planned/actual consumption, cancellation-without-consumption, pending groups, and adjustment records are wired through `AppViewModel`. Restored legacy numeric count units are canonicalized to `adet`/COUNT on read. Offline inventory recipe options are hydrated with deterministic proposed ingredients. Automated repository/ViewModel/provider tests pass; the uploaded `720b604` physical package did not contain a valid before/action/after evidence chain for actual consumption or cancellation, so exact-final-SHA physical verification remains pending. |
| Cooking-session controller and persistence | Implemented and integrated | `CookingSessionController` and SQLDelight `ActiveCookingSession` persistence are invoked by runtime start/pause/resume/step/end and process-restoration paths. One canonical active session is enforced. Automated recovery tests pass; the uploaded `720b604` package mislabeled the purported running before/after screenshots, so exact-final-SHA running recovery remains pending. |
| Cooking history | Implemented and integrated | History is exposed in primary navigation with empty/list states and accessible entries. Started/completed/cancelled/ended transitions are persisted and kept live in-session. The uploaded `720b604` package independently proves that the History screen is accessible and populated; this does not by itself prove process recovery. |
| Android resource localization | Foundation only | Persistent English/Turkish selection exists, but `L` and hardcoded Compose text remain; migration to Android resources is incomplete. Known recipe difficulty values are now localized at runtime. |
| Vision privacy and safety | Experimental, runtime-hardened | Ingredient image capture requires explicit disclosure/consent and user review. Provider-boundary policy removes low-confidence detections and prevents cooking photos from producing definitive heat, serving, doneness, or safety decisions without manual confirmation. Real-Gemini fail-closed and pan-safety behaviour were observed on a prior physical SHA. The `720b604` physical pass reached an error state rather than candidate review; its evidence did not prove real-provider success. The `generateContent` fallback now uses the documented `responseMimeType` + `responseJsonSchema` structured-output contract, with regression tests. Successful real-Gemini detection → review → reject/confirm → pantry insertion remains pending on the exact final SHA. |
| Notifications | Planned | Notification support remains intentionally deferred until cooking-session behaviour passes final-SHA physical acceptance. |

## Ordered work

- [x] Replace the unsupported API-36 build matrix; remove the SDK-warning suppression and document the verified matrix.
- [x] Modernize CI with PR-range whitespace checking, supported Gradle setup, mandatory build stages, reports, and no broad lint baseline.
- [ ] Triage remaining non-blocking compiler/API deprecation warnings with narrow fixes or documented justification.
- [x] Make `AppContainer` the single owner of runtime preferences, database, repositories, agents, providers, and closeable resources; inject `AppViewModel` through a factory.
- [x] Replace plaintext credential runtime use with Android-Keystore AES-GCM storage and fail-safe one-time migration.
- [x] Define and implement a debug-only metadata logging policy with no sensitive release logging.
- [x] Preserve only whitelisted Gemini provider feature/status/category diagnostic event codes for exact-device QA while continuing to discard user/provider content.
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
- [x] Hydrate offline inventory recipe options with deterministic proposed ingredients so inventory-backed cooking can reach reservation/consumption flows.
- [x] Add a narrow Gemini shopping-photo compatibility fallback and align its raw `generateContent` structured-output request with `responseMimeType` + `responseJsonSchema`.
- [ ] Complete exact-final-SHA planned/actual pantry consumption with correctly labeled visible before/dialog/after/restart evidence and verified arithmetic.
- [ ] Complete exact-final-SHA cancellation-without-consumption with correctly labeled visible before/dialog/after/restart evidence, unchanged stock, and no stale/pending session.
- [ ] Complete a successful real-Gemini ingredient detection → review → reject once → repeat → explicit confirmation → pantry insertion flow; an error/fail-closed state alone does not prove confirmation.
- [ ] Run one concise exact-final-SHA RUNNING force-stop/cold-relaunch recovery check with correctly labeled before/after evidence.
- [ ] Add notification support only after session behaviour is physically accepted.
- [ ] Complete app-wide final-SHA screenshot/accessibility acceptance at font scale `0.8`.
- [ ] Reconcile every completion claim with a production entry point, automated evidence, and visible physical flow before marking the project physically verified.

## Current physical-verification boundary

A substantial historical physical pass was performed against source SHA `011c7f6ba0519f180e27fd7dfc943e63139c32fa`. It provides historical evidence for real Gemini recipe/assistant behaviour, running/paused process recovery, history, fail-closed ingredient vision, and cooking-photo safety. It also exposed three P2 issues that were subsequently corrected in source: legacy numeric count-unit rendering, Turkish difficulty labels, and Assistant focus retention after send. Separate uploaded screenshots later allowed those three P2 UI fixes to be independently inspected and verified.

A targeted physical pass was then attempted on SHA `720b604de6088c4ca113ea58c6dac72921c4a3a3`. Its text report claimed successful actual consumption, cancellation-without-consumption, and running process recovery, but independent inspection of the ZIP rejected those claims because the screenshot labels did not match their contents. Examples include the alleged consumption-dialog file containing Pokémon GO, the alleged consumption baseline already showing post-consumption quantities, actual-entry evidence showing a `502` value inconsistent with the report, and the alleged recovery before/after files showing History and ingredient-selection screens rather than a RUNNING session. The package therefore establishes **no valid physical proof** for actual consumption, cancellation-without-consumption, or RUNNING recovery. It does visibly establish a History-screen smoke.

The same `720b604` pass reached an ingredient-photo error state rather than detected-candidate review, and its sanitized log package did not independently expose sufficient provider metadata to prove a successful real-Gemini call. Source review then identified a compatibility risk in the fallback structured-output body. The fallback now uses the documented raw `generateContent` fields `responseMimeType: application/json` and `responseJsonSchema`, with unit/contract coverage. Debug provider diagnostics now preserve only whitelisted feature/status/category event codes so the next physical pass can prove whether primary Gemini or the fallback succeeded without exposing content.

The branch head is newer than all of those physical passes. The next device pass must therefore use an APK built from the exact current head and test only the still-unproved flows: actual consumption, cancel-without-consuming, real-Gemini reject/confirm, and one RUNNING process-recovery check. Evidence capture must verify the active screen before every screenshot and must not rely on long blind coordinate scripts.

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
