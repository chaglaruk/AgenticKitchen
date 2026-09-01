[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$CsvPath,
    [string]$PackageName = "com.agentickitchen.android",
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
$tokenOutput = & gcloud auth application-default print-access-token `
    --scopes=https://www.googleapis.com/auth/androidpublisher `
    --quiet 2>&1

if ($LASTEXITCODE -ne 0) {
    throw ("Could not obtain an Android Publisher access token from Application Default Credentials. Run the setup/authentication step first.`n" + ($tokenOutput -join [Environment]::NewLine))
}

$token = ($tokenOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "gcloud returned an empty access token."
}

$csv = [System.IO.File]::ReadAllText($resolvedCsv)
$body = @{ safetyLabels = $csv } | ConvertTo-Json -Compress
$uri = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PackageName/dataSafety"

Invoke-RestMethod `
    -Method Post `
    -Uri $uri `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json; charset=utf-8" `
    -Body $body | Out-Null

Write-Host "Google Play Data Safety declaration updated successfully."
