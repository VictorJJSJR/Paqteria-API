$ErrorActionPreference = 'Stop'
$suite = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $suite '.env'
if (Test-Path -LiteralPath $envFile) {
    Get-Content -LiteralPath $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            $nombre = $parts[0].Trim()
            $valorArchivo = $parts[1].Trim()
            if (-not [Environment]::GetEnvironmentVariable($nombre, 'Process') -and $valorArchivo) {
                [Environment]::SetEnvironmentVariable($nombre, $valorArchivo, 'Process')
            }
        }
    }
}
if (-not $env:API_TLS_KEYSTORE_PASSWORD -or -not $env:PAQTERIA_JWT_SECRETO_BASE64) {
    throw 'Configura API_TLS_KEYSTORE_PASSWORD y PAQTERIA_JWT_SECRETO_BASE64 en .env o en esta sesión.'
}
$javaVersion = (& java -version 2>&1 | Out-String)
$coincidencia = [regex]::Match($javaVersion, 'version "([^"]+)"')
if (-not $coincidencia.Success) { throw 'No se pudo determinar la versión de Java instalada.' }
$segmentos = $coincidencia.Groups[1].Value.Split('.')
$mayor = if ($segmentos[0] -eq '1') { [int]$segmentos[1] } else { [int]$segmentos[0] }
if ($mayor -lt 17) { throw "La API requiere JDK 17 o posterior; se encontró Java $mayor." }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Instala Maven 3.6.3 o posterior, o utiliza docker compose up --build api.' }
Push-Location (Join-Path $suite 'api')
try { mvn spring-boot:run }
finally { Pop-Location }
