# Autenticación de DonaTrack

El frontend conserva el JWT de DonaTrack en la sesión del servidor y lo envía como
`Authorization: Bearer <token>` a las APIs. La clave pública de verificación está
en `GET /api/autenticacion/claves-publicas`. Los tokens tienen emisor
`urn:donatrack:autenticacion`, audiencia `donatrack-api` y vigencia de una hora.

## Levantar en local

En la configuración local de **ambos** proyectos, establecer el mismo
`AUTH_FRONTEND_KEY` de al menos 32 caracteres. Se usa
entre servidores cuando el frontend vincula el perfil creado en Donaciones y
para los enlaces de activación. Es obligatoria: sin ella el servicio no arranca
y el log dice qué falta.

En el proyecto backend, configurar la cuenta inicial con
`AUTH_ADMINISTRADOR_EMAIL` y `AUTH_ADMINISTRADOR_PASSWORD`. El arranque crea
esa cuenta si aún no existe. Desde el panel de Administración se crean las
cuentas de los choferes. Estas variables se guardan en el entorno local o en
un `.env` excluido de Git. Las contraseñas se almacenan con BCrypt.

Desde `donatrack`:

```
docker compose up -d --build servicio-autenticacion
```

Autenticación escucha en el puerto 8088. 

El acceso con email y contraseña funciona sin configurar Google. La Entrega 6
pide mostrar un ingreso social funcionando. Para demostrar el proveedor
implementado, configurar el mismo `GOOGLE_CLIENT_ID` en los dos proyectos y
`GOOGLE_CLIENT_SECRET` únicamente en el frontend. Para local, registrar
`http://localhost:8080/login/oauth2/code/google` como URI de redirección.

## Cuentas de las personas que cargó la administración (requisito 20)

Cuando la administración importa donantes por CSV, o da de alta uno a mano,
Donaciones avisa a este servicio y cada persona recibe una cuenta **vinculada a
su perfil y sin contraseña**. La persona la activa desde el front (`/activar`):
pone su email, recibe un enlace por correo y elige su contraseña.

| Endpoint | Quién lo llama | Qué hace |
|---|---|---|
| `POST /api/autenticacion/cuentas/importadas` | Donaciones, con `X-DonaTrack-Servicio` | Crea las cuentas. Idempotente por email; responde `creadas`, `existentes` y `rechazadas` con el motivo |
| `POST /api/autenticacion/activaciones` | El front, con `X-DonaTrack-Frontend` | Manda el enlace si la cuenta existe. Responde 202 siempre, sin revelar si el email está registrado |
| `POST /api/autenticacion/activaciones/confirmar` | El front, con `X-DonaTrack-Frontend` | Guarda la contraseña y devuelve la sesión. 404 enlace desconocido o reemplazado, 409 ya usado, 410 vencido |

Decisiones:

- Una cuenta que ya existía con ese email y no está vinculada a esa persona no
  se toca: sería entregarle el perfil a quien se registró primero sin probar que
  el email es suyo.
- El código del enlace es aleatorio (256 bits) y se guarda solo su hash. Vence a
  las 24 horas (`donatrack.auth.activacion.vigencia`), sirve una vez, pedir otro
  borra los anteriores y entre dos pedidos de la misma cuenta pasa un minuto.
- El correo lo manda este servicio y no Notificaciones: Notificaciones guarda el
  texto de cada mensaje y lo devuelve en su API, y el enlace permite elegir la
  contraseña de la cuenta.
- El mismo camino sirve para recuperar el acceso de un donante o una entidad.

Variables:

| Variable | Para qué |
|---|---|
| `DONATRACK_SERVICIOS_KEY` | Clave entre servicios (al menos 32 caracteres), la misma que usa Donaciones. Sin ella se rechazan los avisos |
| `FRONTEND_URL` | Dirección pública del front para armar el enlace (por defecto `http://localhost:8080`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP. Sin `MAIL_HOST` no sale ningún correo. Con Gmail hace falta una contraseña de aplicación |
| `AUTH_ENLACE_EN_LOG` | `true` muestra el enlace en el log cuando no hay SMTP. Solo para probar en local |
