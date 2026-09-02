[CmdletBinding()]
param(
    [string]$ProjectId,
    [string]$ServiceAccountName = "agentickitchen-play-publisher",
    [string]$GoogleAccount,
    [switch]$CreateProject
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud was not found in PATH."
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    $googleServicesPath = Join-Path $repoRoot "app-android\google-services.json"
    if (-not (Test-Path $googleServicesPath)) {
        throw "ProjectId was not provided and app-android\google-services.json was not found. Pass -ProjectId explicitly."
    }

    try {
        $googleServices = [System.IO.File]::ReadAllText($googleServicesPath) | ConvertFrom-Json
        $ProjectId = [string]$googleServices.project_info.project_id
    }
    catch {
        throw "Could not read project_info.project_id from app-android\google-services.json."
    }

    if ([string]::IsNullOrWhiteSpace($ProjectId)) {
        throw "app-android\google-services.json does not contain project_info.project_id."
    }
}

$adminAccount = (& gcloud config get-value account --quiet).Trim()
if ([string]::IsNullOrWhiteSpace($adminAccount)) {
    throw "No active gcloud admin account was found. Run 'gcloud auth login' first."
}

if ([string]::IsNullOrWhiteSpace($GoogleAccount)) {
    $GoogleAccount = $adminAccount
}

& gcloud projects describe $ProjectId --format="value(projectId)" --quiet 1>$null 2>$null
$projectExists = $LASTEXITCODE -eq 0

if (-not $projectExists) {
    if (-not $CreateProject) {
        throw "Google Cloud project '$ProjectId' does not exist or is not accessible to the active gcloud admin account '$adminAccount'."
    }

    & gcloud projects create $ProjectId --name="AgenticKitchen Play Publisher" --quiet
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create Google Cloud project '$ProjectId'."
    }
}

& gcloud services enable `
    androidpublisher.googleapis.com `
    iamcredentials.googleapis.com `
    --project=$ProjectId `
    --quiet
if ($LASTEXITCODE -ne 0) {
    throw "Failed to enable the Google Play Android Developer API and/or IAM Service Account Credentials API."
}

$serviceAccountEmail = "$ServiceAccountName@$ProjectId.iam.gserviceaccount.com"
& gcloud iam service-accounts describe $serviceAccountEmail --project=$ProjectId --quiet 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    & gcloud iam service-accounts create $ServiceAccountName `
        --display-name="AgenticKitchen Play Publisher" `
        --project=$ProjectId `
        --quiet
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create service account '$serviceAccountEmail'."
    }
}

& gcloud iam service-accounts add-iam-policy-binding $serviceAccountEmail `
    --member="user:$GoogleAccount" `
    --role="roles/iam.serviceAccountTokenCreator" `
    --project=$ProjectId `
    --quiet 1>$null
if ($LASTEXITCODE -ne 0) {
    throw "Failed to grant '$GoogleAccount' permission to impersonate '$serviceAccountEmail'."
}

Write-Host ""
Write-Host "Google Cloud side is ready."
Write-Host ("Cloud admin used for setup: {0}" -f $adminAccount)
Write-Host ("Google account authorised to impersonate Play publisher: {0}" -f $GoogleAccount)
Write-Host ("Service account: {0}" -f $serviceAccountEmail)
Write-Host "Enabled APIs: androidpublisher.googleapis.com, iamcredentials.googleapis.com"
Write-Host ""
Write-Host "Play Console access must be granted to the service account itself:"
Write-Host ("  {0}" -f $serviceAccountEmail)
Write-Host ""
Write-Host "For the current draft/pre-production app grant these APP permissions:"
Write-Host "  - Edit and delete draft apps"
Write-Host "  - Manage store presence"
Write-Host "  - Release apps to testing tracks"
Write-Host "  - Manage testing tracks and edit tester lists"
Write-Host ""
Write-Host "Do NOT grant production-release or financial permissions."
Write-Host "The draft-app permission is needed while the Play app itself is still a draft; it does not allow production rollout."
Write-Host ""
Write-Host "Then authenticate locally with the explicit Google account:"
Write-Host ('  .\scripts\play\auth-google-play.ps1 -GoogleAccount "{0}"' -f $GoogleAccount)
Write-Host ""
Write-Host "Then publish the listing with:"
Write-Host "  .\scripts\play\publish-listing.ps1 -Execute"
