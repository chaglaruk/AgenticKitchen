[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$GoogleAccount,
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

Write-Host ("Authenticating ADC explicitly as: {0}" -f $GoogleAccount)
Write-Host ("Play publisher service account: {0}" -f $serviceAccountEmail)

& gcloud auth application-default login $GoogleAccount `
    --scopes=https://www.googleapis.com/auth/cloud-platform
if ($LASTEXITCODE -ne 0) {
    throw "Google Application Default Credentials login failed for '$GoogleAccount'."
}

$adcTokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("User ADC was created but could not produce an access token.`n" + ($adcTokenOutput -join [Environment]::NewLine))
}

$impersonationError = $null
$impersonationReady = $false
for ($attempt = 1; $attempt -le 12; $attempt++) {
    $impersonationOutput = & gcloud auth print-access-token $GoogleAccount `
        "--impersonate-service-account=$serviceAccountEmail" `
        --quiet 2>&1

    if ($LASTEXITCODE -eq 0) {
        $impersonationReady = $true
        break
    }

    $impersonationError = $impersonationOutput -join [Environment]::NewLine
    if ($attempt -lt 12) {
        Write-Host ("Waiting for IAM impersonation permission to propagate ({0}/12)..." -f $attempt)
        Start-Sleep -Seconds 10
    }
}

if (-not $impersonationReady) {
    throw ("'$GoogleAccount' has valid ADC but cannot impersonate the Play publisher service account. Re-run setup with -GoogleAccount '$GoogleAccount'.`n" + $impersonationError)
}

$env:AGENTICKITCHEN_PLAY_SERVICE_ACCOUNT = $serviceAccountEmail
Write-Host "Google Play authentication is ready."
Write-Host ("ADC account: {0}" -f $GoogleAccount)
