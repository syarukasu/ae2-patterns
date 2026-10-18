$ErrorActionPreference='Stop'
$jar=Join-Path $PSScriptRoot 'google-java-format.jar'
$expected='16B2A1EE938686C8B1D88ABF19EB83DFD0D623CEE9DE6FC6D09980214F816D3F'
if(!(Test-Path -LiteralPath $jar)){
    Invoke-WebRequest 'https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/1.22.0/google-java-format-1.22.0-all-deps.jar' -OutFile $jar
}
if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne $expected){throw 'Formatter SHA-256 mismatch'}
$files=Get-ChildItem (Split-Path $PSScriptRoot -Parent) -Recurse -Filter '*.java' | Where-Object FullName -NotMatch '\\build\\'
& java -jar $jar --replace $files.FullName
if($LASTEXITCODE -ne 0){throw 'Formatter failed'}
