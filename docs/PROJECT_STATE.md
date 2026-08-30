# Project State

Last reconciled against PR #1 on 2026-08-30 after Phase 2 deterministic recipe matching/ranking integration.

## Implemented and integrated

- `AgenticKitchenApp` owns one `AppContainer`; `MainActivity` receives an injected `AppViewModel` through `AppViewModelFactory`.
- Typed preferences, the single SQLDelight database, pantry inventory, recipe history, active cooking-session persistence, agents, and provider factory are runtime dependency paths.
- Android-Keystore AES-GCM credential storage is the runtime path for direct Gemini BYOK; verified legacy plaintext values migrate and are removed.
- Typed target-time choices, structured recipe/cooking requests, `AiResult<T>`, DTO parsing, cooking-plan validation, scheduling, and reader-safe errors are in the runtime path.
- Pantry reservation plus planned/actual consumption and cancellation flows are integrated.
- Cooking sessions and History are integrated and have exact-head physical acceptance from the completed Phase 1 closure.
- Managed provider selection is integrated: `FIREBASE` for Firebase AI Logic, `GEMINI` for direct BYOK, and `FREE` for deterministic offline behaviour.
- Managed Firebase requests are classified by task, use Remote Config-backed model names with safe built-in defaults, and provide Firebase SDK response schemas before application decode/validation.
- App Check provider installation is build-type specific: debug provider for debug builds and Play Integrity for release builds.
- Managed Firebase AI Logic physical verification is CLOSED: real managed requests were observed without a personal Gemini key, recipe preparation preserved identity, managed shopping extraction was observed, and Firebase AI Logic showed App Check enforcement with verified traffic.
- Smart Pantry 2.0 is CLOSED and physically VERIFIED: migration-backed pantry metadata, locations, custom location labels, best-before/use-by dates, deterministic freshness states, expiry/name/quantity sorting, location filters, `Use First`, pantry edit controls, in-place migration preservation, and cooking/consumption metadata preservation all passed exact-head device evidence.
- Phase 2 deterministic recipe matching/ranking is integrated. Provider-generated structured candidates are compared and ranked locally; no provider call performs the pantry comparison itself.
- Phase 2 local result groups are `Ready Now`, `Missing 1`, `Missing 2`, and `AI Ideas`.
- Phase 2 ranking accounts for local allergy/diet safety gates, pantry coverage, expiring/use-soon stock, shortage count and prioritized-shortage importance, requested ready time, equipment fit, previous successful recipes, and explicit local ingredient priority.
- Pantry quantities and active reservations participate in deterministic shortage classification.
- Inventory-backed recipe search captures servings and requested ready time before candidate generation and preserves both into recipe detail/preparation.
- Recipe option UI surfaces result grouping, pantry coverage, duration, servings, expiring/use-soon signals, and shortages.
- `AI Ideas` may remain visible in non-strict mode as inspiration, but are not pantry-preparable; both Compose UI and `AppViewModel` fail closed against direct preparation until shortages are resolved.
- Strict-stock mode surfaces only `Ready Now` results.
- Firebase AI Logic and direct Gemini BYOK use one shared pantry-candidate prompt contract: the provider proposes structured candidates, while local code remains authoritative for pantry classification/ranking and the non-strict missing-item allowance is not treated as an upstream generation hard filter.

## Automated verification complete; physical verification pending

- The final Phase 2 guard was validated before commit with `git diff --check`, shared tests, Android unit tests, Android lint, and debug APK assembly; that validation completed successfully.
- Phase 2 has dedicated regression coverage for deterministic tiers, quantity/reservation shortages, expiry priority, requested-ready-time ranking, important shortages, safety/diet fail-closed behaviour, AI-Idea surfacing/preparation policy, equipment/history/priority tie-breaks, shared provider prompt semantics, result-summary grouping, and target-time handoff.
- A normal exact-head Android CI run is still required after the documentation reconciliation commit; automated success does not substitute for physical device verification.
- Phase 2 still requires exact-head in-place physical validation because it changes recipe-discovery behaviour and Android UI.
- Required Phase 2 device checks include non-strict grouping/ranking, strict-stock filtering, target-time/servings handoff, AI-Idea non-preparability, card metadata, allergy/diet smoke, provider regression, and a valid pantry-backed Prepare → Operations regression.
- Ingredient and cooking-photo vision remain safety-sensitive. User review/confirmation and fail-closed safety behaviour remain mandatory.

## Foundation only / incomplete

- Android string-resource localization is incomplete; `L` and hardcoded Compose copy remain in the runtime.
- Cooking notifications / lock-screen controls are not yet integrated.
- Free/Pro entitlements and application-level AI usage metering are product-roadmap work, not current runtime features.

## Product work queued after Phase 2 physical verification

1. pantry-aware structured substitutions;
2. Smart Shopping;
3. multi-photo kitchen scan;
4. recipe import;
5. My Recipes;
6. Home UI refinement;
7. Pantry / Recipe Options / Recipe Detail UI refinement where still outstanding;
8. Cooking Mode polish;
9. receipt-to-pantry;
10. small weekly planner;
11. later hands-free/voice UX.

The detailed feature, UI, AI-cost, and monetization decisions are recorded in `docs/ROADMAP.md`.

## Build matrix

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

Automated green status does not transfer physical VERIFIED status from an older source SHA to a newer source SHA.
