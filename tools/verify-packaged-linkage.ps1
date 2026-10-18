param(
    [Parameter(Mandatory=$true)][string]$ForgeJar,
    [Parameter(Mandatory=$true)][string]$NeoForgeJar,
    [string]$Javap = 'javap'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($path in @($ForgeJar, $NeoForgeJar)) {
    $resolved = (Resolve-Path -LiteralPath $path).Path
    $zip = [IO.Compression.ZipFile]::OpenRead($resolved)
    try {
        $classes = @($zip.Entries | Where-Object {
            $_.FullName -match '^com/syaru/ae2mrp/platform/EncoderBlockEntity(?:\$\d+)?\.class$'
        } | ForEach-Object { $_.FullName.Replace('/', '.').Replace('.class', '') })
    } finally { $zip.Dispose() }
    $hosts = 0
    foreach ($class in $classes) {
        $output = & $Javap -p -s -c -classpath $resolved $class
        if ($LASTEXITCODE -ne 0) { throw "javap failed: $class in $resolved" }
        $text = $output -join "`n"
        if ($text -notmatch '(?m)^.*\bimplements\b.*appeng\.helpers\.IPatternTerminalLogicHost') { continue }
        $hosts++
        if ($text -match '\bextends net\.minecraft\.world\.level\.block\.entity\.BlockEntity\b') {
            throw "Logic host inherits remapped Minecraft methods: $class"
        }
        foreach ($method in @(
            'public appeng.parts.encoding.PatternEncodingLogic getLogic();',
            'public net.minecraft.world.level.Level getLevel();',
            'public void markForSave();'
        )) {
            if (!$text.Contains($method)) { throw "Missing explicit AE2 host method $method in $class" }
        }
        Write-Output "PASS explicit AE2 logic-host methods: $class in $resolved"
    }
    if ($hosts -ne 1) { throw "Expected one isolated native encoding host, found $hosts in $resolved" }
}
