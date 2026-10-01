# Security

## Credential storage

Runtime credentials are stored with Android Keystore AES-GCM:

- The AES key is generated and retained by `AndroidKeyStore` under the application-specific alias.
- Private preferences contain only a versioned payload with the random IV and authenticated ciphertext.
- Blank credentials remove their encrypted entry rather than persisting an empty secret.
- `gemini_api_key` and legacy `hf_api_key` are migrated from ordinary preferences only after encryption and decryption produce the original value.
- Existing secure ciphertext wins over a duplicate plaintext value.
- If Keystore access, encryption, persistence, or verification fails, the legacy plaintext remains for a later retry instead of being silently destroyed.
- After a successful secure round trip, the plaintext keys are removed synchronously.
- Credential ciphertext preferences and diagnostic files are excluded from cloud backup and device transfer.
- `android:allowBackup` remains disabled.

Physical acceptance must still prove the one-time migration on an existing installation without uninstalling, clearing app data, exposing preferences, or printing credential material.

## Logging policy

Release logging is disabled. Debug diagnostics are metadata-only event records.

The logger deliberately discards:

- credential values and credential lengths;
- prompts and AI responses;
- ingredients and pantry contents;
- user questions and recent chat turns;
- image bytes, dimensions, paths, or derived content;
- arbitrary exception messages and stack traces;
- request or response payload lengths;
- absolute file paths and device identifiers.

The on-device debug log file is private, size-limited, excluded from backup/device transfer, and contains only timestamp, severity, bounded component identifier, and a fixed event code.

## Vision privacy and safety

The application does not persist selected vision images to files or SQLDelight.

Ingredient image capture now starts with an explicit disclosure that:

- the chosen image is sent once to the active AI provider for analysis;
- the application does not save the image;
- AI detections may be wrong;
- the user must review results before adding them.

Provider-boundary safeguards:

- remove blank or low-confidence shopping detections;
- fail closed when no sufficiently confident detection remains;
- reject structurally incomplete cooking-photo assessments;
- replace definitive photo-based heat/serving instructions with an explicit manual-confirmation requirement;
- state that a photo cannot verify internal temperature, doneness, or food safety;
- direct the user to verify the food and use a thermometer when appropriate.

These controls are automated and runtime-integrated, but vision remains **experimental** until the final SHA is physically tested with the actual UI and real Gemini provider. Mock, offline, fixture, or static-response evidence does not prove real Gemini behaviour.

## Debug APK signing and physical acceptance

Debug APK signatures are machine-specific unless a shared signing key is deliberately configured. GitHub Actions and the development laptop therefore produce differently signed debug APKs even from identical source.

For the current personal-device workflow:

- do not commit, upload, export, or replace private signing keys;
- do not weaken Android signature verification;
- build the exact final source SHA locally on the original laptop;
- record the resulting APK SHA-256 and source SHA;
- use `adb install -r` only;
- never uninstall or clear application data to bypass a signature mismatch;
- if the locally built APK still does not match the installed certificate, report `BLOCKED`.

A CI artifact proves automated build output. It does not by itself prove that the artifact can safely upgrade the existing phone installation.
