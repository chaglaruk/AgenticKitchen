# Google Play automation

AgenticKitchen uses Gradle Play Publisher (GPP) for repeatable Google Play listing and release automation, plus the official Android Publisher REST endpoint for Data Safety.

## Compatibility decision

The project is currently on Android Gradle Plugin 8.13.2. GPP 4.x requires AGP 9, so this branch intentionally pins GPP 3.13.0.

## Credentials

Never commit a Google service-account JSON key.

Recommended local workflow:

```powershell
.\scripts\play\set-play-credentials.ps1 -Path "C:\secure\agentickitchen-play-service-account.json"
```

This loads the JSON into `ANDROID_PUBLISHER_CREDENTIALS` for the current PowerShell process without printing the private key. GPP reads that environment variable directly.

The service account must be invited in Google Play Console and given only the permissions AgenticKitchen actually needs. After the connection is verified, do not leave broad Google Cloud Project Owner permissions on the account.

## First artifact limitation

Google Play requires the first APK/AAB for a newly created app to be uploaded through Play Console. GPP can manage subsequent artifacts and metadata after that initial registration step.

## Store listing

Metadata is source-controlled under:

`app-android/src/main/play/`

Publish it with:

```powershell
.\scripts\play\publish-listing.ps1 -Execute
```

Do not run `bootstrapListing` casually: GPP documents that bootstrapping resets an existing `play` metadata folder.

## Internal App Bundle

The Gradle configuration defaults to App Bundles, the `internal` track, and `DRAFT` release status to avoid accidental public rollout.

After the first manual artifact has registered the app and release signing is configured:

```powershell
.\scripts\play\publish-internal-bundle.ps1 -Execute
```

## Data Safety

Publish an exported/reviewed Play Console CSV through Google's official `applications.dataSafety` endpoint:

```powershell
.\scripts\play\publish-data-safety.ps1 -CsvPath "C:\path\data_safety_agentickitchen_filled.csv" -Execute
```

The script uses `gcloud auth application-default print-access-token` with the Android Publisher OAuth scope. If `ANDROID_PUBLISHER_CREDENTIALS` is loaded, the script creates a temporary credential file only for the token request and deletes it in `finally`.

Keep the reviewed CSV outside the repository until its answers have been revalidated against the exact release SDK/data-flow state. Once final, it can be source-controlled deliberately.

## Intentionally manual in Play Console

Some app-content declarations do not have equivalent Android Publisher API endpoints and remain manual, including target audience, content rating and several policy questionnaires.

## Release discipline

- Keep package name `com.agentickitchen.android`.
- Increment `versionCode` for every uploaded artifact.
- Store release notes only for functionality appropriate to that build's verification status.
- Re-review Data Safety whenever SDK or data-flow behavior changes.
- Never publish roadmap-only or `AUTOMATED_ONLY` behavior as physically verified production functionality.
