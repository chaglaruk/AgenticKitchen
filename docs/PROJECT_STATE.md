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

## Known compatibility blocker

The branch currently combines API 36 with AGP 8.1.4 and suppresses the unsupported-SDK warning. This is not a release-ready build matrix and is the next build-health task.
