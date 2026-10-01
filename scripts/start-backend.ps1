$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($maven) { $mvn = $maven.Source } else {
    $mvn = Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists" -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}
if (!$mvn) { throw 'Instala Maven 3.9+ y agrega mvn al PATH.' }
Set-Location "$root\backend"
& $mvn spring-boot:run
