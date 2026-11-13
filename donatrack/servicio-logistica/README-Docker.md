# Docker - Servicio de Logistica

El compose del proyecto está en `donatrack/docker-compose.yml`.
Desde esa carpeta, este comando levanta MongoDB y el servicio de logística en el puerto `8085`:

```bash
cd donatrack
docker compose up --build servicio-logistica
```

Endpoints utiles:

- Swagger UI: `http://localhost:8085/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

## Variables de entorno

El compose sobreescribe la configuracion por defecto (`application.properties`) via
variables de entorno:

- `SERVER_PORT`: puerto del servicio (8085).
- `INTEGRACIONES_PROVEEDOR_RUTEO_URL`: URL del proveedor de ruteo (apunta al mock interno). Se llama una vez por lote y responde de forma síncrona con las rutas de los camiones usados.
- `INTEGRACIONES_BROKER_EVENTOS_URL`: endpoint del broker de logistica al que se publican los
  eventos de entrega. En el `docker-compose.yml` de `donatrack/` apunta a
  `http://servicio-broker-logistica:8087/api/broker/eventos/donatrack`; si el broker corre en
  la maquina host, usar `host.docker.internal`.
- `SPRING_DOCKER_COMPOSE_ENABLED`: `false` (MongoDB se levanta como servicio del compose, no desde Spring).
- `SPRING_DATA_MONGODB_URI`: MongoDB de logística (base `logistica`, colecciones `camiones`, `lotes` y `rutas`).
  En el compose apunta al servicio `mongo`.
- `AUTH_CLAVES_PUBLICAS_URL`: claves públicas del Servicio de Autenticación para validar el JWT
  (Entrega 6). Se piden con el primer token, así que Logística arranca aunque Autenticación no esté.
- `DONATRACK_SERVICIOS_KEY`: clave entre servicios (al menos 32 caracteres). La manda el broker
  en `X-DonaTrack-Servicio` para planificar; tiene que ser la misma en los dos.

## Seguridad (Entrega 6)

Cada pedido lleva `Authorization: Bearer <JWT>` del Servicio de Autenticación. Se decide por el
claim `rol`, y una entidad beneficiaria solo ve y confirma las entregas de su entidad (claim
`perfilId` contra la parada). El callback de planificación (`/api/logistica/planificaciones/callback`)
y el proveedor simulado (`/ruteo/planificar`) tienen su propia cadena de seguridad, sin JWT: el
callback lo llama un proveedor externo, que se autentica con el token de correlación del lote.
Ver `config/SeguridadConfig.java` y `docs/GUIA-PRUEBAS.md`.

## Base de datos

Logística usa solo MongoDB (`donatrack-mongo`, puerto `27017`, base `logistica`). Ahí están las colecciones `camiones`, `lotes` y `rutas`. Las paradas, entregas, direcciones, historial y fotos van embebidos en la ruta. Las donaciones del pedido van embebidas en el lote.

Postgres sigue en el compose para donaciones, incentivos y notificaciones. Logística no se conecta a él.

```bash
cd donatrack
docker compose up -d mongo
```

Los ids de donaciones y de entidades beneficiarias se guardan como UUID, sin relación hacia servicio-donaciones.

Los datos persisten en el volumen `datos-mongo`. `docker compose down -v` desde `donatrack/` borra también el Postgres de los otros servicios y RabbitMQ. Para vaciar solo logística, borrar el volumen `datos-mongo`.
