$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Add-Type -AssemblyName System.Drawing
$size = 16
foreach ($name in @('blank_composite_pattern', 'encoded_composite_pattern')) {
    $sourcePath = Join-Path $root "artwork/original/$name.png"
    $destinationPath = Join-Path $root "shared/src/main/resources/assets/ae2mrp/textures/item/$name.png"
    $originalHash = (Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
    $source = [System.Drawing.Bitmap]::new($sourcePath)
    $target = [System.Drawing.Bitmap]::new($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        # Center-sampled nearest neighbor, with no filtering or alpha blending.
        for ($y = 0; $y -lt $size; $y++) {
            $sourceY = [int][Math]::Floor(($y + 0.5) * $source.Height / $size)
            for ($x = 0; $x -lt $size; $x++) {
                $sourceX = [int][Math]::Floor(($x + 0.5) * $source.Width / $size)
                $target.SetPixel($x, $y, $source.GetPixel($sourceX, $sourceY))
            }
        }
        $target.Save($destinationPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $saved = [System.Drawing.Bitmap]::new($destinationPath)
        try {
            if ($saved.Width -ne $size -or $saved.Height -ne $size) { throw "Invalid export size: $name" }
            for ($y = 0; $y -lt $size; $y++) {
                $sourceY = [int][Math]::Floor(($y + 0.5) * $source.Height / $size)
                for ($x = 0; $x -lt $size; $x++) {
                    $sourceX = [int][Math]::Floor(($x + 0.5) * $source.Width / $size)
                    if ($saved.GetPixel($x, $y).ToArgb() -ne $source.GetPixel($sourceX, $sourceY).ToArgb()) {
                        throw "Nearest-neighbor RGBA mismatch: $name at $x,$y"
                    }
                }
            }
        } finally { $saved.Dispose() }
        Write-Output "$name : $($source.Width)x$($source.Height) -> ${size}x${size}; all RGBA samples verified"
    } finally {
        $target.Dispose()
        $source.Dispose()
    }
    if ((Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash -ne $originalHash) {
        throw "Original changed: $sourcePath"
    }
    Get-FileHash -LiteralPath $destinationPath -Algorithm SHA256 | Select-Object Hash, Path
}
