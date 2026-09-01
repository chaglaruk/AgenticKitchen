[CmdletBinding()]
param(
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $Execute) {
    throw "Refusing to upload an App Bundle without -Execute. Re-run with: .\scripts\play\publish-internal-bundle.ps1 -Execute"
}

if ([string]::IsNullOrWhiteSpace($env:ANDROID_PUBLISHER_CREDENTIALS) -and
    [string]::IsNullOrWhiteSpace($env:GOOGLE_APPLICATION_CREDENTIALS)) {
    throw "Google Play credentials are not configured. Set ANDROID_PUBLISHER_CREDENTIALS or GOOGLE_APPLICATION_CREDENTIALS first."
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Push-Location $repoRoot
try {
    & .\gradlew.bat :app-android:publishReleaseBundle --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "App Bundle upload failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
