# Google Play automation

AgenticKitchen uses Gradle Play Publisher (GPP) for repeatable Google Play listing and release automation, plus the official Android Publisher REST endpoint for Data Safety.

## One-command local update + setup

From any up-to-date checkout of the AgenticKitchen repository, run:

```powershell
.\scripts\play\update-and-setup.ps1
```

The helper refuses to touch a dirty working tree, fetches `origin`, switches to `refactor/agentic-kitchen-production-foundation`, fast-forwards only, prints the resulting exact HEAD, and then runs the Google Cloud / Play publisher setup. It never rebases, resets, force-pushes, or discards local work.

## Compatibility decision

The project is currently on Android Gradle Plugin 8.13.2. GPP 4.x requires AGP 9, so this branch intentionally pins GPP 3.13.0.

## Authentication: keyless ADC

The preferred workflow does not create or store a long-lived Google service-account JSON key. GPP is configured to use Google Application Default Credentials (ADC), and local development should impersonate a narrowly-permissioned service account.

With the normal AgenticKitchen local setup, run:

```powershell
.\scripts\play\setup-google-cloud.ps1
```

The script reads the existing Firebase/Google Cloud project ID locally from the git-ignored `app-android/google-services.json`, enables `androidpublisher.googleapis.com`, creates the `agentickitchen-play-publisher` service account when needed, and grants the currently authenticated gcloud user permission to impersonate it. The project ID does not need to be copied into chat or source control.

If `google-services.json` is unavailable, pass an existing project explicitly:

```powershell
.\scripts\play\setup-google-cloud.ps1 -ProjectId "YOUR_GCP_PROJECT_ID"
```

A separate project can also be deliberately created with `-CreateProject`.

Google no longer requires a Play developer account to be linked to the Google Cloud project used for Android Publisher API access.

## Required Play Console permission step

The Cloud service account still has to be invited in Play Console under `Settings > Users and permissions` and granted access to Agentic Kitchen.

For the current pre-production workflow grant only:

- Manage store presence
- Release apps to testing tracks
- Manage testing tracks and edit tester lists
- Manage policy related pages

Do not grant production-release or financial permissions at this stage.

After the Play Console invitation is active, create local ADC by impersonating the service account:

```powershell
gcloud auth application-default login --impersonate-service-account=SERVICE_ACCOUNT_EMAIL
```

No private service-account key needs to be downloaded.

## First artifact limitation

Google Play requires the first APK/AAB for a newly created app to be uploaded through Play Console. GPP can manage subsequent artifacts and metadata after that initial registration step.

## Store listing

Metadata is source-controlled under:

`app-android/src/main/play/`

Current source-controlled listings:

- `en-GB`
- `tr-TR`

Publish them with:

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

Publish an exported and reviewed Play Console CSV through Google's official `applications.dataSafety` endpoint:

```powershell
.\scripts\play\publish-data-safety.ps1 -CsvPath "C:\path\data_safety_agentickitchen_filled.csv" -Execute
```

The script uses the same ADC identity and requests the Android Publisher OAuth scope with `gcloud auth application-default print-access-token`.

Keep the reviewed CSV outside the repository until its answers have been revalidated against the exact release SDK/data-flow state. Once final, it can be source-controlled deliberately.

## Intentionally manual in Play Console

Some app-content declarations do not have equivalent Android Publisher API endpoints and remain manual, including target audience, content rating and several policy questionnaires.

## Release discipline

- Keep package name `com.agentickitchen.android`.
- Increment `versionCode` for every uploaded artifact.
- Store release notes only for functionality appropriate to that build's verification status.
- Re-review Data Safety whenever SDK or data-flow behavior changes.
- Never publish roadmap-only or `AUTOMATED_ONLY` behavior as physically verified production functionality.
