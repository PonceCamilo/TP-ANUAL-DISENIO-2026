# Integración n8n - Evento Logística

Este flujo se dispara desde `servicio-logistica` cada vez que ocurre un hecho de logística
(`INICIO_RUTA`, `ENTREGA_CONFIRMADA`, `ENTREGA_NO_RECIBIDA`). El backend no llama a
`servicio-notificaciones` directamente: solo publica el evento en n8n
(`N8nLogisticaWebhookListener`, vía el webhook configurado) y es el flujo de n8n el que
decide cómo notificar.

Cuando una entrega **falla** (`ENTREGA_NO_RECIBIDA`), el backend agrega la bandera
`requiereAvisoAdmin: true` al payload, y este flujo envía un correo de alerta a un
administrador.

## Importar el workflow

1. En n8n: **Workflows → Import from File** y seleccionar `logistica-evento.workflow.json`.
2. Activar el workflow (toggle **Active**).
3. Verificar que la URL del nodo **Webhook - Evento Logistica** (modo *Production*) coincida
   con `integraciones.n8n.webhook.url` del backend
   (`servicio-logistica/src/main/resources/application.properties`,
   por defecto `http://localhost:5678/webhook/logistica-evento`).

## Payload que envía el backend

```json
{
  "tipo": "ENTREGA_NO_RECIBIDA",
  "entregaId": "uuid",
  "idDonacion": "uuid",
  "idEntidadBeneficiaria": "uuid",
  "idDonante": "uuid",
  "camionId": "uuid",
  "rutaId": "uuid",
  "fotosComprobante": ["..."],
  "motivo": "texto del motivo (solo en ENTREGA_NO_RECIBIDA)",
  "requiereAvisoAdmin": true
}
```

`requiereAvisoAdmin` es `true` solo cuando `tipo` es `ENTREGA_NO_RECIBIDA`; en el resto de
los eventos es `false`.

## Nodos del flujo

- **Webhook - Evento Logistica**: recibe el POST del backend en `/webhook/logistica-evento`.
- **Normalizar payload** (`set`): extrae los campos del `body` a la raíz del item.
- **Requiere aviso admin?** (`if`): evalúa `requiereAvisoAdmin`. La salida **true** (superior)
  dispara el aviso al administrador; la salida **false** (inferior) solo responde al backend.
- **Email aviso a administrador (simulado)** (`set`): simula el envío del correo de alerta.
  No requiere credenciales: arma los campos `para`, `asunto` y `cuerpo` y los deja visibles en
  la pestaña **Executions**, lo que alcanza para demostrar el flujo. Para enviar un correo real,
  reemplazar este nodo por uno de **Send Email (SMTP)** o **Gmail** y configurar su credencial
  (**Credentials → New**); para probar en local se puede usar [Mailtrap](https://mailtrap.io).
- **Responder al backend** (`respondToWebhook`): devuelve `{"status":"ok","avisoAdmin":...}`.
  El backend no bloquea el flujo de logística si esta llamada falla (ver
  `N8nLogisticaWebhookListener`, que loguea el error y continúa).

## Probar en local

1. Levantar n8n: `npx n8n` (o Docker) en `http://localhost:5678`, importar y activar el flujo.
2. Levantar el backend:

   ```bash
   cd servicio-logistica
   mvn spring-boot:run
   ```

   Al arrancar, `DatosDemoLogistica` carga una entrega de prueba en estado `EN_TRASLADO`
   con id fijo `11111111-1111-1111-1111-111111111111` (solo para pruebas locales).
3. Simular que la entrega falla (Postman u otro cliente HTTP):

   - **POST** `http://localhost:8085/api/logistica/entregas/11111111-1111-1111-1111-111111111111/no-recibida`
   - Header: `Content-Type: application/json`
   - Body:

     ```json
     { "motivo": "La entidad beneficiaria no se encontraba en el domicilio" }
     ```

4. Resultado esperado: `200 OK` del backend, y en la pestaña **Executions** del workflow una
   ejecución con `requiereAvisoAdmin: true` y el nodo de email ejecutado.

## Notas

- El backend no espera la respuesta de n8n de forma síncrona: si n8n está caído, la entrega
  igual queda marcada como `NO_RECIBIDA` y solo se loguea el error del webhook.
- `DatosDemoLogistica` es solo para pruebas locales (repositorios en memoria). No debe usarse
  como mecanismo de carga de datos en un entorno real.
