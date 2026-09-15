param([string]$Jdk)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!$Jdk) { $Jdk = Join-Path $env:USERPROFILE '.codex\visualizations\2026\08\26\01a03d27-0ed8-7351-93ed-d41b6c2d47d3\android-dev-tools\jdk17\jdk-17.0.20.1+1' }
New-Item -ItemType Directory -Force build/test-classes | Out-Null
& "$Jdk\bin\javac.exe" -encoding UTF-8 -d build/test-classes app/src/main/java/cn/sunny/wallpaper/DailyTime.java app/src/main/java/cn/sunny/wallpaper/IntervalTime.java app/src/main/java/cn/sunny/wallpaper/ActiveHours.java app/src/main/java/cn/sunny/wallpaper/ContentStore.java app/src/main/java/cn/sunny/wallpaper/Selection.java tests/DailyTimeTest.java tests/LibraryLogicTest.java tests/IntervalTimeTest.java tests/ActiveHoursTest.java app/src/main/java/cn/sunny/wallpaper/WallpaperFlow.java tests/WallpaperFlowTest.java app/src/main/java/cn/sunny/wallpaper/WallpaperResult.java tests/WallpaperResultTest.java
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& "$Jdk\bin\java.exe" -cp build/test-classes cn.sunny.wallpaper.DailyTimeTest
if ($LASTEXITCODE -ne 0) { throw 'DailyTime tests failed' }
& "$Jdk\bin\java.exe" -cp build/test-classes cn.sunny.wallpaper.IntervalTimeTest
if ($LASTEXITCODE -ne 0) { throw 'IntervalTime tests failed' }
& "$Jdk\bin\java.exe" -cp build/test-classes cn.sunny.wallpaper.ActiveHoursTest
if ($LASTEXITCODE -ne 0) { throw 'Active hours tests failed' }
& "$Jdk\bin\java.exe" -cp build/test-classes cn.sunny.wallpaper.LibraryLogicTest "build/test-run-$([guid]::NewGuid().ToString('N'))"
if ($LASTEXITCODE -ne 0) { throw 'Library tests failed' }

& "$Jdk\bin\java.exe" -cp build/test-classes cn.sunny.wallpaper.WallpaperFlowTest
if ($LASTEXITCODE -ne 0) { throw 'Wallpaper fallback flow tests failed' }

& "$Jdk\bin\java.exe" -cp build/test-classes cn.sunny.wallpaper.WallpaperResultTest
if ($LASTEXITCODE -ne 0) { throw 'Wallpaper result tests failed' }
