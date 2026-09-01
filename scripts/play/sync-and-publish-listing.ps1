[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$GoogleAccount,
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $Execute) {
    throw "Refusing to update Google Play without -Execute. Re-run with: .\scripts\play\sync-and-publish-listing.ps1 -GoogleAccount <email> -Execute"
}

& (Join-Path $PSScriptRoot "update-and-setup.ps1") -GoogleAccount $GoogleAccount
if ($LASTEXITCODE -ne 0) {
    throw "Repository sync / Google Cloud setup failed with exit code $LASTEXITCODE."
}

& (Join-Path $PSScriptRoot "auth-google-play.ps1") -GoogleAccount $GoogleAccount
if ($LASTEXITCODE -ne 0) {
    throw "Google Play authentication failed with exit code $LASTEXITCODE."
}

& (Join-Path $PSScriptRoot "publish-listing.ps1") -Execute
if ($LASTEXITCODE -ne 0) {
    throw "Google Play listing publish failed with exit code $LASTEXITCODE."
}
