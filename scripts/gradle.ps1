param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments = @(':app:assembleDebug'))

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
Push-Location $projectRoot
try {
    if (-not $env:JAVA_HOME) {
        $studioJdk = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
        if (-not (Test-Path -LiteralPath (Join-Path $studioJdk 'bin\java.exe'))) {
            throw 'Configurá JAVA_HOME con un JDK compatible o instalá Android Studio.'
        }
        $env:JAVA_HOME = $studioJdk
    }
    # Esta copia del truststore solo existe en el equipo donde se necesitó.
    # No se distribuye ni modifica la confianza global del JDK.
    $localTrust = Join-Path $projectRoot 'work\build-truststore'
    if (Test-Path -LiteralPath $localTrust) {
        $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions `"-Djavax.net.ssl.trustStore=$localTrust`" -Djavax.net.ssl.trustStorePassword=changeit"
    }
    & .\gradlew.bat @GradleArguments
    $result = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    Pop-Location
}
exit $result
