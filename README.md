# Sport School — gestión de canchas y reservas

Aplicación local con **React + TypeScript + Tailwind**, **Spring Boot** y **PostgreSQL**. Web pública y panel interno comparten una API. Los datos se conservan en PostgreSQL al reiniciar.

## Abrir la aplicación

- Web: **http://127.0.0.1:5173**
- Panel: **http://127.0.0.1:5173/#admin**
- Backend: http://localhost:8080/api/public/catalog

| Usuario | Contraseña inicial | Acceso |
|---|---|---|
| `admin` | `SportSchool2026!` | Administración, ambas sedes |
| `operador` | `Canchas2026!` | Atención, ambas sedes |

Las contraseñas se almacenan con BCrypt. `ADMIN_PASSWORD` y `OPERATOR_PASSWORD` permiten elegir las contraseñas **antes del primer inicio**; no cambian cuentas ya existentes.

## Requisitos

- Java 21 o superior (verificado con Java 25).
- Maven 3.9 o superior; el script también encuentra Maven en `.m2/wrapper/dists`.
- Node.js 22 o superior y npm.
- PostgreSQL 18, o Docker Desktop activo para levantar solamente la base.

## Iniciar en Windows

Desde la raíz, en tres terminales:

**1. Base de datos local**

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-db.ps1
```

Usa la instalación `C:\Program Files\PostgreSQL\18\bin`, crea un clúster independiente en `.local/pgdata`, escucha solamente en localhost:55432 y no modifica otras bases instaladas.

Alternativa con Docker, en lugar del script anterior:

```powershell
docker compose up -d db
```

**2. Backend**

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```

O `mvn spring-boot:run` desde `backend/`. Flyway aplica las migraciones automáticamente.

**3. Frontend**

```powershell
cd frontend
npm install
npm run dev
```

Utiliza **127.0.0.1** para abrir el frontend. Vite reenvía `/api` al backend; no hace falta configurar CORS.

## Configuración de PostgreSQL

Valores locales por defecto:

```text
DATABASE_URL=jdbc:postgresql://localhost:55432/sportschool
DATABASE_USER=sportschool
DATABASE_PASSWORD=SportSchoolLocal2026
PORT=8080
```

Para otra base, configura esas variables en la terminal del backend. No subas `.local/`, contraseñas de producción ni comprobantes al repositorio.

## Funcionalidades implementadas

- Página pública con fotografías y logo de Sport School guardados localmente.
- Dos sedes, quince configuraciones alquilables y nueve sectores físicos.
- Horarios semanales, tarifas por hora y lectura de tarifas especiales por fecha.
- Grilla con precios, horas consecutivas y total calculado por el servidor.
- Registro público sin cuenta ni DNI y enlace a WhatsApp con código de solicitud.
- Autenticación mediante sesión, protección CSRF y autorización por sede.
- Solicitudes nuevas, en conversación, pendientes de pago, confirmadas, sin disponibilidad y desistidas.
- Edición, notas, ajuste de importe con motivo e historial.
- Atención directa desde el panel y consulta del historial del cliente.
- Verificación del pago completo y confirmación transaccional de reserva.
- Cierre automático de solicitudes que comparten sectores y horarios.
- Reapertura de solicitudes cerradas para ofrecer otro horario.

## Verificación

```powershell
cd frontend
npm run build
```

Las pruebas de integración usan una **base independiente** `sportschool_test` y limpian sus tablas operativas. Nunca apuntar `TEST_DATABASE_URL` a la base de trabajo.

```powershell
$env:PGPASSWORD='SportSchoolLocal2026'
& 'C:\Program Files\PostgreSQL\18\bin\createdb.exe' -h localhost -p 55432 -U sportschool sportschool_test
cd backend
mvn test
```

## Documentación

- [Guía del usuario](docs/GUIA_USUARIO.md)
- [Guía del desarrollador y modelo de datos](docs/DESARROLLO.md)
- [Alcance y próximas iteraciones](docs/ALCANCE.md)

## Detener

Detén frontend y backend con `Ctrl+C` en sus terminales. Para la base local:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' stop -D '.local/pgdata' -m fast
```

Con Docker: `docker compose stop`. No borres el directorio de datos ni el volumen si quieres conservar las reservas.
