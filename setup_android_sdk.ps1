# Downloads Android SDK command line tools and sets up basic SDK for APK build.
# Requires internet access and Java installed.

param()

$sdkVersion = "11076708"  # Latest command line tools version
$zipUrl = "https://dl.google.com/android/repository/commandlinetools-win-$sdkVersion_latest.zip"
$cacheDir = Join-Path $PSScriptRoot ".android-sdk"
$sdkHome = Join-Path $cacheDir "cmdline-tools"

if (!(Test-Path $sdkHome)) {
    Write-Output "Downloading Android SDK command line tools..."
    try {
        Invoke-WebRequest -Uri $zipUrl -OutFile "$env:TEMP\sdk-tools.zip" -TimeoutSec 600
        if (!(Test-Path $cacheDir)) { New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null }
        Write-Output "Extracting SDK tools..."
        Expand-Archive -Path "$env:TEMP\sdk-tools.zip" -DestinationPath $cacheDir -Force
        # Move to cmdline-tools/latest
        $latestDir = Join-Path $sdkHome "latest"
        if (!(Test-Path $latestDir)) { New-Item -ItemType Directory -Force -Path $latestDir | Out-Null }
        Move-Item -Path (Join-Path $cacheDir "cmdline-tools\bin") -Destination $latestDir -Force
        Move-Item -Path (Join-Path $cacheDir "cmdline-tools\lib") -Destination $latestDir -Force
        Move-Item -Path (Join-Path $cacheDir "cmdline-tools\NOTICE.txt") -Destination $latestDir -Force
        Move-Item -Path (Join-Path $cacheDir "cmdline-tools\source.properties") -Destination $latestDir -Force
    } catch {
        Write-Error "Failed to download or extract Android SDK. Error: $_"
        exit 1
    }
}

$env:ANDROID_HOME = $cacheDir
$env:PATH = "$env:PATH;$sdkHome\latest\bin"

Write-Output "Installing Android SDK components..."
& "sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0" "--sdk_root=$cacheDir" "y"

Write-Output "Android SDK setup complete. ANDROID_HOME: $env:ANDROID_HOME"