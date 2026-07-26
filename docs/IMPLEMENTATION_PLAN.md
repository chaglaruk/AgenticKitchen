# Implementation Plan

Status terms are strict: **Implemented and integrated** means a production entry point invokes the feature. A class without that call path is **Foundation only**.

## Current truth

| Area | Status | Evidence / gap |
|---|---|---|
| Core Compose screens and legacy flow | Implemented and integrated | `MainActivity` creates `AppViewModel`; it drives the screens. |
| AppContainer and dependency ownership | Foundation only | `AppViewModel` still creates preferences and SQLDelight directly. |
| Secure credentials | Foundation only | `SecureCredentialStore` is unused; plaintext credential keys remain in the ViewModel path. |
| Typed target time | Foundation only | UI passes a `String`; `buildTargetIso` parses it. |
| Structured AI and validation | Foundation only | legacy providers/parsers remain the runtime path. |
| Android resource localization | Foundation only | `L` and hardcoded UI text remain in use. |
| Vision privacy flow | Experimental | runtime path exists but is not yet fail-closed and confirmation-safe. |
| Cooking sessions, session persistence, history UI, notifications | Planned | no runtime controller or UI exists. |

## Ordered work

- [ ] Replace the unsupported API-36 build matrix; remove the SDK-warning suppression and document the verified matrix.
- [ ] Modernize CI with PR-range whitespace checking, supported Gradle setup, mandatory build stages, reports, and a reviewed lint baseline.
- [ ] Make `AppContainer` the single owner of preferences, database, providers, validators, and closeable resources; inject `AppViewModel` through a factory.
- [ ] Replace plaintext credential use with Android-Keystore AEAD storage and one-time migration.
- [ ] Define an explicit logging policy; make release logs metadata-only and non-sensitive.
- [ ] Wire typed target-time choices from UI through `TargetTimeResolver`; remove string parsing.
- [ ] Replace legacy pipe parsing with typed provider results, structured parsing, validation, scheduling, and UI errors.
- [ ] Tighten validator rules and add coverage for every emitted validation type.
- [ ] Restore all saved setup values while editing and migrate display text to English/Turkish Android resources with persistent app-language selection.
- [ ] Make vision failure and low-confidence results fail closed with consent and confirmation.
- [ ] Add persisted cooking-session state, history UI, then notification support.
- [ ] Reconcile every claim with a production entry point, test, and visible user flow before marking it integrated.

## Verification required before completion

```bash
./gradlew clean
./gradlew :shared:test
./gradlew :app-android:testDebugUnitTest
./gradlew :app-android:lintDebug
./gradlew :app-android:assembleDebug
./gradlew build
git diff --check origin/main...HEAD
```
