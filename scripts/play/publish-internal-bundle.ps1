[CmdletBinding()]
param(
    [switch]$Execute,
    [string]$ServiceAccountName = "agentickitchen-play-publisher"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $Execute) {
    throw "Refusing to upload an App Bundle without -Execute. Re-run with: .\scripts\play\publish-internal-bundle.ps1 -Execute"
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud is required for the keyless Google Play publishing workflow and was not found in PATH."
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$googleServicesPath = Join-Path $repoRoot "app-android\google-services.json"
if (-not (Test-Path $googleServicesPath)) {
    throw "app-android\google-services.json was not found; cannot resolve the local Play publisher service account."
}

$requiredSigningEnvVars = @(
    "AK_UPLOAD_KEYSTORE_PATH",
    "AK_UPLOAD_STORE_PASSWORD",
    "AK_UPLOAD_KEY_ALIAS",
    "AK_UPLOAD_KEY_PASSWORD"
)
$missingSigningEnvVars = @(
    $requiredSigningEnvVars | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) }
)
if ($missingSigningEnvVars.Count -gt 0) {
    throw (
        "Release upload signing is not ready. Missing required environment variable(s): " +
        (($missingSigningEnvVars | Sort-Object) -join ", ")
    )
}

try {
    $googleServices = [System.IO.File]::ReadAllText($googleServicesPath) | ConvertFrom-Json
    $projectId = [string]$googleServices.project_info.project_id
}
catch {
    throw "Could not read project_info.project_id from app-android\google-services.json."
}

if ([string]::IsNullOrWhiteSpace($projectId)) {
    throw "app-android\google-services.json does not contain project_info.project_id."
}

$serviceAccountEmail = "$ServiceAccountName@$projectId.iam.gserviceaccount.com"
$tokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("Google Application Default Credentials are not ready. Run .\scripts\play\auth-google-play.ps1 first.`n" + ($tokenOutput -join [Environment]::NewLine))
}

$previousImpersonation = $env:ANDROID_PUBLISHER_IMPERSONATE_SERVICE_ACCOUNT
$env:ANDROID_PUBLISHER_IMPERSONATE_SERVICE_ACCOUNT = $serviceAccountEmail

Push-Location $repoRoot
try {
    & .\gradlew.bat :app-android:publishReleaseBundle --track internal --release-status completed --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "App Bundle upload failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
    $env:ANDROID_PUBLISHER_IMPERSONATE_SERVICE_ACCOUNT = $previousImpersonation
}
