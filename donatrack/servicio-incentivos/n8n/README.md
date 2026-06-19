# Integración n8n - Insignia Obtenida

Este flujo se dispara desde `servicio-incentivos` cuando un donante completa una misión y
recibe una insignia (`IncentivosServiceImpl.verificarMisionActiva`, vía `N8nWebhookClient`).
Se encarga de generar la imagen de la insignia y publicarla en la red social elegida,
mencionando al donante.

## Importar el workflow

1. En n8n: **Workflows → Import from File** y seleccionar `insignia-obtenida.workflow.json`.
2. Activar el workflow (toggle "Active").
3. Copiar la URL del nodo **Webhook - Insignia Obtenida** (modo *Production*) y configurarla
   en el backend como `integraciones.n8n.webhook.url` (ver
   `servicio-incentivos/src/main/resources/application.properties`).

## Payload que envía el backend

```json
{
  "donanteId": "uuid",
  "insigniaId": "uuid",
  "insigniaNombre": "Primera donación",
  "insigniaImagen": "ruta o id de plantilla de la insignia",
  "fechaObtencion": "2026-06-18",
  "destinatario": "usuario o contacto a mencionar"
}
```

## Nodos a configurar antes de pasar a producción

- **Generar imagen de la insignia** (`httpRequest`): apunta a `IMAGEN_INSIGNIA_API_URL`
  (variable de entorno de n8n). Configurar ahí el servicio elegido para renderizar la
  imagen (p. ej. Bannerbear, Cloudinary, Canva API) usando `insigniaImagen` como plantilla
  y `insigniaNombre` como texto.
- **Publicar en red social** (`twitter` en el template, placeholder): reemplazar por el
  nodo correspondiente a la red social elegida (Twitter/X, Instagram, LinkedIn, etc.) y
  configurar las credenciales OAuth de la cuenta de DonaTrack en n8n
  (**Credentials → New**). El mensaje ya incluye la mención al `destinatario` y el nombre
  de la insignia.
- **Responder al backend** (`respondToWebhook`): devuelve `{"status":"ok"}`; el backend no
  bloquea el flujo de incentivos si esta llamada falla (ver `N8nWebhookClient`, que loguea
  el error y continúa).

## Notas

- El backend no espera la respuesta de n8n de forma síncrona para nada crítico: si n8n está
  caído, el otorgamiento de la insignia y las notificaciones internas igual se completan.
- Para probar en local, levantar n8n (`npx n8n` o Docker) en `http://localhost:5678` y dejar
  la URL default del webhook (`/webhook/insignia-obtenida`), que coincide con el valor por
  defecto de `integraciones.n8n.webhook.url`.
