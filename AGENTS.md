# AGENTS.md — Agentic Kitchen AI Agent Rules

## Context

Agentic Kitchen is an **Android (Kotlin/Jetpack Compose) smart chef assistant** with:
- Kotlin shared domain module (JVM, NOT Kotlin Multiplatform)
- Gradle 8.9, AGP 8.1.4, Kotlin 1.9.21, compileSdk 36
- Structured AI output (JSON DTOs), not raw pipe-delimited text
- Deterministic plan validation before UI display
- Encrypted credential storage (EncryptedSharedPreferences)
- Build-type aware logging (file/content logging disabled in release)

## Critical Architecture Rules

### Domain vs Presentation
- `shared/` = domain logic ONLY: agents, models, AI contracts, validators, resolvers
- `app-android/` = Android-specific: UI, DI container, data layer, security
- ViewModel must NOT create SQLDelight driver, provider instances, or access SharedPreferences directly
- Domain models must NOT depend on Android SDK
- UI models (DTOs) are separate from domain models

### AI Output
- ALL AI output must use `AiResult<T>` sealed interface (Success/Failure)
- ALL AI DTOs are defined in `shared/.../ai/dto/` with `@Serializable` schemas
- No raw `|` pipe-delimited parsing
- LLM output must pass through `CookingPlanValidator` before use
- Prompt strings live in `PromptFactory`, not scattered across ViewModel

### Security
- API keys: use `SecureCredentialStore` (EncryptedSharedPreferences), NEVER plaintext SharedPreferences
- Never log credential values, key lengths, or user prompts in release builds
- Vision images: never persist to disk, never log, destroy after use
- `android:allowBackup=false` — sensitive data excluded from backup

### Testing
- Validator tests: 15+ cases covering all error types
- Shared tests run with `./gradlew :shared:test`
- Android tests with `./gradlew :app-android:testDebugUnitTest`
- Do NOT mark "done" without running tests first

### Git
- No force push (`--force` or `--force-with-lease`)
- Current implementation plan authority: `docs/IMPLEMENTATION_PLAN.md`
- Documentation must reflect actual code state, not aspirations

## Commands

```bash
# Build + test
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:compileDebugKotlin
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
git diff --check
git status --short
```
