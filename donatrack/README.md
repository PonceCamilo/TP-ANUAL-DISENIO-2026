# DonaTrack

Sistema de Gestión y Trazabilidad de Donaciones — Trabajo Práctico Anual, arquitectura distribuida en microservicios (Spring Boot 3.4.5 / Java 21).

## Servicios

| Servicio | Puerto | Módulo |
|---|---|---|
| Servicio de Donaciones | 8081 | `servicio-donaciones` |
| Servicio de Incentivos | 8083 | `servicio-incentivos` |
| Servicio de Notificaciones | 8084 | `servicio-notificaciones` |
| Servicio de Logística | 8085 | `servicio-logistica` |

## Levantar el sistema

```
docker compose up --build
```

## Documentación Swagger / OpenAPI

Cada servicio expone su propia UI de Swagger una vez levantado:

- Donaciones: http://localhost:8081/swagger-ui/index.html
- Incentivos: http://localhost:8083/swagger-ui/index.html
- Notificaciones: http://localhost:8084/swagger-ui/index.html
- Logística: http://localhost:8085/swagger-ui/index.html
