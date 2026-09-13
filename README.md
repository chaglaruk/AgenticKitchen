# Agentic Kitchen

Android smart-chef assistant built with Kotlin and Jetpack Compose.

## Status definitions

- **Implemented and integrated**: invoked by the installed application runtime.
- **Foundation only**: contract or class exists but the runtime does not use it.
- **Experimental**: invoked at runtime but still requires production or physical validation.
- **Planned**: not implemented.

## Current runtime state

### Implemented and integrated

- Ingredient entry, persistent SQLDelight pantry inventory, pantry-aware recipe requests, setup/preferences, themes, and local Compose navigation.
- Pantry reservations plus planned/actual consumption and cancellation flows.
- Typed recipe options and cooking plans with structured parsing, deterministic validation, target-time resolution, and scheduling.
- Persistent cooking sessions with start/pause/resume/complete/skip/end and recovery support.
- Cooking history and recipe reuse paths.
- App-owned dependency injection: `AgenticKitchenApp` → `AppContainer` → `AppViewModelFactory` → `AppViewModel`.
- Android-Keystore AES-GCM credential storage with legacy plaintext migration for direct Gemini BYOK.
- Provider selection with managed Firebase AI Logic, direct Gemini BYOK, and deterministic offline fallback.
- Managed Firebase requests use task-aware model routing, Remote Config model selection, App Check integration, and SDK response schemas plus application validation.
- Managed Firebase AI Logic + App Check exact-head physical verification is closed: managed requests work without a personal Gemini key and Firebase AI Logic is enforced by App Check.
- Smart Pantry 2.0 foundation is integrated: pantry locations, custom location labels, best-before/use-by metadata, derived freshness states, local expiry/name/quantity sorting, location filtering, a `Use First` selection, and dedicated pantry controls while preserving the existing SQLDelight inventory and cooking-consumption path.

### Automated verification complete; physical verification pending

- Smart Pantry 2.0 exact-head repository automation is green, including shared tests, Android unit tests, lint, debug APK assembly, and full build.
- Because Phase 1 changes persisted database state and Android UI, exact-head in-place device verification is still required before the new pantry migration/UI is labelled physically VERIFIED.
- Camera ingredient and cooking-photo paths remain safety-sensitive and require explicit review/confirmation; vision output is not treated as authoritative food-safety or doneness evidence.

### Foundation only / incomplete

- Android string-resource localization remains incomplete; user-visible text still includes `L`-based and hardcoded Compose copy.
- Notification lifecycle and lock-screen cooking controls are not yet integrated.

### Planned

The next roadmap priorities are:

- deterministic Ready / Missing 1 / Missing 2 recipe matching and ranking;
- pantry-aware structured substitutions and Smart Shopping;
- multi-photo kitchen scanning;
- recipe import and My Recipes;
- targeted Home, Recipe Options, Recipe Detail, and Cooking Mode UI refinement;
- receipt-to-pantry and a deliberately small weekly meal planner;
- later hands-free/voice features after core reliability is proven.

See [Roadmap](docs/ROADMAP.md) for execution order, AI cost controls, UI principles, and the Free + Pro monetization direction.

## Build requirements

Verified matrix: AGP 8.13.2, Gradle 8.13, Kotlin and Compose compiler plugin 2.3.21, Compose BOM 2026.06.00, JDK 17, `compileSdk` 36, and `targetSdk` 36.

## Local verification

```bash
./gradlew clean
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
```

Passing repository automation establishes automated verification only. Source changes that affect device behaviour require appropriately scoped exact-head physical evidence before inheriting a physical VERIFIED status.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Implementation plan and verification boundary](docs/IMPLEMENTATION_PLAN.md)
- [Firebase AI setup](docs/FIREBASE_AI_SETUP.md)
- [Project state](docs/PROJECT_STATE.md)
- [Security](docs/SECURITY.md)
- [External setup](docs/EXTERNAL_SETUP.md)
- [Product](docs/PRODUCT.md)
- [Roadmap](docs/ROADMAP.md)

No production-food-safety certification is claimed. AI and vision output must remain subject to application validation and user judgement.
