# Architecture

## Current runtime

`MainActivity` obtains `AppViewModel` through the default Android factory. The ViewModel currently owns ordinary preferences, a SQLDelight driver/database, legacy providers, prompt construction, legacy parsing, and target-time string parsing. This violates the intended ownership boundary and is being removed.

The shared module is a Kotlin/JVM domain module, not Kotlin Multiplatform.

## Foundation classes not yet integrated

`AgenticKitchenApp` creates `AppContainer`; however, `MainActivity` does not use that container to construct the ViewModel. `PreferencesManager`, `DatabaseManager`, `SecureCredentialStore`, `TargetTimeResolver`, `AiResult`, structured DTOs, `PromptFactory`, and `CookingPlanValidator` therefore have no current production call path.

## Required target ownership

`AgenticKitchenApp` must own one `AppContainer`; the container must provide the database driver/database, preferences, Keystore credential store, repositories, resolver, validators, provider registry, orchestrator, and closeable application resources. An `AppViewModelFactory` must pass only those dependencies to the ViewModel.

## Build compatibility target

The current API-36/AGP-8.1.4 combination is unsupported and must not be released. The matrix will be recorded here only after each selected version is compiled and tested. JDK 17 remains required.
