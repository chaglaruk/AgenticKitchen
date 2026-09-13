# Implementation Plan

Status terms are strict: **Implemented and integrated** means a production entry point invokes the feature. A class without that call path is **Foundation only**. Automated verification and physical-device verification remain separate evidence classes.

## Current truth

| Area | Status | Evidence / gap |
|---|---|---|
| Core Compose screens and application flow | Implemented and integrated | `MainActivity` creates the injected `AppViewModel`; it drives setup, pantry, recipe options, cooking, consumption, history, and settings screens. The final targeted physical-acceptance set is complete for source-behavior SHA `b5f751b4909299eef33dd575bbfbf6e30a26de8a`. |
| AppContainer and dependency ownership | Implemented and integrated | `AgenticKitchenApp` → `AppContainer` → `AppViewModelFactory` → injected `AppViewModel`; fake-dependency tests cover loading and delegation. |
| Secure credentials | Implemented and integrated | Runtime Gemini and legacy HF credentials use Android Keystore AES-GCM. Legacy plaintext keys migrate only after a verified secure round trip, then are removed. Ciphertext preferences are excluded from backup and device transfer. |
| Logging policy | Implemented and integrated | Release logging is disabled. Debug diagnostics discard credentials, prompts, responses, ingredients, questions, image data, exception messages, stack traces, paths, and payload lengths. Gemini diagnostics retain only whitelisted provider feature, HTTP status, and outcome-category event codes so physical QA can distinguish primary/fallback success and failure without logging user content. |
| Typed target time | Implemented and integrated | `RecipeRequestSelection` carries `TargetTimeChoice`; `TargetTimeResolver.resolve` produces the ready time before scheduling and persistence. |
| Structured AI and cooking-plan validation | Implemented and integrated | Runtime recipe-option and cooking-plan requests use typed requests/results and DTOs; `CookingPlanValidator` runs before scheduling, persistence, and display. Automated coverage includes every currently emitted validator error type plus safety warnings. |
| Pantry inventory and consumption | Implemented and integrated; physically accepted | SQLDelight inventory, reservations, planned/actual consumption, cancellation-without-consumption, pending groups, and adjustment records are wired through `AppViewModel`. Offline inventory recipe options are hydrated with deterministic proposed ingredients. Independent physical evidence proves actual-amount arithmetic and cold-restart persistence, plus cancel-without-consuming with unchanged stock and no stale/pending session. |
| Cooking-session controller and persistence | Implemented and integrated; physically accepted | `CookingSessionController` and SQLDelight `ActiveCookingSession` persistence are invoked by runtime start/pause/resume/step/end and process-restoration paths. Independent physical evidence proves a genuinely RUNNING session survives force-stop/cold relaunch with coherent recipe/step/timer state. |
| Cooking history | Implemented and integrated; physically accepted | History is exposed in primary navigation with empty/list states and accessible entries. Started/completed/cancelled/ended transitions are persisted and kept live in-session. Physical evidence proves the History screen is accessible and populated. |
| Android resource localization | Foundation only | Persistent English/Turkish selection exists, but `L` and hardcoded Compose text remain; migration to Android resources is incomplete. Known recipe difficulty values are localized at runtime. |
| Vision privacy and safety | Experimental, runtime-hardened; ingredient-photo path physically accepted | Ingredient image capture requires explicit disclosure/consent and user review. Provider-boundary policy removes low-confidence detections and prevents cooking photos from producing definitive heat, serving, doneness, or safety decisions without manual confirmation. The primary Interactions endpoint uses `/v1beta/interactions`; the narrow `generateContent` fallback remains limited to primary `InvalidResponse`. Independent physical evidence proves two real Gemini `SHOPPING_PHOTO_200_SUCCESS` calls, candidate review, reject-without-insertion, repeat, explicit confirmation, and pantry insertion. |
| Operations idle-state rendering | Implemented and physically accepted | `READY`, `COMPLETED`, and `ENDED` cooking panels render only with an active `RecipeActive` plan; `RUNNING`, `PAUSED`, and `ERROR` retain visibility without plan context. Final physical evidence on `b5f751b...` proves the true idle screen contains only idle copy and no stale recipe/terminal/READY panel text. |
| Notifications | Planned | Notification support was intentionally deferred until cooking-session physical acceptance; that gate is now closed, so notification work may proceed next. |

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
- [x] Use the current Gemini Interactions `/v1beta/interactions` endpoint and pin it with a contract test.
- [x] Complete planned/actual pantry consumption physical acceptance with visible before/dialog/actual/after/restart evidence and verified arithmetic.
- [x] Complete cancellation-without-consumption physical acceptance with unchanged stock after action and cold restart, plus no stale/pending session.
- [x] Complete successful real-Gemini ingredient detection → review → reject once → repeat → explicit confirmation → pantry insertion.
- [x] Complete one concise RUNNING force-stop/cold-relaunch recovery proof.
- [x] Correct Operations idle rendering so stale READY/terminal panels cannot coexist with the empty idle state, and physically verify the final state.
- [ ] Add notification support now that cooking-session physical acceptance is complete.
- [ ] Complete app-wide screenshot/accessibility acceptance at font scale `0.8` for any future broad UI release gate; do not reinterpret this as reopening the targeted acceptance set already closed above.
- [x] Reconcile the targeted completion claims with production entry points, automated evidence, and visible physical evidence before closing the physical-acceptance gate.

## Physical-verification boundary

A substantial historical physical pass was performed against source SHA `011c7f6ba0519f180e27fd7dfc943e63139c32fa`. It supplied historical evidence for real Gemini recipe/assistant behaviour, process recovery, history, fail-closed ingredient vision, and cooking-photo safety, and exposed several later-fixed P2 issues.

A later `720b604de6088c4ca113ea58c6dac72921c4a3a3` evidence package was independently rejected for several claimed flows because screenshot labels did not match their contents. That package is retained only as historical evidence of why operator text reports are not accepted without raw evidence.

The subsequent `8b1c63d32b0dba4246acea49cceb54e0e8c783c3` package independently established:

- actual pantry consumption arithmetic and cold-restart persistence;
- cancel-without-consuming with unchanged stock and no stale/pending session;
- RUNNING process recovery after force-stop/cold relaunch;
- History accessibility.

The `1c0cb4dc440d1702c0ff986547933ad96b5c9aff` package independently established the real Gemini ingredient-photo flow using two primary `SHOPPING_PHOTO_200_SUCCESS` calls: one candidate review was rejected with pantry unchanged, then a second review was explicitly confirmed and the candidate appeared in pantry. That same package exposed a remaining Operations idle/terminal double-render defect.

The defect was fixed by commit `b5f751b4909299eef33dd575bbfbf6e30a26de8a`. A final narrow exact-SHA evidence package, `AgenticKitchen_b5f7_idle_evidence.zip`, was independently inspected. Its screenshot and identical pre/post UIAutomator XML show package `com.agentickitchen.android`, visible idle text `Henüz pişirilen bir tarif yok.`, and absence of stale recipe/terminal/READY text including `Şimdi pişiriyoruz`, `Afiyet olsun.`, `Pişirme adımları tamamlandı.`, `Tarif hazır.`, `Adımları başlatmaya hazırsın.`, and `Pişirmeye Başla`.

Therefore the **targeted physical-acceptance gate is closed for source-behavior SHA `b5f751b4909299eef33dd575bbfbf6e30a26de8a`**. The subsequent documentation-only commits do not alter APK/source behaviour. Their CI is tracked in PR metadata rather than in this document to avoid creating a self-referential documentation/CI commit loop.

## Verification required before source changes are considered complete

```bash
./gradlew clean
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
git diff --check origin/main...HEAD
```

Passing these commands establishes automated verification only. Future source changes that materially affect device behaviour must receive appropriately scoped physical evidence before inheriting a VERIFIED physical status.
