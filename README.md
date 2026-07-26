# Agentic Kitchen

An AI-powered smart chef assistant for Android — delivers military-precision cooking instructions tailored to your kitchen hardware, dietary preferences, and available ingredients.

## Product Goal

Zero-initiative cooking: instead of "medium heat", the app tells you exactly "Set burner to level 7 for 4 minutes 30 seconds." The AI handles ingredient substitution analysis, timing orchestration, and visual cooking supervision.

## Implemented

- Multi-provider AI: Gemini, HuggingFace, DuckDuckGo, Pollinations.ai — free tier works without API keys
- Hardware profile: stove type, power levels, oven features, serving size
- Ingredient management: add, remove, scan via camera, categorize
- Recipe options: AI generates 3 alternatives per ingredient set
- Cooking plan: step-by-step instructions with resource allocation
- Deterministic agents: SimpleTimingAgent, SimpleIngredientAgent, SimplePantryIntelAgent
- Target time system: resolve exact time, duration, "this evening" with DST handling
- 4-color theme system: Green, Blue, Orange, Dark
- Turkish / English language support (via Android string resources)
- SQLDelight persistence for cooking history
- Deterministic CookingPlanValidator (15+ rule types)
- Structured AI output contract (AiResult, JSON schemas)
- Encrypted credential storage (EncryptedSharedPreferences)
- Data extraction rules and backup protection
- Build-type aware logging (file logging disabled in release)

## Experimental

- Vision/camera ingredient scanning
- DuckDuckGo AI provider (SSE-based)
- Pollinations.ai free provider

## Planned

- Cooking session runtime with notification-based timers
- History UI with session restore
- Platform-native notification system (foreground service)
- Full screen reader accessibility

## Architecture

```
app-android/src/main/java/com/agentickitchen/android/
├── app/                    # Application + manual DI container
├── presentation/           # ViewModel + UI state
├── feature/setup/          # Feature-specific composables
├── feature/pantry/
├── feature/options/
├── feature/cooking/
├── feature/settings/
├── data/ai/                # AI provider implementations
├── data/preferences/       # SharedPreferences wrapper
├── data/database/          # SQLDelight driver + database
├── data/repository/        # Repository implementations
└── security/               # Encrypted credential storage

shared/src/main/kotlin/com/agentickitchen/shared/
├── agents/                 # Agent interfaces + implementations
├── ai/                     # AiResult, AiProviderId, AiFailureType
│   ├── dto/                # Structured DTOs (RecipeOptionsResponse, CookingPlanResponse)
│   └── prompt/             # PromptFactory
├── models/                 # Domain data classes
├── scheduler/              # TargetTimeResolver, TargetTimeChoice
├── validator/              # CookingPlanValidator
├── db/                     # HistoryRepository
└── util/                   # DurationFormatter
```

## Build Requirements

- Android Studio Ladybug or later
- JDK 17
- Android SDK 34+
- Gradle 8.9 (wrapper included)

## Local Setup

```bash
# Clone
git clone https://github.com/chaglaruk/AgenticKitchen.git

# Open in Android Studio and sync Gradle

# Build debug APK
./gradlew :app-android:assembleDebug

# Run shared module tests
./gradlew :shared:test

# Run Android unit tests
./gradlew :app-android:testDebugUnitTest
```

## External AI Setup

The app defaults to free AI providers (Pollinations.ai, DuckDuckGo) — no API key required to get started.

For improved quality:
1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)
2. Go to Settings → Hardware Profile → AI Provider → Select "Google Gemini"
3. Enter your API key (stored encrypted in Android Keystore)

See `docs/EXTERNAL_SETUP.md` for detailed provider configuration.

## Testing

```bash
# All shared module tests (49 tests)
./gradlew :shared:test

# Android unit tests
./gradlew :app-android:testDebugUnitTest

# Lint
./gradlew :app-android:lintDebug

# Full build
./gradlew build
```

## Security and Privacy

- API keys stored in EncryptedSharedPreferences (AES256-GCM)
- `android:allowBackup=false` with data extraction rules to exclude sensitive data
- Release build: no file logging, no AI prompt/response logging
- Vision images are not persisted or logged
- See `docs/SECURITY.md` for full details

## Known Limitations

- Build shows warnings: AGP 8.1.4 not tested with compileSdk 36 (suppressed)
- Vision provider requires Gemini API key for real analysis
- Free providers (Pollinations, DuckDuckGo) may be rate-limited
- Cooking session runtime with notifications is planned, not yet implemented
- Navigation uses local Compose state, not a navigation library
- Some hardcoded strings remain in L object (migration to string resources in progress)

## License

No license has been selected yet. All rights are reserved until a license is explicitly added.
