# Guía de pruebas - servicio-logistica + n8n

Esta guía explica, paso a paso, cómo levantar el servicio de logística y n8n en una
máquina local y probar dos flujos:

- **Prueba A:** evento `ENTREGA_NO_RECIBIDA` -> webhook a n8n -> n8n reenvía a Donaciones
  (`/logistica/eventos/entrega-fallida`) -> Donaciones notifica a entidad, donante y admin.
- **Prueba B:** flujo de planificación de rutas contra el proveedor externo *mock*
  (sin `Connection Refused`).

> **Arquitectura (Diseño A):** n8n es un relay/enrutador puro. Recibe el evento de Logística,
> mira `tipo` y reenvía el body a Donaciones. Es **Donaciones** quien resuelve los contactos y
> dispara las notificaciones. Por eso la Prueba A ahora requiere **Donaciones corriendo en
> `:8081`** (y, para ver mails, **Notificaciones en `:8084`** e **Incentivos en `:8083`**).
---

## 0. Requisitos previos

Instalar en la máquina:

- **Java 17+** y **Maven** (para correr el backend con `mvn`).
- **Node.js** (trae `npx`, que usamos para levantar n8n).
- **Postman** (cliente para enviar peticiones HTTP). Alternativa: cualquier cliente REST.

Clonar/abrir el repo del TP. Todos los comandos asumen que estás parado en la carpeta
del servicio: `donatrack/servicio-logistica`.

Para la Prueba A end-to-end, además de Logística tenés que levantar los servicios que
orquestan la notificación:

- **servicio-donaciones** (`:8081`) — recibe el evento reenviado por n8n y notifica.
- **servicio-notificaciones** (`:8084`) — envía (simula) los mails/SMS.
- **servicio-incentivos** (`:8083`) — recibe el aviso de donación exitosa.

Cada uno se levanta igual que logística: `cd donatrack/servicio-<nombre>` y `mvn spring-boot:run`.

---

## 1. Levantar n8n e importar el workflow

1. En una terminal, ejecutar:

   ```
   npx n8n
   ```

   La primera vez descarga n8n; esperá hasta ver que quedó escuchando en
   `http://localhost:5678`.

2. Abrir `http://localhost:5678` en el navegador (crear el usuario local si lo pide).

3. Importar el flujo:
   - Crear un workflow nuevo (botón **Create Workflow** / **+**).
   - Menú de los tres puntos **(⋮)** arriba a la derecha -> **Import from File**.
   - Elegir el archivo `donatrack/servicio-logistica/n8n/logistica-evento.workflow.json`.

4. **Publicar** el workflow:
    (arriba a la derecha). Hacé clic en **Publish**.
   - El nombre NO importa.
   - "Publish" = activar el webhook de producción. No significa hacerlo público.

   > El webhook queda escuchando en `http://localhost:5678/webhook/logistica-evento`,
   > que es exactamente la URL que llama el backend.

---

## 2. Levantar el backend (Spring Boot)

1. En **otra** terminal, pararte en la carpeta del servicio:

   ```bash
   cd donatrack/servicio-logistica
   ```

2. Arrancar la aplicación:

   ```bash
   mvn spring-boot:run
   ```

3. Esperar en la consola estas dos líneas:

   ```text
   Escuchando en el puerto: 8085
   [DatosDemoLogistica] Entrega de prueba cargada: 11111111-1111-1111-1111-111111111111 (estado EN_TRASLADO)
   ```

   La segunda línea confirma que hay una entrega de prueba lista para "fallar".
   (El repositorio es en memoria, así que esta entrega se recrea en cada arranque.)

---

## 3. PRUEBA A - Simular ENTREGA_NO_RECIBIDA

### 3.1 (Opcional) Probar n8n de forma directa

Sirve para confirmar que el workflow enruta y reenvía a Donaciones. **Ojo:** como n8n ahora
reenvía a Donaciones, `idDonacion` tiene que existir en Donaciones para no obtener un `404`.

- Método: **POST**
- URL: `http://localhost:5678/webhook/logistica-evento`
- En Postman: pestaña **Body** -> **raw** -> **JSON**, y pegar (usando una donación real de
  Donaciones):

  ```json
  {
    "tipo": "ENTREGA_NO_RECIBIDA",
    "rutaId": "55555555-5555-5555-5555-555555555555",
    "idDonacion": "<id de una donación existente en Donaciones>",
    "motivoFallo": "ENTIDAD_AUSENTE",
    "replanificable": true
  }
  ```

- Resultado esperado: respuesta `{"status":"ok"}`, y en Donaciones el log de la entrega fallida.

### 3.2 Probar el flujo real por el backend

- Método: **POST**
- URL: `http://localhost:8085/api/logistica/entregas/11111111-1111-1111-1111-111111111111/no-recibida`
- Headers: agregar `Content-Type` = `application/json`
- Body -> **raw** -> **JSON** (el `motivo` es un valor del enum `MotivoFalloEntrega`):

  ```json
  {
    "motivo": "ENTIDAD_AUSENTE"
  }
  ```

  Valores válidos: `ENTIDAD_AUSENTE`, `DIRECCION_INCORRECTA`, `RECHAZADA_POR_ENTIDAD`
  (replanificables) y `MERCADERIA_ROTA`, `MERCADERIA_PERDIDA`, `ROBO` (no replanificables).
  Logística deriva el booleano `replanificable` a partir del motivo; el chofer solo manda el motivo.

- Apretar **Send**.

### 3.3 Qué tenés que ver

1. **En Postman:** código `200 OK` (rápido, no se cuelga). El body de respuesta va vacío.
2. **En la consola de Logística:**

   ```text
   [N8nLogisticaWebhookListener] Evento ENTREGA_NO_RECIBIDA disparado para entrega 11111111-...
   ```

3. **En n8n:** pestaña **Executions** del workflow (NO el lienzo/canvas), refrescar. Aparece una
   ejecución que entra por la salida **ENTREGA_NO_RECIBIDA** del nodo "Enrutar por tipo" y ejecuta
   el nodo "POST Donaciones /entrega-fallida" con `{"tipo":"ENTREGA_NO_RECIBIDA", ...}`.
4. **En la consola de Donaciones:** el log de `LogisticaEventosService` procesando la entrega
   fallida y disparando las notificaciones (entidad, donante y admin).
5. **En la consola de Notificaciones:** los `Notificación ... registrada con estado: ENVIADA`.

> Nota: la donación `22222222-2222-2222-2222-222222222222` de la entrega demo de Logística debe
> existir en Donaciones para que el paso 4 no devuelva `404`. Si Donaciones no tiene esa donación,
> vas a ver el evento reenviado (pasos 1-3) pero Donaciones responderá `404`; en ese caso probá con
> el flujo directo 3.1 apuntando a una donación real.

### 3.4 Importante: la entrega de prueba es de un solo uso

Una vez marcada como `NO_RECIBIDA`, si reenviás el mismo POST vas a recibir
`409 Conflict` ("Transición inválida: NO_RECIBIDA -> NO_RECIBIDA"). Es lo esperado.
Para volver a probar, **reiniciá el backend** (Ctrl+C y `mvn spring-boot:run`): eso
regenera la entrega en estado `EN_TRASLADO`.

---

## 4. PRUEBA B - Flujo de planificación con el proveedor mock

El backend, al planificar, le envía el lote a un proveedor externo de ruteo. Para no depender de un servidor externo, hay un **mock** dentro de la misma app (`MockProveedorRuteoController`) que escucha en `/ruteo/planificar` y responde OK.
La propiedad `integraciones.proveedor-ruteo.url` ya apunta a `http://localhost:8085/ruteo/planificar`.

- Método: **POST**
- URL: `http://localhost:8085/api/logistica/planificaciones`
- Headers: `Content-Type` = `application/json`
- Body -> **raw** -> **JSON**:

  ```json
  {
    "camionesIds": ["66666666-6666-6666-6666-666666666666"],
    "donaciones": [
      {
        "idDonacion": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "idEntidadBeneficiaria": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        "direccionEntrega": {
          "calle": "Av. Medrano",
          "numero": 951,
          "localidad": "CABA",
          "provincia": "Buenos Aires",
          "codigoPostal": "C1179"
        }
      }
    ]
  }
  ```

### Qué tenés que ver

1. **En Postman:** código `202 Accepted`, con un JSON del lote creado (id, estado, etc.).
2. **En la consola de Java:** el log del mock:

   ```text
   [MockProveedorRuteoController] Lote recibido para planificar: loteId=..., tokenCorrelacion=..., callbackUrl=http://localhost:8085/api/logistica/planificaciones/callback
   ```

   Eso demuestra que el backend llamó al proveedor (mock) **sin Connection Refused**.

> Nota: el mock solo loguea y responde OK; no devuelve rutas reales por el callback, así que esta prueba no genera entregas nuevas. Para probar la Prueba A seguí usando la entrega del Seed.

