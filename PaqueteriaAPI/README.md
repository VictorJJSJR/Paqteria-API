# API PAQTERIA

Este paquete contiene únicamente el servidor API Java de PAQTERIA y los archivos necesarios para configurarlo y ejecutarlo. La API conecta los clientes web y móvil con SQL Server mediante HTTPS y JWT; sus endpoints, propiedades JSON y mensajes están principalmente en español.

## Contenido

- `api/`: proyecto Java con Spring Boot, seguridad JWT, TLS integrado, controladores, servicios y acceso JDBC a SQL Server.
- `scripts/`: generación del certificado HTTPS y secreto JWT e inicio local en Windows.
- `.env.example`: plantilla de configuración.
- `compose.yaml`: ejecución del contenedor API; SQL Server se configura por separado.

No incluye aplicaciones web/móvil ni los scripts de base de datos. La base `Paqteria` y las 11 tablas del esquema proporcionado deben existir antes de iniciar la API. La API agrega los roles requeridos y puede crear una cuenta administradora inicial.

## Requisitos

- JDK 17 o posterior y Maven 3.6.3 o posterior, o Docker Desktop.
- SQL Server con el esquema PAQTERIA cargado y un usuario con permisos de lectura/escritura.

## Inicio local en Windows

1. Copia `.env.example` a `.env` y completa `PAQTERIA_DB_URL`, `PAQTERIA_DB_USUARIO` y `PAQTERIA_DB_CONTRASENA`. Para crear al administrador inicial, define `PAQTERIA_ADMIN_CORREO` y `PAQTERIA_ADMIN_CONTRASENA` (mínimo 12 caracteres).
2. Desde PowerShell, en la raíz de este paquete, genera los secretos. Añade `-IPLocal '192.168.1.25'` al crear el certificado si otros dispositivos se conectarán por la red local.

   ```powershell
   . .\scripts\generar-secreto-jwt.ps1
   . .\scripts\generar-certificado.ps1
   ```

   Conserva la misma terminal: las contraseñas quedan en variables de entorno de esa sesión. El almacén PKCS12 queda en `api/secrets/` y está excluido del control de versiones.

3. Inicia la API:

   ```powershell
   .\scripts\iniciar-api.ps1
   ```

   El servicio escucha en `https://localhost:8443`. La API exige certificado, secreto JWT y credenciales de SQL Server; no inicia HTTP sin TLS.

## Inicio con Docker

Completa `.env`, genera `api/secrets/api-keystore.p12` y el secreto JWT, y luego ejecuta:

```powershell
docker compose up --build api
```

El contenedor ejecuta solo la API. SQL Server debe estar accesible desde el host indicado en `PAQTERIA_DB_URL` (por defecto, `host.docker.internal`).

## Autenticación y rutas

Excepto `GET /api/salud` y `POST /api/autenticacion/iniciar-sesion`, las rutas requieren `Authorization: Bearer <token>`.

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/salud` | Estado de la API y SQL Server |
| POST | `/api/autenticacion/iniciar-sesion` | Emite token JWT |
| GET | `/api/autenticacion/yo` | Usuario autenticado |
| GET | `/api/resumen` | Indicadores del día |
| GET, POST | `/api/paquetes` | Consulta y registro de paquetes |
| GET | `/api/paquetes/{id}` | Detalle e historial |
| GET, POST | `/api/rutas/actuales`, `/api/rutas` | Consulta y asignación de turnos y paradas |
| PATCH | `/api/rutas/{idTurno}/estado` | Actualiza estado del turno |
| GET, POST | `/api/repartidores` | Consulta repartidores y alta de cuentas (administrador) |
| GET, POST | `/api/incidencias` | Consulta y registra incidencias |
| POST | `/api/entregas` | Confirma entrega y guarda comprobante |
| GET | `/api/reportes/diario` | Reporte operativo diario |
| GET | `/api/catalogos/paquetes`, `/api/catalogos/rutas` | Clientes, centros, unidades y paquetes asignables |

Los roles son `ADMINISTRADOR`, `OPERADOR`, `REPARTIDOR` y `CLIENTE`. Los administradores y operadores gestionan la operación; el repartidor accede a sus turnos y solo registra entregas/incidencias de paquetes asignados.

## Seguridad de desarrollo y producción

El certificado generado localmente es autofirmado: configura los clientes móviles para confiar en él durante desarrollo. En producción usa un certificado emitido por una autoridad confiable, limita `PAQTERIA_CORS_ORIGENES`, establece `trustServerCertificate=false` para JDBC y administra secretos fuera de `.env`. No compartas `.env` ni `api/secrets/`.
