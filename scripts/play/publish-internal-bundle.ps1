[CmdletBinding()]
param(
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $Execute) {
    throw "Refusing to upload an App Bundle without -Execute. Re-run with: .\scripts\play\publish-internal-bundle.ps1 -Execute"
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud is required for the keyless Google Play publishing workflow and was not found in PATH."
}

$tokenOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("Google Application Default Credentials are not ready. Run .\scripts\play\auth-google-play.ps1 first.`n" + ($tokenOutput -join [Environment]::NewLine))
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
