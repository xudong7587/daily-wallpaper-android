param([string]$Jdk, [string]$BuildTools, [string]$PlatformJar)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!$Jdk -or !$BuildTools -or !$PlatformJar) {
    $cached = Join-Path $env:USERPROFILE '.codex\visualizations\2026\08\26\01a03d27-0ed8-7351-93ed-d41b6c2d47d3'
    if (!$Jdk) { $Jdk = Join-Path $cached 'android-dev-tools\jdk17\jdk-17.0.20.1+1' }
    if (!$BuildTools) { $BuildTools = Join-Path $cached 'apk-reverse-tools\build-tools-35\android-15' }
    if (!$PlatformJar) { $PlatformJar = Join-Path $cached 'android-dev-tools\platform-35\android-35\android.jar' }
}
foreach ($required in @("$Jdk\bin\javac.exe", "$BuildTools\aapt2.exe", $PlatformJar)) {
    if (!(Test-Path -LiteralPath $required)) { throw "Missing tool: $required. Supply -Jdk -BuildTools -PlatformJar." }
}
function Run([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed: $LASTEXITCODE" }
}
# Never package compiled classes from a source file removed in a newer version.
$compiledDir = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'build/classes'))
$buildBoundary = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'build')) + [IO.Path]::DirectorySeparatorChar
if (!$compiledDir.StartsWith($buildBoundary, [StringComparison]::OrdinalIgnoreCase)) { throw 'Compiled output is outside build directory' }
if (Test-Path -LiteralPath $compiledDir) { Remove-Item -LiteralPath $compiledDir -Recurse -Force }
New-Item -ItemType Directory -Force build, build/classes, build/dex, build/generated, dist | Out-Null
Run "$BuildTools\aapt2.exe" @('compile', '--dir', 'app/src/main/res', '-o', 'build/resources.zip')
Run "$BuildTools\aapt2.exe" @('link', '-o', 'build/base.apk', '-I', $PlatformJar, '--java', 'build/generated', '--manifest', 'app/src/main/AndroidManifest.xml', 'build/resources.zip')
$sources = @(Get-ChildItem app/src/main/java,build/generated -Recurse -Filter *.java | ForEach-Object FullName)
Run "$Jdk\bin\javac.exe" (@('-encoding', 'UTF-8', '--release', '8', '-classpath', $PlatformJar, '-d', 'build/classes') + $sources)
Run "$Jdk\bin\jar.exe" @('cf', 'build/classes.jar', '-C', 'build/classes', '.')
Run "$Jdk\bin\java.exe" @('-cp', "$BuildTools\lib\d8.jar", 'com.android.tools.r8.D8', '--lib', $PlatformJar, '--min-api', '24', '--output', 'build/dex', 'build/classes.jar')
Copy-Item -LiteralPath build/base.apk -Destination build/unsigned.apk -Force
Run "$Jdk\bin\jar.exe" @('uf', 'build/unsigned.apk', '-C', 'build/dex', 'classes.dex')
Run "$BuildTools\zipalign.exe" @('-f', '4', 'build/unsigned.apk', 'build/aligned.apk')
if (!(Test-Path -LiteralPath 'local.keystore')) {
    Run "$Jdk\bin\keytool.exe" @('-genkeypair', '-keystore', 'local.keystore', '-storepass', 'android', '-keypass', 'android', '-alias', 'wallpaper', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-dname', 'CN=DailyWallpaper Local Build')
}
Run "$Jdk\bin\java.exe" @('-jar', "$BuildTools\lib\apksigner.jar", 'sign', '--ks', 'local.keystore', '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', 'dist/DailyWallpaper.apk', 'build/aligned.apk')
Run "$Jdk\bin\java.exe" @('-jar', "$BuildTools\lib\apksigner.jar", 'verify', '--verbose', 'dist/DailyWallpaper.apk')
Run "$BuildTools\aapt.exe" @('dump', 'badging', 'dist/DailyWallpaper.apk')
Get-FileHash -LiteralPath 'dist/DailyWallpaper.apk' -Algorithm SHA256
