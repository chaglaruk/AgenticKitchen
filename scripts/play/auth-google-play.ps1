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

if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    throw "A Google Cloud project ID could not be resolved."
}

$serviceAccountEmail = "$ServiceAccountName@$ProjectId.iam.gserviceaccount.com"

Write-Host ("Authenticating gcloud + ADC explicitly as: {0}" -f $GoogleAccount)
Write-Host ("Play publisher service account: {0}" -f $serviceAccountEmail)

# `gcloud auth application-default login` currently fails on some Windows/gcloud
# builds with "None could not be converted to bytes". The documented
# `gcloud auth login --update-adc` path writes the user login to ADC without that flow.
# Keep the existing Cloud-admin account active so future setup runs can still manage IAM.
& gcloud auth login $GoogleAccount `
    --force `
    --update-adc `
    --no-activate
if ($LASTEXITCODE -ne 0) {
    throw "Google login / ADC creation failed for '$GoogleAccount'."
}

$adcTokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("ADC was written but could not produce an access token.`n" + ($adcTokenOutput -join [Environment]::NewLine))
}

$adcToken = ($adcTokenOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($adcToken)) {
    throw "ADC returned an empty access token."
}

# Verify impersonation with the exact ADC principal GPP will use. Do not continue
# to Gradle until IAM Credentials can mint an Android Publisher token.
$encodedServiceAccount = [uri]::EscapeDataString($serviceAccountEmail)
$impersonationUri = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/$encodedServiceAccount`:generateAccessToken"
$impersonationBody = @{
    scope = @("https://www.googleapis.com/auth/androidpublisher")
    lifetime = "600s"
} | ConvertTo-Json -Compress

$impersonationReady = $false
$impersonationError = $null
for ($attempt = 1; $attempt -le 12; $attempt++) {
    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri $impersonationUri `
            -Headers @{ Authorization = "Bearer $adcToken" } `
            -ContentType "application/json; charset=utf-8" `
            -Body $impersonationBody

        if (-not [string]::IsNullOrWhiteSpace([string]$response.accessToken)) {
            $impersonationReady = $true
            break
        }

        $impersonationError = "IAM Credentials API returned no access token."
    }
    catch {
        $impersonationError = $_.Exception.Message
    }

    if ($attempt -lt 12) {
        Write-Host ("Waiting for IAM impersonation permission to propagate ({0}/12)..." -f $attempt)
        Start-Sleep -Seconds 10
    }
}

if (-not $impersonationReady) {
    throw ("ADC for '$GoogleAccount' is valid, but it cannot impersonate '$serviceAccountEmail'. Re-run setup with the same -GoogleAccount value.`n" + $impersonationError)
}

Write-Host "Google Play authentication is ready."
Write-Host ("ADC account requested: {0}" -f $GoogleAccount)
Write-Host ("Impersonation verified: {0}" -f $serviceAccountEmail)
