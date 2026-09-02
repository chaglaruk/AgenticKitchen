# Architecture

## Current runtime

`AgenticKitchenApp` creates one `AppContainer`. `MainActivity` creates `AppViewModel` through `AppViewModelFactory.from(container)`. The ViewModel receives typed preferences, history repository, orchestration, pantry intelligence, provider factory, and resolver dependencies; it does not create Android storage, SQLDelight, agents, or providers.

The shared module is a Kotlin/JVM domain module, not Kotlin Multiplatform.

## Implemented runtime ownership

`AppContainer` owns the Android preference implementation, one SQLDelight driver/database, the history repository, deterministic agents/orchestrator, `TargetTimeResolver`, legacy provider factory, and credential-store foundation. Provider and vision clients are cached by configuration and closed through the container's testable `close()` method; production keeps the container for application lifetime. `DatabaseManager` was removed as a duplicate owner.

`SecureCredentialStore`, `AiResult`, structured DTOs, `PromptFactory`, and `CookingPlanValidator` remain foundation-only because the legacy runtime does not invoke them. `TargetTimeResolver` is container-provided but the typed target-time UI is still foundation-only.

## Required target ownership

`AgenticKitchenApp` must own one `AppContainer`; the container must provide the database driver/database, preferences, Keystore credential store, repositories, resolver, validators, provider registry, orchestrator, and closeable application resources. An `AppViewModelFactory` must pass only those dependencies to the ViewModel.

## Verified build matrix

AGP 8.13.2, Gradle 8.13, Kotlin and Compose compiler plugin 2.3.21, Compose BOM 2026.06.00, JDK 17, `compileSdk` 36, and `targetSdk` 36. Shared tests and Android compilation/unit-test task passed after the change. The unsupported-SDK suppression was removed.
