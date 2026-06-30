# Guía de pruebas - servicio-logistica + n8n

Esta guía explica, paso a paso, cómo levantar el servicio de logística y n8n en una
máquina local y probar dos flujos:

- **Prueba A:** evento `ENTREGA_NO_RECIBIDA` -> webhook a n8n -> aviso al administrador
  (bandera `requiereAvisoAdmin`).
- **Prueba B:** flujo de planificación de rutas contra el proveedor externo *mock*
  (sin `Connection Refused`).
---

## 0. Requisitos previos

Instalar en la máquina:

- **Java 17+** y **Maven** (para correr el backend con `mvn`).
- **Node.js** (trae `npx`, que usamos para levantar n8n).
- **Postman** (cliente para enviar peticiones HTTP). Alternativa: cualquier cliente REST.

Clonar/abrir el repo del TP. Todos los comandos asumen que estás parado en la carpeta
del servicio: `donatrack/servicio-logistica`.

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

Sirve para confirmar que el workflow responde y para "calentarlo".

- Método: **POST**
- URL: `http://localhost:5678/webhook/logistica-evento`
- En Postman: pestaña **Body** -> **raw** -> **JSON**, y pegar:

  ```json
  {
    "tipo": "ENTREGA_NO_RECIBIDA",
    "entregaId": "11111111-1111-1111-1111-111111111111",
    "idEntidadBeneficiaria": "33333333-3333-3333-3333-333333333333",
    "idDonante": "44444444-4444-4444-4444-444444444444",
    "motivo": "prueba directa",
    "requiereAvisoAdmin": true
  }
  ```

- Resultado esperado: respuesta `{"status":"ok","avisoAdmin":true}`.

### 3.2 Probar el flujo real por el backend

- Método: **POST**
- URL: `http://localhost:8085/api/logistica/entregas/11111111-1111-1111-1111-111111111111/no-recibida`
- Headers: agregar `Content-Type` = `application/json`
- Body -> **raw** -> **JSON**:

  ```json
  {
    "motivo": "La entidad beneficiaria no se encontraba en el domicilio"
  }
  ```

- Apretar **Send**.

### 3.3 Qué tenés que ver

1. **En Postman:** código `200 OK` (rápido, no se cuelga). El body de respuesta va vacío.
2. **En la consola de Java:**

   ```text
   [N8nLogisticaWebhookListener] Evento ENTREGA_NO_RECIBIDA disparado para entrega 11111111-...
   ```

3. **En n8n:** ir a la pestaña **Executions** del workflow (NO al lienzo/canvas) y
   refrescar. Aparece una ejecución que entra por la rama **true** del nodo
   "Requiere aviso admin?" y ejecuta "Email aviso a administrador (simulado)". Si abrís
   ese nodo, ves los campos `para`, `asunto` y `cuerpo` del correo con los datos de la
   entrega.

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
        "idDonante": "cccccccc-cccc-cccc-cccc-cccccccccccc",
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

