$ErrorActionPreference = 'Stop'
$main = Get-Content -Raw "$PSScriptRoot/../app/src/main/java/cn/sunny/wallpaper/MainActivity.java"
$guide = Get-Content -Raw "$PSScriptRoot/../app/src/main/java/cn/sunny/wallpaper/OnboardingActivity.java"
$gallery = Get-Content -Raw "$PSScriptRoot/../app/src/main/java/cn/sunny/wallpaper/GalleryActivity.java"
foreach ($required in @('今天，换一处风景', '自动更换', '允许时段', '图片来源', '权限与后台', '查看完整权限引导')) {
    if (!$main.Contains($required)) { throw "Main UI is missing: $required" }
}
foreach ($required in @('允许设置壁纸', '允许准时执行', '允许后台运行', '进入应用')) {
    if (!$guide.Contains($required)) { throw "Onboarding is missing: $required" }
}
if (!$guide.Contains('onboarding_complete') -or !$main.Contains('!p.contains("revision")')) { throw 'First-run onboarding guard is missing' }
if (!$gallery.Contains('Ui.button') -or !$gallery.Contains('Ui.shape') -or !$gallery.Contains('我的图库')) { throw 'Gallery visual system is missing' }
Write-Host 'PASS: first-run permission guide, daily dashboard hierarchy, reusable guide and gallery visual system'
