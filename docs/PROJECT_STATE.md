# Project State

Last reconciled against PR #1 on 2026-08-30 during Smart Pantry 2.0 work.

## Implemented and integrated

- `AgenticKitchenApp` owns one `AppContainer`; `MainActivity` receives an injected `AppViewModel` through `AppViewModelFactory`.
- Typed preferences, the single SQLDelight database, pantry inventory, recipe history, active cooking-session persistence, agents, and provider factory are runtime dependency paths.
- Android-Keystore AES-GCM credential storage is the runtime path for direct Gemini BYOK; verified legacy plaintext values migrate and are removed.
- Typed target-time choices, structured recipe/cooking requests, `AiResult<T>`, DTO parsing, cooking-plan validation, scheduling, and reader-safe errors are in the runtime path.
- Pantry reservation plus planned/actual consumption and cancellation flows are integrated.
- Cooking sessions and History are integrated and have historical physical acceptance on earlier exact source SHAs.
- Managed provider selection is integrated: `FIREBASE` for Firebase AI Logic, `GEMINI` for direct BYOK, and `FREE` for deterministic offline behaviour.
- Managed Firebase requests are classified by task, use Remote Config-backed model names with safe built-in defaults, and provide Firebase SDK response schemas before application decode/validation.
- App Check provider installation is build-type specific: debug provider for debug builds and Play Integrity for release builds.
- Managed Firebase AI Logic exact-head physical verification is CLOSED: real managed requests were observed without a personal Gemini key, recipe preparation preserved identity, managed shopping extraction was observed, and Firebase AI Logic showed App Check enforcement with verified traffic.
- Smart Pantry 2.0 is now integrated in source: SQLDelight migration-backed pantry metadata, locations, custom location label, best-before/use-by dates, deterministic freshness states, expiry/name/quantity sorting, location filters, a `Use First` view, and dedicated pantry edit controls.
- Phase 1 metadata is stored alongside the existing pantry model without replacing the established reservation/consumption path.

## Automated verification complete; physical verification pending

- Exact-head Phase 1 repository automation is green through whitespace check, clean, shared tests, Android unit tests, lint, debug APK assembly, full build, and artifact upload.
- Smart Pantry 2.0 still requires exact-head in-place physical validation because it changes both persistent database schema and Android UI.
- Required Phase 1 device checks: upgrade existing app data without uninstall/clear-data, confirm migration preserves existing stock, create/edit location and expiry metadata, verify `Use First` and filters/sorts, and run a regression cooking/consumption smoke on the same exact-head APK.
- Ingredient and cooking-photo vision remain safety-sensitive. User review/confirmation and fail-closed safety behaviour remain mandatory.

## Foundation only / incomplete

- Android string-resource localization is incomplete; `L` and hardcoded Compose copy remain in the runtime.
- Cooking notifications / lock-screen controls are not yet integrated.
- Free/Pro entitlements and application-level AI usage metering are product-roadmap work, not current runtime features.

## Product work queued after Phase 1 physical verification

1. deterministic Ready / Missing 1 / Missing 2 recipe matching and ranking;
2. Recipe Options UI refinement;
3. pantry-aware structured substitutions;
4. Smart Shopping;
5. Home refinement;
6. multi-photo kitchen scan;
7. recipe import and My Recipes;
8. Recipe Detail and Cooking Mode polish;
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
