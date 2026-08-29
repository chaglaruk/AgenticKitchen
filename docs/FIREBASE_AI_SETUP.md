# Firebase AI Logic setup

AgenticKitchen supports a managed Gemini path through Firebase AI Logic while keeping direct Gemini BYOK and fully offline operation available.

## Scope

The managed path intentionally uses only:

- Firebase Core configuration required by the Android app;
- Firebase AI Logic with the Gemini Developer API backend;
- Firebase App Check;
- Debug App Check provider for local debug builds;
- Play Integrity App Check provider for release builds.

Do not add Firebase Auth, Firestore, Analytics, cloud sync, Storage, Remote Config, or unrelated backend services without a separate product decision.

## Android app registration

1. Create or select the Firebase project for AgenticKitchen.
2. Register Android application ID `com.agentickitchen.android`.
3. Download a fresh `google-services.json` from Firebase.
4. Place it locally at `app-android/google-services.json`.
5. Never commit this file. It is gitignored.

CI deliberately builds without `google-services.json`; the Google Services Gradle plugin is applied only when the local file exists. With no Firebase configuration, selecting the managed provider falls back safely to the existing offline provider.

## Firebase AI Logic

Use the Firebase console guided setup for Firebase AI Logic and select the **Gemini Developer API** backend. The Android client uses stable model:

`gemini-3.7-flash`

No user Gemini API key is required for the managed path. Direct Gemini BYOK remains a separate advanced provider.

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

Firebase AI Logic has a default per-user generate-content quota of 100 requests/minute. That is higher than AgenticKitchen needs. Lower it to a conservative initial value such as **10 requests/minute/user** in Google Cloud quotas, then adjust only from real usage evidence.

Gemini provider quotas are project-level and shared by all users/apps attached to the project. A 429 can mean project quota exhaustion or temporary model capacity pressure.

Before enabling billing, review current Firebase AI Logic and Gemini Developer API pricing/data-governance terms. Do not assume free-tier and paid-tier data use terms are identical.

## Privacy and logging

- Do not log prompts, responses, images, App Check tokens, or user Gemini keys by default.
- Do not embed a privileged Gemini secret in the APK.
- Firebase configuration identifiers in `google-services.json` are not a substitute for App Check.
- Preserve the local/offline path when managed AI is unavailable.

## Provider behavior

- `FIREBASE`: managed Firebase AI Logic. If Firebase is not locally configured, use the offline provider rather than crash.
- `GEMINI`: direct Gemini Developer API using the user's stored Gemini API key.
- `FREE`: deterministic offline provider.

Existing saved `GEMINI` and `FREE` choices remain valid. Fresh installs default to `FIREBASE`.

## Local verification

After repository CI is green, perform local device verification without changing source:

1. fast-forward to the exact audited SHA;
2. add the local noncommitted `app-android/google-services.json`;
3. build/install in place without clearing app data;
4. register the local App Check debug token privately;
5. select/test Firebase AI;
6. verify a real managed recipe-options request and prepared-plan flow;
7. verify no user API key is requested or exposed;
8. confirm App Check-enforced calls work;
9. test BYOK/offline fallback only if needed.
