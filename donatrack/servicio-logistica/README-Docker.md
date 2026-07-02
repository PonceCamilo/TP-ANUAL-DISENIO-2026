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
- `SERVICIO_LOGISTICA_BASE_URL`: URL base usada para armar el callback del proveedor de ruteo.
- `INTEGRACIONES_PROVEEDOR_RUTEO_URL`: URL del proveedor de ruteo (apunta al mock interno).
- `INTEGRACIONES_N8N_WEBHOOK_URL`: webhook de n8n. Usa `host.docker.internal` para llegar a
  un n8n corriendo en la maquina host.
- `SPRING_DOCKER_COMPOSE_ENABLED`: `false` (los repositorios son en memoria).
