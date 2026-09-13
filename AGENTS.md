# AGENTS.md — Agentic Kitchen Rules

## Status definitions

- **Implemented and integrated:** used by the actual application runtime.
- **Foundation only:** class or contract exists but is not wired into the runtime.
- **Experimental:** runtime integration exists but reliability is not production-grade.
- **Planned:** not implemented.

Do not call a foundation-only feature a working feature.

## Architecture

- `shared/` contains JVM domain logic only; it must not depend on Android.
- `app-android/` contains UI, DI, storage, and Android integrations.
- `AgenticKitchenApp` owns one `AppContainer`; the ViewModel receives dependencies through a factory.
- The ViewModel must not create drivers, databases, providers, HTTP clients, validators, or direct SharedPreferences access.
- All runtime LLM output uses `AiResult<T>`, structured DTOs, centralized parsing, and `CookingPlanValidator` before scheduling, persistence, or display.

## Security

- Credentials use Android Keystore authenticated encryption, never plaintext preferences or SQLDelight.
- Never log credential material or length, prompts, AI responses, ingredients, questions, or image content in release builds.
- Vision images are never persisted or logged and require disclosure and confirmation.
- Keep `android:allowBackup=false`; exclude sensitive blobs and metadata from backup/device transfer.

## Testing and Git

- Run the relevant tests before declaring work done; use `./gradlew :shared:test` and `./gradlew :app-android:testDebugUnitTest` at minimum.
- Use `docs/IMPLEMENTATION_PLAN.md` as the implementation-plan authority and document only verified runtime state.
- Do not force-push, create a second PR, merge PR #1, or mark it ready until the requested review stage.
