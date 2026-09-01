[CmdletBinding()]
param(
    [string]$ProjectId,
    [string]$ServiceAccountName = "agentickitchen-play-publisher",
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

& gcloud projects describe $ProjectId --format="value(projectId)" --quiet 1>$null 2>$null
$projectExists = $LASTEXITCODE -eq 0

if (-not $projectExists) {
    if (-not $CreateProject) {
        throw "Google Cloud project '$ProjectId' does not exist or is not accessible. Re-run with -CreateProject if you want this script to create it."
    }

    & gcloud projects create $ProjectId --name="AgenticKitchen Play Publisher" --quiet
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create Google Cloud project '$ProjectId'."
    }
}

& gcloud services enable androidpublisher.googleapis.com --project=$ProjectId --quiet
if ($LASTEXITCODE -ne 0) {
    throw "Failed to enable the Google Play Android Developer API."
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

$currentAccount = (& gcloud config get-value account --quiet).Trim()
if ([string]::IsNullOrWhiteSpace($currentAccount)) {
    throw "No active gcloud user account was found. Run 'gcloud auth login' first."
}

& gcloud iam service-accounts add-iam-policy-binding $serviceAccountEmail `
    --member="user:$currentAccount" `
    --role="roles/iam.serviceAccountTokenCreator" `
    --project=$ProjectId `
    --quiet 1>$null
if ($LASTEXITCODE -ne 0) {
    throw "Failed to grant the current gcloud user permission to impersonate the service account."
}

Write-Host ""
Write-Host "Google Cloud side is ready."
Write-Host ("Service account: {0}" -f $serviceAccountEmail)
Write-Host ""
Write-Host "Manual Play Console step still required:"
Write-Host "  Settings > Users and permissions > Invite new users"
Write-Host ("  Invite: {0}" -f $serviceAccountEmail)
Write-Host "  Grant this app: Agentic Kitchen"
Write-Host "  Permissions: Manage store presence; Release apps to testing tracks; Manage testing tracks and edit tester lists."
Write-Host "  Do NOT grant production-release or financial permissions at this stage."
Write-Host ""
Write-Host "After the Play Console invitation is active, run:"
Write-Host "  .\scripts\play\auth-google-play.ps1"
Write-Host ""
Write-Host "Then publish the listing with:"
Write-Host "  .\scripts\play\publish-listing.ps1 -Execute"
