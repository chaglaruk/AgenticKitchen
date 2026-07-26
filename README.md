# Agentic Kitchen

Android smart-chef assistant built with Kotlin and Jetpack Compose.

## Status definitions

- **Implemented and integrated**: invoked by the installed application runtime.
- **Foundation only**: contract or class exists but the runtime does not use it.
- **Experimental**: invoked at runtime but not production-grade.
- **Planned**: not implemented.

## Current runtime state

### Implemented and integrated

- Ingredient entry, pantry analysis, setup/preferences, themes, and local Compose navigation.
- Legacy AI-provider calls and legacy recipe-option/cooking-plan flow.
- SQLDelight recipe-history reads and writes from the current ViewModel.

### Experimental

- Camera ingredient analysis: provider reliability and user-confirmation handling are incomplete.
- Free AI providers: availability and output reliability are not guaranteed.

### Foundation only

- `AppContainer`, `PreferencesManager`, and `DatabaseManager`: the ViewModel still creates its own preferences and database.
- `SecureCredentialStore`: credentials are still loaded from ordinary preferences; the current implementation also relies on deprecated AndroidX crypto APIs.
- `AiResult<T>`, provider architecture, structured DTOs, `PromptFactory`, and `CookingPlanValidator`: classes exist, but the runtime still uses legacy provider and parsing paths.
- `TargetTimeChoice` and `TargetTimeResolver`: classes and unit tests exist, but option selection still passes a string and the ViewModel parses it.
- Android string-resource localization: resources exist, but user-visible text still uses the `L` object and hardcoded UI text.

### Planned

- Persistent cooking-session runtime, history UI, and timer notifications.
- Keystore-backed credential encryption and plaintext migration.
- Complete resource localization and per-app language switching.

## Build requirements

The current branch is transitional: `compileSdk` and `targetSdk` are 36, but AGP 8.1.4 is not a supported API-36 pairing and the warning is suppressed. Do not treat the current matrix as release-ready. JDK 17 is required.

## Local verification

```bash
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Project state](docs/PROJECT_STATE.md)
- [Security](docs/SECURITY.md)
- [External setup](docs/EXTERNAL_SETUP.md)
- [Product](docs/PRODUCT.md)
- [Roadmap](docs/ROADMAP.md)

No production-food-safety certification is claimed. Do not use the experimental AI or vision output without review.
