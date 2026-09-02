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

The preferred workflow does not create or store a long-lived Google service-account JSON key. GPP is configured to use Google Application Default Credentials (ADC) and service-account impersonation.

With the normal AgenticKitchen local setup, run:

```powershell
.\scripts\play\setup-google-cloud.ps1 -GoogleAccount "YOUR_PLAY_CONSOLE_GOOGLE_ACCOUNT"
```

The setup script reads the existing Firebase/Google Cloud project ID locally from the git-ignored `app-android/google-services.json`, enables `androidpublisher.googleapis.com` and `iamcredentials.googleapis.com`, creates the `agentickitchen-play-publisher` service account when needed, and grants the explicitly supplied Google account permission to impersonate it. The project ID does not need to be copied into chat or source control.

If `google-services.json` is unavailable, pass an existing project explicitly:

```powershell
.\scripts\play\setup-google-cloud.ps1 -ProjectId "YOUR_GCP_PROJECT_ID" -GoogleAccount "YOUR_PLAY_CONSOLE_GOOGLE_ACCOUNT"
```

A separate project can also be deliberately created with `-CreateProject`.

Google no longer requires a Play developer account to be linked to the Google Cloud project used for Android Publisher API access.

## Required Play Console permission step

The Cloud service account still has to be invited in Play Console under `Settings > Users and permissions` and granted access to Agentic Kitchen.

While the Play app itself is still a draft, grant these app-level permissions:

- Edit and delete draft apps
- Manage store presence
- Release apps to testing tracks
- Manage testing tracks and edit tester lists

`Edit and delete draft apps` is required to commit edits while the app is still in draft state. It does not grant production rollout rights.

Do not grant production-release or financial permissions at this stage.

After the Play Console invitation is active, authenticate with the explicit Play Console Google account:

```powershell
.\scripts\play\auth-google-play.ps1 -GoogleAccount "YOUR_PLAY_CONSOLE_GOOGLE_ACCOUNT"
```

On Windows, this helper uses `gcloud auth login ... --update-adc` rather than the older `gcloud auth application-default login` path that can fail in some CLI builds. It then verifies service-account impersonation directly through IAM Credentials before allowing Play publishing to continue.

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

Do not place documentation or arbitrary files inside `src/main/play`; GPP validates this tree as Play metadata. Do not run `bootstrapListing` casually: GPP documents that bootstrapping resets an existing `play` metadata folder.

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

The script uses the same ADC identity and short-lived impersonated service-account credentials.

Keep the reviewed CSV outside the repository until its answers have been revalidated against the exact release SDK/data-flow state. Once final, it can be source-controlled deliberately.

## Intentionally manual in Play Console

Some app-content declarations do not have equivalent Android Publisher API endpoints and remain manual, including target audience, content rating and several policy questionnaires.

## Release discipline

- Keep package name `com.agentickitchen.android`.
- Increment `versionCode` for every uploaded artifact.
- Store release notes only for functionality appropriate to that build's verification status.
- Re-review Data Safety whenever SDK or data-flow behavior changes.
- Never publish roadmap-only or `AUTOMATED_ONLY` behavior as physically verified production functionality.
