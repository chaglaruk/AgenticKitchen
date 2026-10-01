# Project State

Last reconciled against GitHub source of truth on 2026-09-01. This documentation-only reconciliation describes source-behaviour head `2115893eb5a30dba913ed79bef045527eaac220d`; the reconciliation commit itself does not change APK/runtime behaviour.

## Git / review topology

- Repository: `chaglaruk/AgenticKitchen`.
- Active branch: `refactor/agentic-kitchen-production-foundation`.
- PR #1 remains open, draft, and unmerged against `main`.
- Do not create a second branch or PR for this work.
- Do not merge or mark PR #1 ready for review.
- Do not amend, rebase, squash, reset, or force-push the active branch.
- Physical-device evidence and automated verification remain separate evidence classes. Never transfer VERIFIED physical status from an older source SHA to a newer source SHA.

## Current roadmap checkpoint

### Phase 0 — foundation / production-readiness QA

CLOSED for its previously accepted scope. The architecture, credential handling, structured AI contracts, validator path, persistence foundations, and targeted physical-acceptance work remain part of the inherited runtime.

### Phase 1 — Smart Pantry 2.0

CLOSED and physically VERIFIED for its accepted exact-head evidence set. The runtime includes SQLDelight-backed pantry inventory, metadata, locations, freshness/use-soon state, sorting/filtering, reservation, planned/actual consumption, cancellation, and persistence.

### Phase 2 — deterministic recipe matching / ranking

IMPLEMENTED AND INTEGRATED. Provider-generated structured candidates are ranked locally against pantry quantities/reservations, diet/allergy constraints, equipment, requested ready time, use-soon/expiry signals, shortages, priorities, and successful-history signals. Strict-stock and non-strict result policies fail closed against preparation when required pantry shortages remain.

The Phase 2 physical-device acceptance described in the earlier checkpoint has not been promoted to VERIFIED for later source SHAs.

### Phase 3 — pantry-aware structured substitutions

IMPLEMENTED AND INTEGRATED in the current source tree. Structured substitution contracts, mutation validation, shortage reconciliation, and Operations runtime actions are present. No new physical-device VERIFIED claim is made here.

### Phase 4 — Smart Shopping

IMPLEMENTED AND INTEGRATED; AUTOMATED_ONLY for its dedicated feature checkpoint. Persistent shopping-list storage, deterministic shortage quantities, grouping, recipe linkage, checked state, and substitution-aware reconciliation are present. No physical-device VERIFIED claim is made here.

### Phase 5 — multi-photo kitchen scan

IMPLEMENTED AND INTEGRATED; AUTOMATED_ONLY for its dedicated feature checkpoint. Labelled Fridge / Freezer / Pantry / Counter capture, structured candidates, confidence/uncertainty preservation, review/edit/remove/location correction, and explicit-confirm inventory mutation are present. No physical-camera or managed-vision VERIFIED claim is made here.

### Phase 6 — recipe import

IMPLEMENTED AND INTEGRATED in the current source tree. Runtime entry points support URL, pasted/plain text, recipe photo, and Android text share. Deterministic parsing is preferred where practical; ambiguous extraction can use the selected AI provider. Review compares the recipe with pantry state, preserves uncertainty, validates the source-faithful cooking plan, and blocks cooking while recipe shortages remain.

The dedicated `Phase 6 recipe import wiring` workflow completed successfully on the human-authored staging/repair commit before producing the integrated feature commit. The resulting bot-authored exact source head did not receive a normal Android CI execution: the PR Android CI run ended `action_required` with zero jobs. Therefore source head `2115893eb5a30dba913ed79bef045527eaac220d` must not be described as exact-head Android-CI green.

### Phase 7 — My Recipes

NEXT PRODUCT SLICE. The current tree has recipe history and recipe-import models, but no dedicated saved-recipe SQLDelight table/repository and no `My Recipes` screen/runtime library. Phase 7 should unify useful local recipes without turning the product into a content feed. Eligible sources include imported recipes, explicitly saved AI/offline recipes, manually saved recipes, and successfully cooked history-derived recipes.

Known/successful local recipes should be considered before requesting a new AI generation when they satisfy pantry and safety constraints.

## Runtime invariants that remain mandatory

- `shared/` remains Android-independent domain/data logic.
- `AgenticKitchenApp` owns one `AppContainer`; runtime repositories/providers are injected into `AppViewModel` through `AppViewModelFactory`.
- `AppViewModel` must not directly create database drivers, databases, provider HTTP clients, validators, or direct `SharedPreferences` access.
- Credentials remain Android-Keystore protected; no credential material is stored in SQLDelight or plaintext preferences.
- Release logging must not expose credentials, prompts, provider responses, ingredients, questions, images, or sensitive payload metadata.
- Vision images are not persisted or logged, and inventory mutation requires explicit review/confirmation.
- Runtime LLM output remains structured through `AiResult<T>` / DTO parsing and cooking plans must pass `CookingPlanValidator` before scheduling, persistence, or display.
- Preserve deterministic scheduler behaviour, pantry reservation/consumption semantics, diet/allergy safety, Firebase managed AI, optional Gemini BYOK, offline fallback behaviour, and the existing AgenticKitchen editorial visual identity.

## Verification state at this checkpoint

The source-behaviour head before this documentation-only reconciliation is `2115893eb5a30dba913ed79bef045527eaac220d`.

Confirmed:

- PR #1 is open, draft, and unmerged.
- The active branch points to the source-behaviour head above before this docs-only commit.
- That source tree contains the integrated Phase 3, 4, 5, and 6 runtime code and tests.
- The dedicated Phase 6 recipe-import wiring workflow succeeded.

Not confirmed at that exact source head:

- a normal Android CI run with executed jobs;
- new physical-device acceptance for Phases 2 through 6.

This documentation-only commit is intentionally used to trigger a normal user-authored exact-tree Android CI run before Phase 7 source changes are layered on top.

## Foundation work still incomplete but not silently promoted into Phase 7

- Android string-resource localization remains incomplete; `L` and hardcoded Compose copy are still present.
- Cooking notifications / lock-screen controls remain incomplete.
- App-wide future screenshot/accessibility acceptance remains a separate release gate.
- Free/Pro entitlement and application-level AI metering remain later product/monetization work, not an excuse to add Auth, Firestore, Analytics, or cloud sync to Phase 7.

## Ordered product work from here

1. establish a normal Android-CI green checkpoint for the current source-equivalent tree;
2. implement Phase 7 My Recipes as a local-first saved-recipe library;
3. Phase 8 Home UI refinement;
4. Phase 9 Pantry UI refinement;
5. Phase 10 Recipe Options UI refinement;
6. Phase 11 Recipe Detail / Prepare refinement;
7. Phase 12 Cooking Mode polish;
8. Phase 13 receipt-to-pantry;
9. Phase 14 small meal planner;
10. later advanced / hands-free UX.

See `docs/ROADMAP.md` for product intent and design principles.

## Build matrix and completion gate

AGP 8.13.2, Gradle 8.13, Kotlin and Compose compiler plugin 2.3.21, Compose BOM 2026.06.00, JDK 17, `compileSdk` 36, and `targetSdk` 36.

Repository verification remains:

```bash
./gradlew clean
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
git diff --check origin/main...HEAD
```

Passing these commands establishes automated verification only. Source changes that materially affect device behaviour require appropriately scoped exact-SHA physical evidence before they can inherit a VERIFIED physical status.
