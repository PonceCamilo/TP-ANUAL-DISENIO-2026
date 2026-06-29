## Variables de entorno requeridas

Configurar antes de levantar n8n:

```powershell
$env:DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/..."
n8n start
```

## Payload que envia el backend

```json
{
  "donanteId": "uuid",
  "insigniaId": "uuid",
  "insigniaNombre": "Semilla de Solidaridad",
  "insigniaImagen": "semilla.png",
  "fechaObtencion": "2026-06-24",
  "destinatario": "mail o usuario"
}
```

## Respuesta al backend

```json
{
  "status": "ok",
  "destino": "discord",
  "imagenGenerada": true
}
```

Si n8n falla, el backend de incentivos no bloquea la entrega de la insignia: registra el error en logs y sigue.
