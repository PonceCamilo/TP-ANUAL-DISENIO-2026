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
- `SPRING_DOCKER_COMPOSE_ENABLED`: `false` (los repositorios son en memoria).
