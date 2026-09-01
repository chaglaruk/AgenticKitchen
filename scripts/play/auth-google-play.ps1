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
$currentAccount = (& gcloud config get-value account --quiet).Trim()
if ([string]::IsNullOrWhiteSpace($currentAccount)) {
    throw "No active gcloud user account was found. Run 'gcloud auth login' first."
}

Write-Host ("Creating user ADC for: {0}" -f $currentAccount)
Write-Host "GPP will impersonate the Play publisher service account itself."

# Keep ADC as the human user. The human ADC only needs Cloud Platform access to call
# IAM Service Account Credentials. GPP requests the Android Publisher scope on the
# short-lived impersonated service-account token.
& gcloud auth application-default login $currentAccount `
    --scopes=https://www.googleapis.com/auth/cloud-platform
if ($LASTEXITCODE -ne 0) {
    throw "Google Application Default Credentials login failed."
}

$adcTokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("User ADC was created but could not produce an access token.`n" + ($adcTokenOutput -join [Environment]::NewLine))
}

# Verify the exact CLI user granted Token Creator can impersonate the service account.
# IAM changes can take a short time to propagate, so retry before failing.
$impersonationError = $null
$impersonationReady = $false
for ($attempt = 1; $attempt -le 12; $attempt++) {
    $impersonationOutput = & gcloud auth print-access-token $currentAccount `
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
    throw ("User ADC is valid, but service-account impersonation is still denied. Re-run .\scripts\play\setup-google-cloud.ps1 and verify that the browser/CLI account is '$currentAccount'.`n" + $impersonationError)
}

Write-Host "Google Play authentication is ready."
Write-Host ("ADC user: {0}" -f $currentAccount)
Write-Host ("Play publisher service account: {0}" -f $serviceAccountEmail)
