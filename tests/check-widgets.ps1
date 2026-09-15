$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Set-Location $project
$ns = 'http://schemas.android.com/apk/res/android'
foreach ($size in @(@(1,1), @(1,2), @(2,4), @(2,5))) {
    $height = $size[0]; $width = $size[1]
    [xml]$provider = Get-Content -LiteralPath "app/src/main/res/xml/widget_${height}x${width}.xml"
    if ($provider.DocumentElement.GetAttribute('targetCellWidth', $ns) -ne "$width" -or $provider.DocumentElement.GetAttribute('targetCellHeight', $ns) -ne "$height") { throw 'Widget dimensions changed' }
}
foreach ($layout in @('widget_small','widget_bar','widget_wide')) {
    [xml]$xml = Get-Content -LiteralPath "app/src/main/res/layout/$layout.xml"
    $spinner = @($xml.SelectNodes('//*') | Where-Object { $_.GetAttribute('id', $ns) -eq '@+id/widget_spinner' })
    if ($spinner.Count -ne 1 -or $spinner[0].Name -ne 'ProgressBar' -or $spinner[0].GetAttribute('indeterminate', $ns) -ne 'true') { throw "$layout must have a native spinner" }
}
[xml]$small = Get-Content -LiteralPath app/src/main/res/layout/widget_small.xml
if ($small.DocumentElement.GetAttribute('background', $ns)) { throw '1x1 has a full-cell background again' }
$button = $small.SelectSingleNode('//FrameLayout')
if ($button.GetAttribute('layout_width', $ns) -ne '52dp' -or $button.GetAttribute('layout_height', $ns) -ne '52dp') { throw 'Small icon default no longer fits app-icon scale' }
[xml]$manifest = Get-Content -LiteralPath app/src/main/AndroidManifest.xml
if (@($manifest.manifest.application.activity | Where-Object { $_.GetAttribute('name', $ns) -eq '.WidgetActionActivity' }).Count) { throw 'Popup activity still registered' }
$code = Get-Content -LiteralPath app/src/main/java/cn/sunny/wallpaper/WallpaperWidget.java -Raw
if ($code.Contains('PendingIntent.getActivity') -or !$code.Contains('PendingIntent.getBroadcast')) { throw 'Widget click no longer stays on launcher' }
Write-Output 'PASS: height x width, native spinner in all layouts, bounded small icon, no popup entry point'
