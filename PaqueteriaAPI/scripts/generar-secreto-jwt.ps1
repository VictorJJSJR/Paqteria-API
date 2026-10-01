$ErrorActionPreference = 'Stop'
$bytes = New-Object byte[] 32
$generador = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $generador.GetBytes($bytes) }
finally { $generador.Dispose() }
$env:PAQTERIA_JWT_SECRETO_BASE64 = [Convert]::ToBase64String($bytes)
[Array]::Clear($bytes, 0, $bytes.Length)
Write-Host 'Secreto JWT aleatorio creado en PAQTERIA_JWT_SECRETO_BASE64 para esta sesión.'
