# Docker - Servicio de Logistica

Este compose levanta unicamente el servicio de logistica en el puerto `8085`.

Desde `donatrack/servicio-logistica`:

```bash
docker compose up --build
```

Endpoints utiles:

- Swagger UI: `http://localhost:8085/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

## Variables de entorno

El compose sobreescribe la configuracion por defecto (`application.properties`) via
variables de entorno:

- `SERVER_PORT`: puerto del servicio (8085).
- `INTEGRACIONES_PROVEEDOR_RUTEO_URL`: URL del proveedor de ruteo (apunta al mock interno). Se llama una vez por camión y responde de forma síncrona con la ruta planificada.
- `INTEGRACIONES_BROKER_EVENTOS_URL`: endpoint del broker de logistica al que se publican los
  eventos de entrega. En el `docker-compose.yml` de `donatrack/` apunta a
  `http://servicio-broker-logistica:8087/api/broker/eventos/donatrack`; si el broker corre en
  la maquina host, usar `host.docker.internal`.
- `SPRING_DOCKER_COMPOSE_ENABLED`: `false` (la base se levanta como servicio del compose, no desde Spring).
- `LOGISTICA_DB_URL`, `LOGISTICA_DB_USER`, `LOGISTICA_DB_PASSWORD`: conexión a la base propia del
  servicio. En el compose apuntan al contenedor `logistica-db` (PostgreSQL 16); sin estas
  variables se usa `jdbc:postgresql://localhost:5433/logistica_db` (usuario y clave `logistica`),
  que es el mismo contenedor publicado en el host.

## Base de datos

`servicio-logistica` tiene su propia base (`logistica_db`): ningún otro servicio accede a ella.
El esquema sigue el DER de Logística del grupo, con claves primarias UUID. Los datos de
servicio-donaciones (donación, entidad beneficiaria) no se copian: las tablas `donacion` y
`entidad_beneficiaria` guardan solo el id, para que las foreign keys de logística sean válidas,
y logística las completa sola al recibir cada donación.

- Esquema: migraciones de Flyway en `src/main/resources/db/migration`. Para cambiar el esquema
  se agrega un archivo nuevo (`V2__descripcion.sql`); las migraciones ya aplicadas no se editan.
- Al arrancar, Hibernate valida (`ddl-auto=validate`) que las entidades coincidan con las tablas.
- Los datos persisten en el volumen `logistica-db-data`; `docker compose down -v` los borra.
