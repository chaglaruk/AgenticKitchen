# Project State

Last reconciled against PR #1 during the managed Firebase AI foundation work on 2026-08-29.

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

## Experimental / physical verification pending

- Managed Firebase AI Logic source integration is automated-testable without a committed Firebase config, but real managed requests and App Check still require exact-head device verification with private local Firebase configuration.
- Ingredient and cooking-photo vision remain safety-sensitive. User review/confirmation and fail-closed safety behaviour remain mandatory.

## Foundation only / incomplete

- Android string-resource localization is incomplete; `L` and hardcoded Compose copy remain in the runtime.
- Cooking notifications / lock-screen controls are not yet integrated.
- Free/Pro entitlements and application-level AI usage metering are product-roadmap work, not current runtime features.

## Product work queued after managed-AI verification

1. expiry/use-soon inventory and pantry locations;
2. deterministic Ready / Missing 1 / Missing 2 recipe matching and ranking;
3. Recipe Options and pantry UI refinement;
4. pantry-aware structured substitutions;
5. Smart Shopping;
6. Home refinement and multi-photo kitchen scan;
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
