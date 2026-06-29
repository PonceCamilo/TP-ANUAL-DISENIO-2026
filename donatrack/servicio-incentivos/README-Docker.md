# Docker - Servicio de Incentivos

Este compose levanta unicamente el servicio de incentivos en el puerto `8083`.

Desde `donatrack/servicio-incentivos`:

```bash
docker compose up --build
```

Endpoints utiles:

- Swagger UI: `http://localhost:8083/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`
