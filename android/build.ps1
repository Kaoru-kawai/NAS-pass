param(
    [string]$SdkPath = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$JavaPath = 'C:\Program Files\Android\Android Studio\jbr',
    [switch]$Test,
    [switch]$DeviceTests,
    [string]$Serial
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$toolingRoot = Join-Path (Split-Path $projectRoot) '.tooling'
$gradleVersion = '8.11.1'
$gradleRoot = Join-Path $toolingRoot "gradle-$gradleVersion"
if (-not (Test-Path -LiteralPath (Join-Path $SdkPath 'platforms'))) { throw 'Android SDKが見つかりません。Android StudioでSDKを設定してください。' }
if (-not (Test-Path -LiteralPath (Join-Path $JavaPath 'bin\java.exe'))) { throw 'JDKが見つかりません。-JavaPathでJDK 17以降を指定してください。' }
New-Item -ItemType Directory -Path $toolingRoot -Force | Out-Null
if (-not (Test-Path -LiteralPath (Join-Path $gradleRoot 'bin\gradle.bat'))) {
    $zipPath = Join-Path $toolingRoot "gradle-$gradleVersion-bin.zip"
    $downloadUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath
    $checksumContent = (Invoke-WebRequest -Uri "$downloadUrl.sha256").Content
    $checksum = if ($checksumContent -is [byte[]]) { [System.Text.Encoding]::UTF8.GetString($checksumContent).Trim() } else { ([string]$checksumContent).Trim() }
    if ((Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLower() -ne $checksum.ToLower()) { throw 'Gradle checksum mismatch' }
    Expand-Archive -LiteralPath $zipPath -DestinationPath $toolingRoot -Force
}
$env:JAVA_HOME = $JavaPath
$env:ANDROID_HOME = $SdkPath
$env:GRADLE_USER_HOME = Join-Path $toolingRoot 'gradle-user-home'
$gradleCommand = Join-Path $gradleRoot 'bin\gradle.bat'
$target = if ($DeviceTests) { 'assembleDebugAndroidTest' } elseif ($Test) { 'testDebugUnitTest' } else { 'assembleDebug' }
& $gradleCommand -p $projectRoot $target --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
if ($DeviceTests) {
    & $gradleCommand -p $projectRoot assembleDebug --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'App build failed' }
    $adb = Join-Path $SdkPath 'platform-tools\adb.exe'
    $deviceArgs = if ($Serial) { @('-s', $Serial) } else { @() }
    & $adb @deviceArgs install -r (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk')
    if ($LASTEXITCODE -ne 0) { throw 'App APK install failed' }
    & $adb @deviceArgs install -r (Join-Path $projectRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Test APK install failed' }
    $testOutput = & $adb @deviceArgs shell am instrument -w jp.personal.naspass.test/jp.personal.naspass.DeviceTestRunner
    $testExit = $LASTEXITCODE
    $testOutput | Write-Output
    $testOutput | Set-Content -LiteralPath (Join-Path $toolingRoot 'device-test-results.txt')
    if ($testExit -ne 0 -or ($testOutput -join "`n") -match 'FAIL|INSTRUMENTATION_FAILED' -or ($testOutput -join "`n") -notmatch 'PASS membership opening does not repeat') { throw 'Device tests failed' }
} elseif (-not $Test) {
    $outputDir = Join-Path (Split-Path $projectRoot) 'output'
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $outputDir 'NAS-Pass-debug.apk') -Force
    Write-Output "APK: $outputDir\NAS-Pass-debug.apk"
}
