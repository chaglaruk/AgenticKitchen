# Project State — Agentic Kitchen

## Verified Baseline

| Property | Value |
|----------|-------|
| Current main SHA | `db16344` |
| Current branch | `refactor/agentic-kitchen-production-foundation` |
| Last validated commands | `./gradlew :shared:test` (49 passed), `./gradlew :app-android:compileDebugKotlin` |
| Remote | `https://github.com/chaglaruk/AgenticKitchen.git` |

## Working Features

- Multi-provider AI with typed AiResult contract
- Structured RecipeOptionsResponse and CookingPlanResponse DTOs
- Deterministic CookingPlanValidator (19 validation rules, 19 tests)
- TargetTimeResolver with DST/timezone support
- DurationFormatter (EN/TR human-readable)
- SQLDelight recipe history persistence
- Encrypted credential storage (SecureCredentialStore)
- Build-type aware logging (file disabled in release)
- 4-color theme system (Green, Blue, Orange, Dark)
- Turkish / English string resources
- Deterministic ingredient substitution (SimpleIngredientAgent)
- Pantry intel analysis (SimplePantryIntelAgent)
- Backward scheduling (SimpleTimingAgent)

## Experimental Features

- Camera vision ingredient scanning (requires Gemini API key)
- DuckDuckGo provider (SSE-based, experimental)
- Pollinations.ai free provider (rate-limited)

## Blockers

- AGP 8.1.4 not tested with compileSdk 36 (warning suppressed in gradle.properties)
- AppViewModel still contains direct SharedPreferences and SQLDelight driver creation (migration to AppContainer in progress)
- Some strings still hardcoded in L object within AppViewModel (migration to string resources in progress)
- No notification system implemented yet
- No Compose UI tests yet

## Next Actions

1. Complete AppViewModel refactor to use AppContainer exclusively
2. Add notification system with foreground service
3. Add Compose UI tests
4. Complete migration from L object to string resources
5. Add CI badge to README
