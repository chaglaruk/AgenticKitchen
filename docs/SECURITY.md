# Security

## Current state

`android:allowBackup` is disabled and backup-rule files exist. The current credential implementation is **foundation only**: it is not wired into the runtime and uses deprecated AndroidX crypto APIs. The active ViewModel path still reads plaintext legacy credential keys from ordinary preferences.

Release logging is also not yet an explicit, tested metadata-only policy. Do not treat the current implementation as suitable for sensitive production data.

## Required completion criteria

- Android Keystore key plus standard authenticated encryption for credentials.
- Ciphertext, nonce, version, and necessary metadata only in private storage; exclude these from backup and device transfer.
- One-time migration of legacy `gemini_api_key` and `hf_api_key`, followed by secure verification and plaintext removal.
- No credential values or lengths in logs; no sensitive release Logcat, ring buffer, or file logging.
- Vision images never persist or log; external processing is disclosed and detections require confirmation.
