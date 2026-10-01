param([string]$IPLocal = "")

$ErrorActionPreference = 'Stop'
$paquete = Split-Path -Parent $PSScriptRoot
$secrets = Join-Path $paquete 'api\secrets'
New-Item -ItemType Directory -Path $secrets -Force | Out-Null
$keystore = Join-Path $secrets 'api-keystore.p12'
$segura = Read-Host 'Contraseña nueva para el certificado HTTPS (mínimo 12 caracteres)' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
try { $contrasena = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
if ($contrasena.Length -lt 12) { throw 'La contraseña del certificado debe tener al menos 12 caracteres.' }
$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) { throw 'No se encontró keytool. Instala y configura un JDK 17 o posterior.' }
$san = 'SAN=dns:localhost,ip:127.0.0.1,ip:10.0.2.2'
if ($IPLocal) { $san += ",ip:$IPLocal" }
& $keytool.Source -genkeypair -alias 'paqteria-api' -keyalg RSA -keysize 3072 -validity 365 -storetype PKCS12 -keystore $keystore -storepass $contrasena -keypass $contrasena -dname 'CN=localhost, OU=Desarrollo, O=PAQTERIA, C=MX' -ext $san -noprompt
if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el certificado PKCS12.' }
$env:API_TLS_KEYSTORE_PASSWORD = $contrasena
Write-Host "Certificado HTTPS creado en $keystore. La contraseña quedó en la variable de entorno de esta sesión."
