param(
    [Parameter(Mandatory=$true)][string]$ForgeAe2Jar,
    [Parameter(Mandatory=$true)][string]$NeoAe2Jar
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$shared=Join-Path $root 'shared/src/main/resources'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jsonFiles=@(Get-ChildItem $shared,(Join-Path $root 'forge/src/main/resources'),(Join-Path $root 'neoforge/src/main/resources') -Recurse -Filter '*.json')
foreach($file in $jsonFiles){$null=Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json -AsHashtable}
$en=Get-Content -LiteralPath (Join-Path $shared 'assets/ae2mrp/lang/en_us.json') -Raw | ConvertFrom-Json -AsHashtable
$ja=Get-Content -LiteralPath (Join-Path $shared 'assets/ae2mrp/lang/ja_jp.json') -Raw | ConvertFrom-Json -AsHashtable
if(Compare-Object @($en.Keys | Sort-Object) @($ja.Keys | Sort-Object)){throw 'Language keys differ'}
foreach($key in $en.Keys){
    $enTokens=@([regex]::Matches($en[$key],'%(?:\d+\$)?[sdf%]') | ForEach-Object Value)
    $jaTokens=@([regex]::Matches($ja[$key],'%(?:\d+\$)?[sdf%]') | ForEach-Object Value)
    if(($enTokens -join '|') -ne ($jaTokens -join '|')){throw "Format placeholders differ: $key"}
}
$loaderIndex=0
foreach($path in @($ForgeAe2Jar,$NeoAe2Jar)){
    $loader=@('forge','neoforge')[$loaderIndex++]
    $model=Get-Content -LiteralPath (Join-Path $root "$loader/src/main/resources/assets/ae2mrp/models/block/composite_pattern_encoder.json") -Raw | ConvertFrom-Json -AsHashtable
    if($model.ContainsKey('elements') -or $model.loader -ne "${loader}:composite"){throw "Expected native model composition for $loader"}
    if(($model.transform.translation -join ',') -ne '0,0,0.8125'){throw "Unexpected wall offset for $loader"}
    $zip=[System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $path))
    try {
        foreach($entry in @('assets/ae2/textures/guis/background.png','assets/ae2/textures/guis/states.png','assets/ae2/textures/guis/checkbox.png','assets/ae2/textures/guis/text_field.png','assets/ae2/screens/terminals/pattern_access_terminal.json','assets/ae2/models/item/wireless_terminal.json','assets/ae2/textures/item/wireless_terminal.png')){
            if($null -eq $zip.GetEntry($entry)){throw "Missing native GUI/item resource $entry in $path"}
        }
        foreach($texture in @($model.textures.Values | Sort-Object -Unique)){
            $entry='assets/ae2/textures/'+$texture.Substring(4)+'.png'
            if($null -eq $zip.GetEntry($entry)){throw "Missing external texture $entry in $path"}
        }
        foreach($child in $model.children.Values){
            $entry='assets/ae2/models/'+$child.parent.Substring(4)+'.json'
            if($null -eq $zip.GetEntry($entry)){throw "Missing native terminal model $entry in $path"}
        }
    } finally {$zip.Dispose()}
}
foreach($name in @('blank_composite_pattern','encoded_composite_pattern')){
    $path=Join-Path $shared "assets/ae2mrp/textures/item/$name.png"
    $bytes=[IO.File]::ReadAllBytes($path)
    if(($bytes[0..7] -join ',') -ne '137,80,78,71,13,10,26,10'){throw "Not PNG: $path"}
    $width=([int]$bytes[16] -shl 24) -bor ([int]$bytes[17] -shl 16) -bor ([int]$bytes[18] -shl 8) -bor [int]$bytes[19]
    $height=([int]$bytes[20] -shl 24) -bor ([int]$bytes[21] -shl 16) -bor ([int]$bytes[22] -shl 8) -bor [int]$bytes[23]
    if($width -ne 16 -or $height -ne 16){throw "Expected 16x16 game texture: $path ($width x $height)"}
    Get-FileHash -LiteralPath $path -Algorithm SHA256 | Select-Object Hash,Path
}
Write-Output "Validated $($jsonFiles.Count) JSON files, $($en.Count) bilingual keys and all referenced textures in BOTH AE2 versions."
