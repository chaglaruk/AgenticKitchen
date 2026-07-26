# Project State

Last reconciled against PR #1 branch head `97fdcd2` on 2026-07-26.

## Implemented and integrated

- The Android runtime presents setup, ingredient, options, operations, and settings screens.
- The current `AppViewModel` loads setup data from `agentic_prefs`, creates a SQLDelight database, and reads/writes recipe history.
- Existing provider calls, legacy prompt construction, and legacy recipe parsing are still the active AI path.

## Foundation only

- `AppContainer`, `PreferencesManager`, `DatabaseManager`, and `SecureCredentialStore` are not the dependency path used by `AppViewModel`.
- `AiResult<T>`, structured DTOs, `PromptFactory`, `CookingPlanValidator`, `TargetTimeChoice`, and `TargetTimeResolver` have no production call path.
- Android string resources exist, but the runtime still uses the `L` object and hardcoded display text.

## Experimental

- Vision and free-provider paths are runtime-reachable but have not met the required privacy, confirmation, or reliability criteria.

## Planned

- Keystore-backed credential storage with migration, runtime AI integration and validation, complete localization, cooking sessions, history UI, and notifications.

## Verified build matrix

AGP 8.13.2, Gradle 8.13, Kotlin and Compose compiler plugin 2.3.21, Compose BOM 2026.06.00, JDK 17, `compileSdk` 36, and `targetSdk` 36. The unsupported-SDK suppression was removed. Shared tests and Android compilation/unit-test task passed on this matrix.

## Lint

The old broad lint baseline was removed because it hid 52 warnings and 8 hints, while retaining eight stale entries. The current lint run reports 0 errors, 118 warnings, and 8 hints with no baseline. The warnings are visible and must be fixed or individually documented; this branch does not claim zero warnings.
