[CmdletBinding()]
param(
    [string]$ProjectId,
    [string]$ServiceAccountName = "agentickitchen-play-publisher"
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
}

$serviceAccountEmail = "$ServiceAccountName@$ProjectId.iam.gserviceaccount.com"
$scopes = "https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher"

Write-Host ("Creating ADC with Play Publisher scope via: {0}" -f $serviceAccountEmail)
& gcloud auth application-default login `
    "--impersonate-service-account=$serviceAccountEmail" `
    "--scopes=$scopes"

if ($LASTEXITCODE -ne 0) {
    throw "Google Application Default Credentials login failed."
}

$tokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("ADC was created but an access token could not be obtained.`n" + ($tokenOutput -join [Environment]::NewLine))
}

Write-Host "Google Play ADC authentication is ready."
