# Integración n8n - Evento Logística (relay / enrutador)

Este flujo se dispara desde `servicio-logistica` cada vez que ocurre un hecho de logística
(`INICIO_RUTA`, `ENTREGA_CONFIRMADA`, `ENTREGA_NO_RECIBIDA`). El backend de logística no llama
a Donaciones ni a Notificaciones directamente: solo publica el evento en n8n
(`N8nLogisticaWebhookListener`, vía el webhook configurado).

## Rol de n8n (Diseño A - Donaciones orquesta)

n8n es un **relay/enrutador puro**: recibe el evento, mira el campo `tipo` y reenvía el body
**tal cual** al endpoint correspondiente del Servicio de Donaciones. n8n **no** consulta
contactos ni envía mails; toda la orquestación de notificaciones (donante, entidad,
administrador, incentivos) la hace Donaciones en `LogisticaEventosService`.

```
Logística → webhook n8n → Switch por `tipo` → POST a Donaciones → Donaciones notifica
```

Mapa de ruteo:

| `tipo`                | Endpoint de Donaciones                              |
|-----------------------|-----------------------------------------------------|
| `INICIO_RUTA`         | `POST http://localhost:8081/logistica/eventos/inicio-ruta`    |
| `ENTREGA_CONFIRMADA`  | `POST http://localhost:8081/logistica/eventos/entrega-exitosa`|
| `ENTREGA_NO_RECIBIDA` | `POST http://localhost:8081/logistica/eventos/entrega-fallida`|

## Importar el workflow

1. En n8n: **Workflows → Import from File** y seleccionar `logistica-evento.workflow.json`.
2. Activar/publicar el workflow (toggle **Active** / botón **Publish**).
3. Verificar que la URL del nodo **Webhook - Evento Logistica** (modo *Production*) coincida
   con `integraciones.n8n.webhook.url` del backend
   (`servicio-logistica/src/main/resources/application.properties`,
   por defecto `http://localhost:5678/webhook/logistica-evento`).

## Payloads que envía el backend

Los nombres de campo coinciden con los DTOs de Donaciones, por eso n8n reenvía el `body`
sin transformarlo. `fechaHoraEntrega` viaja como string ISO-8601 local (`LocalDateTime`).

```json
// INICIO_RUTA (un evento por ruta, agrupando todas sus donaciones)
{
  "tipo": "INICIO_RUTA",
  "idRuta": "<uuid>",
  "idsDonaciones": ["<uuid>", "<uuid>"],
  "urlMapaInteractivo": "https://donatrack.org/tracking/ruta/<idRuta>"
}
```

```json
// ENTREGA_CONFIRMADA → /entrega-exitosa (rutaId es solo observabilidad; Donaciones lo ignora)
{
  "tipo": "ENTREGA_CONFIRMADA",
  "rutaId": "<uuid>",
  "idDonacion": "<uuid>",
  "idCamion": "<uuid>",
  "patenteCamion": "AB123CD",
  "fechaHoraEntrega": "2026-07-02T18:30:00"
}
```

```json
// ENTREGA_NO_RECIBIDA → /entrega-fallida
{
  "tipo": "ENTREGA_NO_RECIBIDA",
  "rutaId": "<uuid>",
  "idDonacion": "<uuid>",
  "motivoFallo": "ENTIDAD_AUSENTE",
  "replanificable": true
}
```

## Nodos del flujo

- **Webhook - Evento Logistica**: recibe el POST del backend en `/webhook/logistica-evento`.
- **Enrutar por tipo** (`switch`): 3 salidas según `{{$json.body.tipo}}`.
- **POST Donaciones /inicio-ruta | /entrega-exitosa | /entrega-fallida** (`httpRequest`):
  reenvían `{{$json.body}}` al endpoint de Donaciones correspondiente.
- **Responder al backend** (`respondToWebhook`): devuelve `{"status":"ok"}`. El backend no
  bloquea el flujo de logística si esta llamada falla (ver `N8nLogisticaWebhookListener`).

## Cambiar la URL base de Donaciones

Si Donaciones no corre en `localhost:8081`, editar la propiedad **URL** de los tres nodos
`POST Donaciones ...` (por ejemplo si lo levantás en otra máquina o puerto, o dentro de Docker
donde `localhost` no resuelve al host). Solo cambia el host/puerto; las rutas
(`/logistica/eventos/...`) se mantienen.

## Probar en local

1. Levantar n8n: `npx n8n` (o Docker) en `http://localhost:5678`, importar y activar el flujo.
2. Levantar **Donaciones** (`:8081`), **Notificaciones** (`:8084`), **Incentivos** (`:8083`)
   y **Logística** (`:8085`).
3. Disparar un evento desde Logística (ver `docs/GUIA-PRUEBAS.md`). n8n reenviará el body a
   Donaciones y Donaciones disparará las notificaciones.

## Notas

- El backend no espera la respuesta de n8n de forma síncrona: si n8n está caído, la entrega
  igual queda marcada y solo se loguea el error del webhook.
- `DatosDemoLogistica` es solo para pruebas locales (repositorios en memoria). No debe usarse
  como mecanismo de carga de datos en un entorno real.
