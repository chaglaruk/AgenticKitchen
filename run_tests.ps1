# Downloads a temporary Gradle distribution and runs tests for :shared module.
# Requires internet access and Java installed (JDK 11+ recommended).

param()

$gradleVersion = "8.4.1"
$zipUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
$cacheDir = Join-Path $PSScriptRoot ".gradle-dist"
$gradleHome = Join-Path $cacheDir "gradle-$gradleVersion"

if (!(Test-Path $gradleHome)) {
    Write-Output "Downloading Gradle $gradleVersion..."
    try {
        Invoke-WebRequest -Uri $zipUrl -OutFile "$env:TEMP\gradle-$gradleVersion-bin.zip" -TimeoutSec 300
        if (!(Test-Path $cacheDir)) { New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null }
        Write-Output "Extracting Gradle..."
        Expand-Archive -Path "$env:TEMP\gradle-$gradleVersion-bin.zip" -DestinationPath $cacheDir -Force
    } catch {
        Write-Error "Failed to download or extract Gradle. Error: $_"
        exit 1
    }
}

$gradleCmd = Join-Path $gradleHome "bin\gradle.bat"
if (!(Test-Path $gradleCmd)) { Write-Error "Gradle executable not found: $gradleCmd"; exit 1 }

Write-Output "Running :shared:test using Gradle $gradleVersion..."
& $gradleCmd ":shared:test" "--console=plain"

exit $LASTEXITCODE
