# Firebase AI Logic setup

AgenticKitchen supports a managed Gemini path through Firebase AI Logic while keeping direct Gemini BYOK and fully offline operation available.

## Scope

The managed path intentionally uses only:

- Firebase Core configuration required by the Android app;
- Firebase AI Logic with the Gemini Developer API backend;
- Firebase App Check;
- Debug App Check provider for local debug builds;
- Play Integrity App Check provider for release builds;
- Firebase Remote Config for non-secret managed-AI model selection.

Do not add Firebase Auth, Firestore, Analytics, cloud sync, Storage, or unrelated backend services without a separate product decision. Remote Config must not contain credentials, user data, prompts, responses, or other secrets.

## Android app registration

1. Create or select the Firebase project for AgenticKitchen.
2. Register Android application ID `com.agentickitchen.android`.
3. Download a fresh `google-services.json` from Firebase.
4. Place it locally at `app-android/google-services.json`.
5. Never commit this file. It is gitignored.

CI deliberately builds without `google-services.json`; the Google Services Gradle plugin is applied only when the local file exists. With no Firebase configuration, selecting the managed provider falls back safely to the existing offline provider.

## Managed model routing

Firebase AI requests are classified by task rather than sending every operation to one model.

In-app defaults:

- extraction/parsing: `gemini-3.5-flash-lite`;
- recipe reasoning, cooking plans, and cooking chat: `gemini-3.7-flash`;
- cooking-photo judgement: `gemini-3.7-flash`.

Remote Config keys:

- `firebase_ai_model_extraction`
- `firebase_ai_model_reasoning`
- `firebase_ai_model_vision`

The app ships safe defaults, accepts only bounded Gemini-style model names from Remote Config, and falls back to its built-in default when a value is blank or invalid. The client requests `fetchAndActivate()` with a one-hour minimum fetch interval. A failed fetch does not block AI startup because the last activated value or built-in default remains usable.

Model names are deliberately not hard-coded into individual feature call sites. This allows a stable model to be replaced without an APK release while preserving task-level cost and quality control.

## Structured output

Managed Firebase requests use `application/json` together with an SDK `responseSchema` for every structured response family:

- recipe options;
- cooking plans;
- shopping text/photo extraction;
- cooking-photo inspection;
- cooking assistant replies;
- connection tests.

The existing kotlinx.serialization decode and semantic validation remain in place after schema-constrained generation. The schema is therefore a generation constraint, not a replacement for application validation or food-safety checks.

## App Check

App Check is part of the managed-AI security boundary and must stay enforced for Firebase AI Logic.

### Debug builds

Debug builds use `DebugAppCheckProviderFactory`. After installing/running a locally configured debug build, obtain the App Check debug token from the local Android logs and register it in Firebase Console > App Check > Apps > Manage debug tokens.

Treat the debug token as a secret operational value:

- never commit it;
- never paste it into chat, issue/PR text, screenshots, QA XML, or evidence archives;
- revoke it if exposed.

### Release builds

Release builds use `PlayIntegrityAppCheckProviderFactory`. Configure the Android app in App Check for Play Integrity before a production release. Verify the Play-distributed signing/package configuration rather than assuming a sideloaded debug build proves production attestation.

Firebase requires App Check enforcement for Firebase AI Logic starting November 2, 2026. Keep enforcement enabled before that deadline as well.

## Quota and cost guardrails

Firebase AI Logic currently has a default per-user generate-content quota of 100 requests/minute. That is higher than AgenticKitchen needs. Lower it to a conservative initial value such as **10 requests/minute/user** in Google Cloud quotas, then adjust only from real usage evidence.

Gemini provider quotas are project-level and shared by all users/apps attached to the project. A 429 can mean project quota exhaustion or temporary model capacity pressure.

Before enabling billing, review current Firebase AI Logic and Gemini Developer API pricing/data-governance terms. Do not assume free-tier and paid-tier data use terms are identical.

The product roadmap also requires application-level AI usage metering before commercial launch so Free/Pro entitlements can be enforced independently from the selected provider.

## Privacy and logging

- Do not log prompts, responses, images, App Check tokens, Remote Config payloads containing anything sensitive, or user Gemini keys by default.
- Do not embed a privileged Gemini secret in the APK.
- Firebase configuration identifiers in `google-services.json` are not a substitute for App Check.
- Preserve the local/offline path when managed AI is unavailable.

## Provider behavior

- `FIREBASE`: managed Firebase AI Logic. If Firebase is not locally configured, use the offline provider rather than crash.
- `GEMINI`: direct Gemini Developer API using the user's Keystore-backed Gemini API key.
- `FREE`: deterministic offline provider.

Existing saved `GEMINI` and `FREE` choices remain valid. Fresh installs default to `FIREBASE`.

## Commercial boundary

Provider choice and paid entitlement are separate concepts.

- Firebase is the recommended no-key managed provider for ordinary users.
- BYOK is an advanced provider option and does not automatically grant future Pro product features.
- Commercial launch is planned around Free + Pro subscription, not banner/interstitial advertising.
- If subscription entitlement cannot be verified safely on-device, add only the minimal Google Play entitlement-verification backend required for purchase validation. Do not expand that into accounts, pantry sync, analytics, or unrelated cloud infrastructure.

## Local verification

After repository CI is green, perform local device verification without changing source:

1. fast-forward to the exact audited SHA;
2. add the local noncommitted `app-android/google-services.json`;
3. build/install in place without clearing app data;
4. register the local App Check debug token privately;
5. select/test Firebase AI;
6. verify a real managed recipe-options request and prepared-plan flow;
7. verify a real extraction path uses managed AI without asking for a user key;
8. verify no user API key, prompt, response, image, App Check token, or device identifier is exposed in evidence;
9. confirm App Check-enforced calls work;
10. confirm BYOK and offline choices still remain available;
11. record unsupported or unsafe-to-test items as `PARTIAL` or `NOT_ATTEMPTED`, never as verified.
