# Autenticación de DonaTrack

El frontend conserva el JWT de DonaTrack en la sesión del servidor y lo envía como
`Authorization: Bearer <token>` a las APIs. La clave pública de verificación está
en `GET /api/autenticacion/claves-publicas`. Los tokens tienen emisor
`urn:donatrack:autenticacion`, audiencia `donatrack-api` y vigencia de una hora.

## Levantar en local

En la configuración local de **ambos** proyectos, establecer el mismo
`AUTH_FRONTEND_KEY` de al menos 32 caracteres. Se usa
entre servidores cuando el frontend vincula el perfil creado en Donaciones.

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
