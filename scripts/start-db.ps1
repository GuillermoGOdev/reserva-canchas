$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$bin = 'C:\Program Files\PostgreSQL\18\bin'
if (!(Test-Path "$bin\pg_ctl.exe")) { throw 'Instala PostgreSQL 18 o inicia compose.yaml y omite este script.' }
$local = Join-Path $root '.local'
$data = Join-Path $local 'pgdata'
New-Item -ItemType Directory -Force $local | Out-Null
if (!(Test-Path "$data\PG_VERSION")) {
    $passwordFile = Join-Path $local 'pg-password.tmp'
    [IO.File]::WriteAllText($passwordFile, 'SportSchoolLocal2026')
    try {
        & "$bin\initdb.exe" -D $data -U sportschool -A scram-sha-256 --pwfile=$passwordFile --encoding=UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo inicializar PostgreSQL.' }
    } finally { Remove-Item $passwordFile -ErrorAction SilentlyContinue }
    Add-Content "$data\postgresql.conf" "`nport = 55432`nlisten_addresses = 'localhost'"
}
& "$bin\pg_ctl.exe" status -D $data *> $null
if ($LASTEXITCODE -ne 0) {
    & "$bin\pg_ctl.exe" start -D $data -l "$local\postgres.log" -w
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar PostgreSQL.' }
}
$env:PGPASSWORD = 'SportSchoolLocal2026'
$exists = & "$bin\psql.exe" -h localhost -p 55432 -U sportschool -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='sportschool'"
if ($exists -ne '1') { & "$bin\createdb.exe" -h localhost -p 55432 -U sportschool sportschool }
Remove-Item Env:PGPASSWORD
Write-Host 'PostgreSQL disponible en localhost:55432 / sportschool'
