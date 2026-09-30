[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$KeystorePath,
    [string]$KeyAlias = "agentickitchen-upload",
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $Execute) {
    throw "Refusing to publish without -Execute."
}

$resolvedKeystore = (Resolve-Path -LiteralPath $KeystorePath -ErrorAction Stop).Path
if (-not (Test-Path -LiteralPath $resolvedKeystore -PathType Leaf)) {
    throw "Upload keystore was not found: $KeystorePath"
}

$storeSecure = Read-Host "Upload keystore password" -AsSecureString
$keySecure = Read-Host "Upload key password" -AsSecureString

$storeBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storeSecure)
$keyBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keySecure)

$previous = @{
    AK_UPLOAD_KEYSTORE_PATH = $env:AK_UPLOAD_KEYSTORE_PATH
    AK_UPLOAD_STORE_PASSWORD = $env:AK_UPLOAD_STORE_PASSWORD
    AK_UPLOAD_KEY_ALIAS = $env:AK_UPLOAD_KEY_ALIAS
    AK_UPLOAD_KEY_PASSWORD = $env:AK_UPLOAD_KEY_PASSWORD
}

try {
    $env:AK_UPLOAD_KEYSTORE_PATH = $resolvedKeystore
    $env:AK_UPLOAD_STORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($storeBstr)
    $env:AK_UPLOAD_KEY_ALIAS = $KeyAlias
    $env:AK_UPLOAD_KEY_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyBstr)

    if (Get-Command keytool -ErrorAction SilentlyContinue) {
        & keytool -list -keystore $resolvedKeystore -alias $KeyAlias -storepass:env AK_UPLOAD_STORE_PASSWORD 1>$null
        if ($LASTEXITCODE -ne 0) {
            throw "The upload keystore could not be opened with the supplied password/alias."
        }
    }

    & (Join-Path $PSScriptRoot "publish-internal-bundle.ps1") -Execute
    if ($LASTEXITCODE -ne 0) {
        throw "Internal bundle publish helper failed with exit code $LASTEXITCODE."
    }
}
finally {
    foreach ($name in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], "Process")
    }
    if ($storeBstr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($storeBstr)
    }
    if ($keyBstr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyBstr)
    }
    Remove-Variable storeSecure, keySecure -ErrorAction SilentlyContinue
}
