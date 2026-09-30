[CmdletBinding()]
param(
    [string]$Track = "internal",
    [string]$ServiceAccountName = "agentickitchen-play-publisher"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud is required for the keyless Google Play track-read workflow and was not found in PATH."
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$googleServicesPath = Join-Path $repoRoot "app-android\google-services.json"
if (-not (Test-Path $googleServicesPath)) {
    throw "app-android\google-services.json was not found; cannot resolve the Play publisher project."
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

$packageName = "com.agentickitchen.android"
$serviceAccountEmail = "$ServiceAccountName@$projectId.iam.gserviceaccount.com"

$adcOutput = & gcloud auth application-default print-access-token --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("Google Application Default Credentials are not ready. Run .\scripts\play\auth-google-play.ps1 first." + [Environment]::NewLine + ($adcOutput -join [Environment]::NewLine))
}
$adcToken = ($adcOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($adcToken)) {
    throw "ADC returned an empty access token."
}

$encodedServiceAccount = [uri]::EscapeDataString($serviceAccountEmail)
$impersonationUri = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/${encodedServiceAccount}:generateAccessToken"
$impersonationBody = @{ scope = @("https://www.googleapis.com/auth/androidpublisher"); lifetime = "600s" } | ConvertTo-Json -Compress
$impersonated = Invoke-RestMethod -Method Post -Uri $impersonationUri -Headers @{ Authorization = "Bearer $adcToken" } -ContentType "application/json; charset=utf-8" -Body $impersonationBody

$publisherToken = [string]$impersonated.accessToken
if ([string]::IsNullOrWhiteSpace($publisherToken)) {
    throw "IAM Credentials API returned no Android Publisher access token."
}

$headers = @{ Authorization = "Bearer $publisherToken" }
$baseUri = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$packageName"
$editId = $null

try {
    $edit = Invoke-RestMethod -Method Post -Uri "$baseUri/edits" -Headers $headers -ContentType "application/json; charset=utf-8" -Body "{}"
    $editId = [string]$edit.id
    if ([string]::IsNullOrWhiteSpace($editId)) {
        throw "Android Publisher API did not return an edit id."
    }

    $trackInfo = Invoke-RestMethod -Method Get -Uri "$baseUri/edits/$editId/tracks/$Track" -Headers $headers
    $releases = @($trackInfo.releases | ForEach-Object {
        $release = $_
        [pscustomobject]@{
            status = if ($release.PSObject.Properties["status"]) { [string]$release.status } else { $null }
            name = if ($release.PSObject.Properties["name"]) { [string]$release.name } else { $null }
            versionCodes = if ($release.PSObject.Properties["versionCodes"]) {
                @($release.versionCodes | ForEach-Object { [string]$_ })
            } else {
                @()
            }
            userFraction = if ($release.PSObject.Properties["userFraction"]) { $release.userFraction } else { $null }
        }
    })

    [pscustomobject]@{
        packageName = $packageName
        track = [string]$trackInfo.track
        releases = $releases
    } | ConvertTo-Json -Depth 6
}
finally {
    if (-not [string]::IsNullOrWhiteSpace($editId)) {
        try {
            Invoke-RestMethod -Method Delete -Uri "$baseUri/edits/$editId" -Headers $headers | Out-Null
        }
        catch {
            Write-Warning "The temporary read-only Play edit could not be deleted automatically. No edit was committed."
        }
    }
}
