[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$CsvPath,
    [string]$PackageName = "com.agentickitchen.android",
    [string]$ServiceAccountName = "agentickitchen-play-publisher",
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $Execute) {
    throw "Refusing to update the Play Data safety declaration without -Execute. Re-run with: .\scripts\play\publish-data-safety.ps1 -CsvPath <path> -Execute"
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud is required for the Data Safety REST call and was not found in PATH."
}

$resolvedCsv = (Resolve-Path $CsvPath).Path
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$googleServicesPath = Join-Path $repoRoot "app-android\google-services.json"
if (-not (Test-Path $googleServicesPath)) {
    throw "app-android\google-services.json was not found; cannot resolve the local Play publisher service account."
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
$userTokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("Could not obtain the user ADC access token. Run .\scripts\play\auth-google-play.ps1 first.`n" + ($userTokenOutput -join [Environment]::NewLine))
}

$userToken = ($userTokenOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($userToken)) {
    throw "gcloud returned an empty user ADC access token."
}

$encodedServiceAccount = [System.Uri]::EscapeDataString($serviceAccountEmail)
$iamUri = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/$encodedServiceAccount`:generateAccessToken"
$iamBody = @{
    scope = @("https://www.googleapis.com/auth/androidpublisher")
    lifetime = "3600s"
} | ConvertTo-Json -Compress

try {
    $iamResponse = Invoke-RestMethod `
        -Method Post `
        -Uri $iamUri `
        -Headers @{
            Authorization = "Bearer $userToken"
            "x-goog-user-project" = $projectId
        } `
        -ContentType "application/json; charset=utf-8" `
        -Body $iamBody
}
catch {
    throw ("Could not mint a short-lived Android Publisher token for the Play service account. Re-run .\scripts\play\setup-google-cloud.ps1 and .\scripts\play\auth-google-play.ps1.`n" + $_.Exception.Message)
}

$playToken = [string]$iamResponse.accessToken
if ([string]::IsNullOrWhiteSpace($playToken)) {
    throw "IAM Service Account Credentials returned an empty access token."
}

$csv = [System.IO.File]::ReadAllText($resolvedCsv)
$body = @{ safetyLabels = $csv } | ConvertTo-Json -Compress
$uri = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PackageName/dataSafety"

Invoke-RestMethod `
    -Method Post `
    -Uri $uri `
    -Headers @{ Authorization = "Bearer $playToken" } `
    -ContentType "application/json; charset=utf-8" `
    -Body $body | Out-Null

Write-Host "Google Play Data Safety declaration updated successfully."
