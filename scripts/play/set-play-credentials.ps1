[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$resolved = (Resolve-Path $Path).Path
$json = [System.IO.File]::ReadAllText($resolved)

try {
    $parsed = $json | ConvertFrom-Json
}
catch {
    throw "Credential file is not valid JSON: $resolved"
}

if ([string]::IsNullOrWhiteSpace([string]$parsed.client_email) -or
    [string]::IsNullOrWhiteSpace([string]$parsed.private_key)) {
    throw "Credential file does not look like a Google service-account JSON key."
}

$env:ANDROID_PUBLISHER_CREDENTIALS = $json
Write-Host "ANDROID_PUBLISHER_CREDENTIALS loaded for this PowerShell process."
Write-Host ("Service account: {0}" -f $parsed.client_email)
Write-Host "The key itself was not printed."
