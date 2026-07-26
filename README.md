# Agentic Kitchen

**An AI-powered smart chef assistant for Android** — delivers military-precision cooking instructions tailored to your kitchen hardware, dietary preferences, and available ingredients.

The application takes full initiative: instead of vague terms like "medium heat", it tells you exactly "Set burner to level 7 for 4 minutes." It integrates real-time visual feedback via camera, multi-provider AI (Gemini, HuggingFace, DuckDuckGo, Pollinations.ai), and persistent cooking history via SQLDelight.

## Current Status

Active development (v1.12.3). The project is a functional prototype with:
- Full UI flow (Setup → Home → Options → Active Recipe)
- Real AI integration with multiple providers (free tier works without API keys)
- Camera-based ingredient scanning and cooking supervision
- SQLDelight persistence for cooking history
- Multi-language support (Turkish / English)

## Key Features

- **Zero-Initiative Instructions** — No ambiguous terms like "medium heat"; exact burner levels and durations.
- **Multi-Provider AI** — Gemini, HuggingFace, DuckDuckGo (GPT-4o-mini), Pollinations.ai (Mistral-7B). Free providers work out of the box.
- **Visual Cooking Supervision** — Take a photo mid-cooking; AI inspects doneness and gives corrective instructions.
- **Hardware-Aware Cooking** — Configure your stove type, power levels, oven features, and serving size.
- **Reverse Scheduling** — Set a target meal time; the system back-calculates when to start each step.
- **Ingredient Agent** — Ask if an ingredient can be substituted; the AI enforces flavor profile integrity.
- **4-Color Theme System** — Green, Blue, Orange, and Dark themes with dynamic gradients.
- **SQLDelight History** — Persistent recipe history with structured query support.
- **Offline-First Design** — Falls back gracefully when no API key is configured.

## Technologies Used

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.21 |
| Platform | Android (Min SDK 24, Target SDK 34) |
| UI | Jetpack Compose (Material 2) |
| Architecture | Clean Architecture + MVVM |
| AI SDK | Gemini 1.5 Flash, Ktor-based providers |
| Database | SQLDelight 2.0.0 (SQLite) |
| Networking | Ktor 2.3.5, Kotlinx Serialization |
| Persistence | SharedPreferences |
| Logging | Custom AppLogger (Logcat + file) |
| Build | Gradle 8.9, AGP 8.1.4 |
| KMP | Shared Kotlin Multiplatform module (JVM target) |

## Requirements

- Android Studio (or VS Code with Kotlin plugin)
- JDK 17
- Android SDK 34
- Gradle 8.9 (wrapper included)

No API key is required to start — the app defaults to free AI providers.

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/chaglaruk/AgenticKitchen.git
   ```

2. Open the project in Android Studio.

3. Sync Gradle and let dependencies resolve.

4. Run on an emulator or physical device (API 24+).

## Environment Variables

This project does not require environment variables at build time. API keys are configured at runtime through the app's Settings screen and stored securely in SharedPreferences.

For development reference, a `.env.example` file is provided:

```env
GEMINI_API_KEY=
HUGGINGFACE_API_KEY=
```

These are entirely optional — the app works with free providers (Pollinations.ai, DuckDuckGo) without any API key.

## Running

```bash
# Build debug APK
./gradlew :app-android:assembleDebug

# Run tests (shared module)
./gradlew :shared:test

# Install via ADB
adb install -r app-android/build/outputs/apk/debug/app-android-debug.apk
```

## Build

```bash
# Full build
./gradlew build

# Clean build
./gradlew clean build
```

## Testing

```bash
# Shared module unit tests
./gradlew :shared:test

# Android module tests
./gradlew :app-android:test
```

## Project Structure

```
agentic-kitchen/
├── settings.gradle.kts          # Root project config
├── build.gradle.kts             # Root Gradle build
├── gradle.properties            # Gradle properties
├── gradlew / gradlew.bat        # Gradle wrapper
│
├── shared/                      # KMP shared module (domain + agents)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/agentickitchen/shared/
│       │   ├── agents/          # Orchestrator, TimingAgent, IngredientAgent, etc.
│       │   ├── models/          # Domain data classes
│       │   └── db/              # SQLDelight HistoryRepository
│       ├── main/sqldelight/     # AppDatabase.sq schema
│       └── commonTest/          # Unit tests
│
├── app-android/                 # Android application module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/agentickitchen/android/
│       │   ├── MainActivity.kt
│       │   ├── AppViewModel.kt  # Central ViewModel with all state + AI orchestration
│       │   ├── AppLogger.kt     # Central logging system
│       │   ├── HardwareProfileManager.kt
│       │   ├── ai/              # GeminiProvider, HuggingFaceService, etc.
│       │   ├── vision/          # VisionAgentAndroid
│       │   └── ui/              # Compose screens (Home, Setup, Settings, etc.)
│       └── res/                 # Android resources
│
├── data/                        # Seed data
│   └── ingredients_seed.json
│
├── docs/                        # Architecture and planning docs
│   ├── architecture.md
│   └── superpowers/plans/
│
├── dizayn/                      # UI/UX design documentation
│   ├── analog_heritage/
│   └── zen_precision/
│
├── run_tests.ps1                # Test utility script
├── setup_android_sdk.ps1        # Android SDK setup script
├── CONTINUE.md                  # Comprehensive handoff guide for AI agents
├── AGENTS.md                    # Universal AI coding rules
├── copilot-instructions.md      # GitHub Copilot guidelines
├── .gitignore
└── .env.example                 # Environment variable template
```

## Known Issues / Limitations

- Theme changes may cause contrast issues on some color palettes.
- DuckDuckGo provider (SSE stream) can occasionally truncate long responses.
- Free provider (Pollinations.ai) may be rate-limited; for production use, configure a Gemini API key.
- Build currently shows compile errors in `HomeScreen.kt` and `SettingsScreen.kt` (import/resolution issues) — doğrulanması gerekiyor.
- Android SDK must be installed separately (see `setup_android_sdk.ps1` for automated setup).

## Security Notes

- No API keys or secrets are hardcoded in the source code.
- API keys are stored in Android SharedPreferences (private mode).
- Log files only record API key length, never the key value itself.
- The `.env` file is never committed (excluded via `.gitignore`).
- If you use a custom API key, keep it private and do not share your `SharedPreferences` backup.

## License

No license has been selected yet. All rights are reserved until a license is explicitly added.
