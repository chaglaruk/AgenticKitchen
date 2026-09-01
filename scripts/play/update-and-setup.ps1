[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$expectedBranch = "refactor/agentic-kitchen-production-foundation"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

Push-Location $repoRoot
try {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw "git was not found in PATH."
    }

    $status = & git status --porcelain
    if ($LASTEXITCODE -ne 0) {
        throw "Could not read git working tree status."
    }
    if ($status) {
        throw "Working tree is not clean. Commit, stash, or discard local changes before updating. No files were changed by this script."
    }

    & git fetch origin --prune
    if ($LASTEXITCODE -ne 0) {
        throw "git fetch failed."
    }

    $currentBranch = (& git branch --show-current).Trim()
    if ($currentBranch -ne $expectedBranch) {
        & git switch $expectedBranch
        if ($LASTEXITCODE -ne 0) {
            throw "Could not switch to '$expectedBranch'."
        }
    }

    & git pull --ff-only origin $expectedBranch
    if ($LASTEXITCODE -ne 0) {
        throw "Fast-forward pull failed. Resolve the local branch state manually; this script will not rebase, reset, or force-update."
    }

    $head = (& git rev-parse HEAD).Trim()
    Write-Host ("AgenticKitchen updated to {0}" -f $head)
    Write-Host ""

    & (Join-Path $PSScriptRoot "setup-google-cloud.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Google Play setup failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
