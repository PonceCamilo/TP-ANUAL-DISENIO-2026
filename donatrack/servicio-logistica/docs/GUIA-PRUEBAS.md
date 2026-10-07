# Guía de pruebas - servicio-logistica + broker de logística

Esta guía explica, paso a paso, cómo levantar el servicio de logística y el broker en una
máquina local y probar tres flujos:

- **Prueba A:** evento `ENTREGA_NO_RECIBIDA` -> broker -> Donaciones
  (`/logistica/eventos/entrega-fallida`) -> Donaciones notifica a entidad, donante y admin.
- **Prueba B:** flujo de planificación de rutas contra el proveedor de ruteo *mock*.
- **Prueba C:** el broker eligiendo entre los dos servicios de logística (con fallback).

> **Arquitectura (Diseño A):** Logística no llama a Donaciones ni a Notificaciones. Publica
> sus eventos en el **broker de logística** (`servicio-broker-logistica`, `:8087`), que los
> reenvía al callback de Donaciones según `tipo`. Es **Donaciones** quien resuelve los
> contactos y dispara las notificaciones. El broker reemplaza al relay de n8n que se usaba
> antes (n8n se sigue usando en `servicio-incentivos`, no acá).
>
> ```
> Donaciones ──► Broker (:8087) ──► servicio-logistica         (:8085)
>      ▲            │          └──► servicio-logistica-externa (:8086)
>      └── eventos ─┘   ◄── eventos de ambos proveedores, traducidos al formato de Donaciones
> ```

---

## 0. Requisitos previos

- **Java 21** y **Maven**.
- **Postman** (o cualquier cliente REST). La colección está en `donatrack/postman/`.

Todos los servicios se levantan igual: `cd donatrack/servicio-<nombre>` y `mvn spring-boot:run`.

| Servicio | Puerto | Para qué prueba |
|---|---|---|
| servicio-logistica | 8085 | A, B, C |
| servicio-broker-logistica | 8087 | A, C |
| servicio-logistica-externa | 8086 | C |
| servicio-donaciones | 8081 | A (y C, para ver los eventos procesados) |
| servicio-notificaciones | 8084 | A, para ver los mails simulados |
| servicio-incentivos | 8083 | A, aviso de donación exitosa |

También se puede levantar todo junto con `docker compose up --build` desde `donatrack/`.

---

## 1. Levantar el backend de logística

1. Levantar la base de datos de logística (PostgreSQL, publicada en `localhost:5433`):

   ```bash
   cd donatrack
   docker compose up -d logistica-db
   ```

   Al arrancar, el servicio crea las tablas con las migraciones de Flyway
   (`src/main/resources/db/migration`).

2. En una terminal:

   ```bash
   cd donatrack/servicio-logistica
   mvn spring-boot:run
   ```

3. Esperar en la consola estas líneas:

   ```text
   Escuchando en el puerto: 8085
   [DatosDemoLogistica] Entrega de prueba cargada: 11111111-1111-1111-1111-111111111111 (estado EN_TRASLADO) en ruta 55555555-...
   [DatosDemoLogistica] Camiones DISPONIBLE cargados: 88888888-... (AC456EF), 99999999-... (AD789GH)
   ```

   Confirman que hay una entrega de prueba lista para "fallar" (su camión, `AB123CD`, queda
   `EN_RUTA`) y dos camiones `DISPONIBLE` para planificar. Los datos quedan guardados en la
   base: en los arranques siguientes no se recrean (se ve "ya existente, no se recarga"),
   así que los cambios de estado de una prueba persisten. Para empezar de cero:
   `docker compose down -v` (borra el volumen `logistica-db-data`).

4. En otra terminal, levantar el broker:

   ```bash
   cd donatrack/servicio-broker-logistica
   mvn spring-boot:run
   ```

   Logística publica sus eventos en `integraciones.broker.eventos.url`
   (por defecto `http://localhost:8087/api/broker/eventos/donatrack`). Si el broker no está
   levantado, la entrega igual cambia de estado y solo se loguea el error del envío del evento.

---

## 2. PRUEBA A - Simular ENTREGA_NO_RECIBIDA

### 2.1 (Opcional) Probar el broker de forma directa

Sirve para confirmar que el broker enruta y reenvía a Donaciones. `idDonacion` tiene que
existir en Donaciones para no obtener un error.

- Método: **POST**
- URL: `http://localhost:8087/api/broker/eventos/donatrack`
- Body -> **raw** -> **JSON**:

  ```json
  {
    "tipo": "ENTREGA_NO_RECIBIDA",
    "rutaId": "55555555-5555-5555-5555-555555555555",
    "idDonacion": "<id de una donación existente en Donaciones>",
    "motivoFallo": "ENTIDAD_AUSENTE",
    "replanificable": true
  }
  ```

- Resultado esperado: `200 OK`. Si Donaciones no está levantado o rechaza el evento, el broker
  responde `502 Bad Gateway` con el detalle.

### 2.2 Probar el flujo real por el backend

- Método: **POST**
- URL: `http://localhost:8085/api/logistica/entregas/11111111-1111-1111-1111-111111111111/no-recibida`
- Headers: `Content-Type` = `application/json`
- Body -> **raw** -> **JSON** (el `motivo` es un valor del enum `MotivoFalloEntrega`):

  ```json
  {
    "motivo": "ENTIDAD_AUSENTE"
  }
  ```

  Valores válidos: `ENTIDAD_AUSENTE`, `DIRECCION_INCORRECTA`, `RECHAZADA_POR_ENTIDAD`
  (replanificables) y `MERCADERIA_ROTA`, `MERCADERIA_PERDIDA`, `ROBO` (no replanificables).
  Logística deriva el booleano `replanificable` a partir del motivo; el chofer solo manda el motivo.

### 2.3 Qué tenés que ver

1. **En Postman:** `200 OK` (rápido, no se cuelga). El body de respuesta va vacío.
2. **En la consola de Logística:**

   ```text
   [BrokerEventosListener] Evento ENTREGA_NO_RECIBIDA disparado para entrega 11111111-...
   ```

3. **En la consola del broker:**

   ```text
   [Broker] Evento ENTREGA_NO_RECIBIDA de DONATRACK
   [Broker] Evento reenviado a Donaciones /logistica/eventos/entrega-fallida
   ```

4. **En la consola de Donaciones:** el log de `LogisticaEventosService` procesando la entrega
   fallida y disparando las notificaciones (entidad, donante y admin).
5. **En la consola de Notificaciones:** los `Notificación ... registrada con estado: ENVIADA`.

> Nota: la donación `22222222-2222-2222-2222-222222222222` de la entrega demo de Logística debe
> existir en Donaciones para que el paso 4 funcione. Si no existe, vas a ver el evento en el
> broker (pasos 1-3) pero Donaciones lo rechazará; en ese caso probá con el flujo directo 2.1
> apuntando a una donación real.

### 2.4 Importante: la entrega de prueba es de un solo uso

Una vez marcada como `NO_RECIBIDA`, si reenviás el mismo POST vas a recibir
`409 Conflict` ("Transición inválida: NO_RECIBIDA -> NO_RECIBIDA"). Es lo esperado.
Para volver a probar, **reiniciá el backend**: eso regenera la entrega en estado `EN_TRASLADO`.

---

## 3. PRUEBA B - Flujo de planificación con el proveedor de ruteo mock

El backend, al planificar, le pide a un proveedor externo de ruteo que planifique la ruta de
cada camión: una llamada síncrona por camión, que responde con la ruta planificada. Para no
depender de un servidor externo, hay un **mock** dentro de la misma app
(`MockProveedorRuteoController`) en `/ruteo/planificar`.

1. **Ver la flota:** **GET** `http://localhost:8085/api/logistica/camiones`. Al arrancar hay dos
   camiones `DISPONIBLE` (`AC456EF` y `AD789GH`) y el de la entrega demo (`AB123CD`, `EN_RUTA`).

2. **Planificar.** `camionesIds` es opcional: si no se envía, se usan todos los camiones en
   estado `DISPONIBLE` (así lo usa el broker). Para elegirlos a mano, agregar por ejemplo
   `"camionesIds": ["88888888-8888-8888-8888-888888888888"]`.

   - **POST** `http://localhost:8085/api/logistica/planificaciones`

     ```json
     {
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

1. **En Postman:** `200 OK` con el lote planificado: `estado: "COMPLETADO"` y, dentro de
   `rutas`, el camión con sus paradas y las entregas creadas (en `LISTO_PARA_ENTREGAR`).
2. **En la consola de Logística:** el log del mock:

   ```text
   [MockProveedorRuteoController] Planificando lote=..., camion=..., 1 donaciones
   ```

Si no hay ningún camión `DISPONIBLE` la respuesta es `503` ("No hay camiones disponibles"):
el broker lo interpreta como proveedor no disponible y prueba con el siguiente.

---

## 4. PRUEBA C - Broker con dos servicios de logística

Levantar además `servicio-logistica-externa` (`:8086`).

1. **Ver los proveedores:** **GET** `http://localhost:8087/api/broker/proveedores`

   ```json
   [{"nombre":"DONATRACK","prioridad":1,"disponible":true},
    {"nombre":"EXTERNA","prioridad":2,"disponible":true}]
   ```

2. **Pedir un envío** (es lo que hará Donaciones): **POST** `http://localhost:8087/api/broker/envios`

   ```json
   {
     "donaciones": [{
       "idDonacion": "44444444-4444-4444-4444-444444444444",
       "idEntidadBeneficiaria": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
       "nombreEntidad": "Comedor Los Pibes",
       "direccionEntrega": {"calle": "Medrano", "numero": 951, "localidad": "CABA", "provincia": "Buenos Aires"}
     }]
   }
   ```

   - Sin `proveedor`, el broker usa la prioridad (`broker.proveedores.prioridad=DONATRACK,EXTERNA`).
   - Con `"proveedor": "EXTERNA"` usa solo ese, sin fallback.
   - La respuesta indica quién lo tomó, a quién descartó y el id de seguimiento de cada donación.

3. **Fallback:** apagar `servicio-logistica` y repetir el paso 2. También pasa si Logística se
   queda sin camiones `DISPONIBLE` (un camión pasa a `EN_RUTA` al iniciar su ruta y vuelve a
   `DISPONIBLE` cuando la ruta termina). La respuesta sale con `"proveedor": "EXTERNA"` y `"proveedoresDescartados": ["DONATRACK"]`.

4. **Ciclo del proveedor externo** (con el `idSeguimiento` = `trackingId` del paso anterior):

   - **POST** `http://localhost:8086/api/v1/envios/{trackingId}/despacho` con `{"vehiculo": "ZZ999ZZ"}`
   - **POST** `http://localhost:8086/api/v1/envios/{trackingId}/fallo` con `{"motivo": "Destinatario ausente"}`
     (opcional; después se puede volver a despachar)
   - **POST** `http://localhost:8086/api/v1/envios/{trackingId}/entrega`

   Cada cambio de estado llega al broker (`/api/broker/eventos/externa`), que lo traduce y lo
   reenvía a Donaciones:

   | Estado del externo | Callback de Donaciones |
   |---|---|
   | `EN_CAMINO` | `/logistica/eventos/inicio-ruta` (con el link de tracking como mapa) |
   | `ENTREGADO` | `/logistica/eventos/entrega-exitosa` (vehículo como patente) |
   | `FALLIDO` | `/logistica/eventos/entrega-fallida` (siempre replanificable) |

5. **Consultar qué proveedor tomó una donación:**
   **GET** `http://localhost:8087/api/broker/envios/44444444-4444-4444-4444-444444444444`
