# Servicio de Donaciones: justificaciones de la Entrega 5

La Entrega 5 pide persistir el modelo de cada servicio (relacional con ORM, salvo Logística, que va a una base documental con ODM) y aplicar **estrategias de desnormalización** donde mejoren las lecturas. Además hay que actualizar el modelo de dominio, el DER, las justificaciones y el diagrama de componentes.

Este documento cubre solo `servicio-donaciones`, con los cambios de la revisión posterior a la entrega (marcados como **revisión**). Los diagramas están en esta misma carpeta:

| Archivo | Contenido |
|---|---|
| `donaciones-modelo.puml/.svg/.pdf` | Modelo de dominio (paquete `models`) |
| `donaciones-servicios.puml/.svg/.pdf` | Services, eventos, bandeja de salida, repositorios, utilitarios e integraciones |
| `donaciones-der.puml/.svg/.pdf` | DER físico de `donaciones_db`, tal como lo crean las migraciones de Flyway |
| `donaciones-componentes.puml/.svg/.pdf` | Diagrama de componentes del servicio |

---

## 1. Modelo de persistencia: relacional con ORM

**Decisión.** Donaciones sigue en PostgreSQL 16 (base `donaciones_db`, en el servidor compartido del `docker-compose`), con Spring Data JPA + Hibernate 6 como ORM.

**Por qué.** Los datos del servicio son entidades con identidad propia y relaciones obligatorias: una donación pertenece a una persona donante, se asigna a una entidad beneficiaria y tiene historial de estados. Estas son las reglas que importan:

- el email de una persona es único;
- no se puede borrar una persona o entidad que tenga donaciones;
- el estado de una donación cambia de forma transaccional junto con su historial;
- una asignación y su contador de recepciones se guardan juntos o no se guardan;
- un aviso a otro servicio se registra junto con el cambio que lo origina (**revisión**, sección 8).

Una base relacional las expresa con restricciones y transacciones. Un modelo documental obligaría a elegir un agregado raíz (¿la persona con sus donaciones? ¿la entidad con las que recibió?) y a repetir datos en los dos lados.

**Alternativa descartada.** MongoDB, como Logística. Logística guarda documentos que se leen y escriben enteros (una entrega con su ruta y sus eventos), así que el documental le sirve. Las consultas de Donaciones cruzan agregados: donaciones por donante, por entidad y por estado, y rankings que combinan donaciones con necesidades de todas las entidades.

---

## 2. Cómo se midió

Antes de cambiar algo se midió cuántas sentencias SQL ejecuta cada operación. Con eso cada decisión de este documento se apoya en un número y no en una intuición.

- **Test de medición**: `MedicionConsultasTest` (en `src/test/.../medicion`). Levanta el servicio completo sobre H2 en modo PostgreSQL y carga 200 donantes, 20 entidades y 60 donaciones (30 asignadas). Cuenta las sentencias con las estadísticas de Hibernate (`prepareStatementCount`). Quedó como **test de regresión**: cada operación tiene un tope y el test falla si alguien vuelve a introducir un N+1.
- **Importación masiva**: `ImportacionCsvMedicionTest` importa el CSV real de 20.000 filas. Solo corre si se pasa `-Ddonatrack.csv.medicion=<ruta del CSV>`, porque el archivo no se sube al repo.
- **Qué vale de los números**: la cantidad de sentencias no depende del motor, así que es la métrica que se compara. Los milisegundos son de H2 en memoria y solo sirven como referencia: en PostgreSQL, con red de por medio, cada sentencia ahorrada pesa más.

### Resultados (sentencias SQL por operación)

| Operación | Línea base | Cierre de la Entrega 5 | Revisión | Tope del test |
|---|---:|---:|---:|---:|
| Matchmaking nocturno (batch) | 75.091 | 192 | **41** | 80 |
| `GET /donaciones/{id}/candidatos` | 2.603 | 13 | **9** | 20 |
| `GET /donaciones/candidatos` (bandeja) | 3.000 | 9 | **7** | 15 |
| `GET /donaciones` (y con filtros) | 121 | 3-4 | **3-4** | 10 |
| `GET /donantes` | 401 | 4 | **3** | 10 |
| Proceso de inactividad (batch) | 401 | 3 | **4** | 10 |
| `POST /donaciones` (4 bienes) | 20 | 7 | **8** | 15 |
| `PATCH /donaciones/{id}/asignar` | 13 | 23 | **14** | 25 |
| `POST /donantes/import` (300 altas) | 1.200 | 6 | **6** | 30 |
| `POST /donantes/import` (300 actualizaciones) | 900 | 7 | **4** | 30 |

| CSV de 20.000 filas | Antes | Cierre de la Entrega 5 | Revisión (H2) | Revisión (PostgreSQL 16) |
|---|---|---|---|---|
| Primera importación | 90,3 s · 80.040 sentencias | 7,9 s · 439 sentencias | **8 a 12 s · 413 sentencias** | **17,9 s** |
| Reimportación (todas actualizaciones) | 161,6 s · 72.162 sentencias | 2,6 s · 693 sentencias | **1,9 a 3,4 s · 413 sentencias** | **2,7 a 4,7 s** |

El CSV tiene 19.986 emails distintos: la primera importación crea 19.986 personas y actualiza 14 (las filas que repiten un email), sin errores. Los segundos varían según la carga de la máquina en cada corrida; la cantidad de sentencias no. La columna de PostgreSQL es el servicio levantado contra la base real, con el pedido HTTP completo.

En la revisión, el matchmaking baja porque los rankings van a una sola tabla y la bandeja se reemplaza de una vez (y suma dos `DELETE` al final para descartar las donaciones que dejaron el depósito durante la corrida, sección 3.1), y asignar baja porque la donación recuerda su asignación anterior y no hay que releerla (sección 3.2). Registrar una carga sube una sentencia: con carga diferida, los contactos de la persona se leen al armar el aviso a incentivos en lugar de venir en un join. El aviso de inactividad sube una porque cada lote se marca como avisado en la base (ya no se re-avisa todas las noches).

---

## 3. Desnormalizaciones

Las tablas desnormalizadas se marcan en amarillo en el DER y las columnas derivadas, con `D`.

### 3.1 Ranking de candidatas precalculado (`ranking_precalculado`, `ranking_puesto`)

**Problema.** Para recomendar a qué entidad asignar una donación corren los algoritmos de asignación (hoy, compatibilidad semántica y prioridad a entidades sub-atendidas). Cada uno recorre todas las entidades con sus campañas y necesidades. El proceso nocturno guardaba el resultado en un `ConcurrentHashMap`, que tiene dos problemas:

- se perdía al reiniciar el servicio, y la bandeja quedaba vacía hasta la noche siguiente;
- cada instancia del servicio tenía su propia bandeja.

**Decisión.** El resultado se persiste como una tabla de lectura: una fila por donación con la fecha del cálculo (`ranking_precalculado`) y sus puestos en `ranking_puesto`, con el algoritmo y la posición como columnas. La bandeja (`GET /donaciones/candidatos`) lo lee tal cual: con `@Fetch(SUBSELECT)` toda la bandeja cuesta dos consultas, no dos por donación. Las coincidencias entre algoritmos no se guardan: se derivan de los rankings. Un algoritmo que no encontró entidades no deja puestos; al leer, el ranking se completa con todos los algoritmos registrados (`RankingPrecalculado.resultado(algoritmos)`), así la bandeja muestra ese algoritmo con la lista vacía, igual que el cálculo a demanda, y no informa coincidencias que no hay.

**Revisión.** Al principio había una tabla por lista (`ranking_coincidencia`, `ranking_semantico`, `ranking_subatendidos`), iguales salvo el nombre, y el algoritmo estaba en el nombre de la tabla. Con una tabla de puestos, sumar un algoritmo no cambia el esquema ni el JSON (`rankings: [{algoritmo, entidades}]`). La migración V2 copió los rankings vigentes a la tabla nueva.

**Cómo se mantiene consistente.**

- El proceso de las 03:30 recalcula todos los rankings y **reemplaza la bandeja de una vez al final**: si las necesidades cambiaron durante el día, una recomendación vieja es peor que ninguna, y si el proceso se corta, la bandeja anterior queda entera.
- Una donación sale de la bandeja en cuanto deja de estar en depósito o se da de baja: el agregado registra el evento y un oyente borra su ranking en la misma transacción.
- En la transacción que reemplaza la bandeja también se **descartan las donaciones que ya no están en depósito** (`descartarFueraDelDeposito`, dos `DELETE ... NOT IN`). Sin eso, una donación asignada mientras corría el proceso, que ya se había puntuado en su lote, volvía a la bandeja hasta la noche siguiente.
- `fecha_calculo` le dice a quien administra qué tan vigente es lo que está viendo.

**Costo.** Durante el día el ranking puede quedar desactualizado respecto de las necesidades. Se acepta porque el enunciado plantea el matchmaking como proceso nocturno. Las donaciones que entran al depósito durante el día (por ejemplo, después de una entrega fallida) recién tienen ranking en la corrida siguiente.

**Alternativas descartadas.**

- *Calcular a demanda*: es lo que hace `GET /donaciones/{id}/candidatos` para una donación (9 sentencias). Para la bandeja completa serían los algoritmos por cada donación en depósito.
- *Vista materializada en PostgreSQL*: los algoritmos están en Java (Strategy + Template Method). Pasarlos a SQL duplicaría la lógica del dominio.
- *Una columna JSON con las listas*: menos tablas, pero se pierde el tipado y no se puede consultar por entidad.

### 3.2 Recepciones por entidad y día (`entidad_recepcion_diaria`)

**Problema.** El algoritmo de prioridad a sub-atendidas necesita saber cuántas donaciones recibió cada entidad en los últimos 3 meses. Lo calculaba leyendo todas las donaciones, con sus bienes e historial, una vez por entidad. Con los datos de prueba, un solo ranking costaba más de mil sentencias.

**Decisión.** Un contador con clave `(entidad_id, fecha)` y la cantidad de donaciones asignadas ese día. El algoritmo lo lee en una sola consulta agregada para todas las entidades. Para eso `AlgoritmoAsignacionBase` tiene un paso del template method, `prepararPuntaje(donacion, entidades)`, que se ejecuta una vez por ranking.

- **Diario y no mensual**, para respetar exactamente la regla del algoritmo ("últimos 3 meses" contados día por día). Un contador mensual obligaría a redondear el trimestre.

**Cómo se mantiene consistente.**

- `DonacionesRepository.guardar` mueve el contador **en la misma transacción** que la donación. La donación recuerda con qué asignación se leyó (`Asignacion`, un objeto de valor): si cambió, descuenta de la anterior y suma a la nueva. **Revisión:** antes había un método aparte (`guardarAsignacion`) que releía la asignación previa con una consulta; con la entidad gestionada dentro de la transacción, esa relectura veía el cambio ya aplicado y el contador no se movía.
- Dar de baja una donación asignada descuenta su recepción.
- Una **reconciliación nocturna** (03:00, antes del matchmaking de las 03:30) vacía el contador y lo recalcula desde `donacion` con un `INSERT ... SELECT ... GROUP BY`. Si alguna vez se desfasa, se corrige antes de que lo use el algoritmo.

**Costo.** Asignar actualiza o inserta la fila del contador: 14 sentencias en vez de 13.

**Alternativas descartadas.**

- *Un `COUNT ... GROUP BY` sobre `donacion` en cada ranking*, usando el índice `(id_entidad_beneficiaria, fecha_asignacion)`. Es válida y también resuelve el N+1 en una consulta. Se eligió el contador porque su lectura queda acotada a entidades × 90 días y no crece con el historial de donaciones, que solo aumenta.
- *Trigger en la base*: deja lógica de negocio en PostgreSQL y no corre igual en H2, donde están los tests.

### 3.3 Desnormalizaciones que ya existían y ahora quedan documentadas

| Columna | Se podría derivar de | Por qué se guarda |
|---|---|---|
| `donacion.estado` | El último registro de `cambio_estado` | Todos los listados y procesos filtran por estado. Derivarlo pediría una subconsulta por fila. Se actualiza en el mismo objeto y la misma transacción que el historial (`Donacion.cambiarEstado`). |
| `donacion.subcategoria_tipo` | La subcategoría de sus bienes | `CargaDonacion` segmenta la carga por subcategoría, así que todos los bienes de una donación comparten la misma. Filtrar por subcategoría no necesita un JOIN con `bien`. |
| `donacion.subcategoria_tipo_normalizado` | `subcategoria_tipo` | Es la forma de búsqueda indexada: "Azúcar" y " azucar " son la misma subcategoría. **Revisión:** la normalización pasó de minúsculas a minúsculas sin acentos y con los espacios colapsados (`NormalizadorTexto`); la migración V3 recalculó las filas guardadas. |

---

## 4. Índices

Se declaran en las entidades (`@Table(indexes = ...)`) y los crean las migraciones. PostgreSQL no indexa solo las columnas FK, así que también se declaran.

| Índice | Lo usa |
|---|---|
| `donacion (estado)` | Matchmaking (`EN_DEPOSITO`), despacho del día (`ASIGNACION_REALIZADA`), filtro por estado |
| `donacion (id_donante, estado)` | `GET /donaciones?idDonante=` (con o sin estado) y el bloqueo de borrado de donantes |
| `donacion (id_entidad_beneficiaria, fecha_asignacion)` | Filtro `idEntidad`, bloqueo de borrado de entidades, reconciliación del contador |
| `donacion (subcategoria_tipo_normalizado)` | Filtro por subcategoría |
| `donacion (fecha_donacion DESC, id)` | Orden del listado paginado (las más nuevas primero, desempate por id) |
| `persona_donante (estado)` | `GET /donantes?estado=` |
| `persona_donante (ultima_interaccion)` | Proceso nocturno de inactividad |
| `persona_donante.email` UNIQUE | Alta, búsqueda e importación CSV por email |
| `entidad_recepcion_diaria (fecha)` | Lectura del trimestre del algoritmo de sub-atendidas |
| `ranking_puesto (id_donacion)` | Lectura y borrado de los puestos de una donación (**revisión**) |
| `mensaje_saliente (estado, proximo_intento)` | El relay busca los avisos pendientes que ya pueden enviarse (**revisión**) |
| Columnas FK de `bien`, `cambio_estado`, `medio_de_contacto` (3), `representante` (2), `campania`, `necesidad` | Carga de colecciones por lotes |

---

## 5. Lecturas: consultas en la base, carga diferida y paginación

**Problema.** `GET /donaciones` traía todas las donaciones y filtraba en memoria. `GET /donantes` devolvía la tabla entera con sus contactos: después de importar el CSV eran 20.000 personas en una respuesta. Además, cada colección `EAGER` se cargaba con una consulta por fila (N+1).

**Decisiones.**

- **Filtros en la base**: `Specification` arma el `WHERE` según los filtros presentes (estado, donante, subcategoría, entidad y bienes vencidos).
- **Paginación siempre activa** en `GET /donaciones`, `GET /donantes` y (**revisión**) `GET /entidades`:
  - `page` empieza en 0; `size` vale 20 si no se indica y como máximo 100.
  - Un pedido fuera de rango o un parámetro no numérico responde **400**. No se corrige en silencio, porque es un error de quien llama.
  - El orden es estable: donaciones por fecha descendente y luego id; donantes por email; entidades por razón social y luego id.
- **Página propia** (`Pagina` en el dominio, `PaginaResponseDTO` en la API: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`), en lugar de devolver el `Page` de Spring. El contrato de la API no depende del framework.
- **Carga diferida y lectura por lotes (revisión).** Las colecciones pasaron de `EAGER` a `LAZY`: cada caso de uso lee solo lo que usa (el matchmaking bajó de 42 a 39 sentencias, asignar de 15 a 14). Cuando una colección se usa, `hibernate.default_batch_fetch_size=100` la trae para hasta 100 dueños en un solo `IN (...)`. Al ser un valor global, reemplaza a los diez `@BatchSize` repetidos y cubre las colecciones nuevas. Los services leen y arman los DTOs dentro de su transacción.
- **Carga de entidades en bloque** (`obtenerPorIds`) para armar la bandeja, los candidatos y los avisos de logística.
- **Inactividad en la base**: una consulta trae a las personas sin interacción ni aviso desde el límite, de a lotes que se marcan como avisados con un `UPDATE`.

**Alternativas descartadas.**

- *Paginación por cursor (keyset) en la API*: escala mejor en páginas profundas, pero complica la API. Para el volumen de un panel de administración alcanza con `OFFSET`, y el índice por fecha cubre el orden. Los procesos nocturnos sí recorren por id (sección 9).
- *`JOIN FETCH` de las colecciones*: con varias listas en la misma entidad genera producto cartesiano, y Hibernate no lo permite con más de un `bag`.
- *Un DTO liviano para los listados*: cambiaría el JSON de `GET /donaciones`, que hoy incluye bienes e historial.

---

## 6. Escrituras: altas directas, lotes JDBC e importación CSV por lotes

**Problema.** Los ids son UUID asignados por la aplicación, así que Spring Data no podía saber si una entidad era nueva: hacía un `merge`, con un `SELECT` antes de cada `INSERT`. La importación CSV procesaba fila por fila (buscar por email y guardar), unas 4 sentencias por fila: 90 s para 20.000 filas.

**Decisiones.**

- Las raíces de agregado (`Donacion`, `PersonaDonante` y, en la revisión, `EntidadBeneficiaria`) extienden `EntidadPersistible`, que implementa **`Persistable<UUID>`**: un indicador transitorio `nueva` vale `true` hasta `@PostPersist`/`@PostLoad`, así que las altas van directo al `INSERT`.
- **Lotes JDBC**: `hibernate.jdbc.batch_size=50` con `order_inserts`/`order_updates`. Funciona porque los ids no son `IDENTITY`, que desactiva el batch.
- **CSV por lotes de 500 filas, cada uno en su transacción** (`TransactionOperations`):
  - Los emails del lote se buscan en una sola consulta (`obtenerPorEmails`, con `IN`) y se guardan todos juntos (`guardarTodos`).
  - Si el lote falla, se reprocesa fila por fila para informar en qué línea estuvo el error.
  - Cada fila se valida antes de procesarse (**revisión**): el email con su forma, el documento con su tipo y que una organización traiga CUIT.
  - En el alta, la persona queda con el email como contacto predeterminado y el teléfono como contacto `TELEFONO`.
  - La respuesta resume el resultado y detalla hasta 100 errores, en lugar de devolver una fila por cada línea del archivo.

**Alternativas descartadas.**

- *`COPY` o `INSERT` nativos masivos*: más rápidos, pero se saltean el dominio (validaciones, estado inicial, normalización del email).
- *Todo el archivo en una transacción*: una fila mala aborta las 20.000, y la transacción retiene bloqueos durante toda la importación.

---

## 7. Integridad

| Decisión | Detalle | Alternativa descartada |
|---|---|---|
| **Email único en la base** | `persona_donante.email` tiene `UNIQUE`. Antes la unicidad dependía de una consulta previa: dos altas simultáneas, o el mismo email con otras mayúsculas, dejaban duplicados. Si la base rechaza el `INSERT` (SQLState 23505), el handler responde **409**. | Solo validar en el servicio (no cubre la concurrencia). |
| **Forma canónica del email** | `NormalizadorEmail`: trim, Unicode NFC y minúsculas (`Locale.ROOT`). Se aplica en `@PrePersist`/`@PreUpdate`, venga la persona de la API, del builder o del CSV. Los contactos `EMAIL` y (**revisión**) los representantes se guardan igual. Se aceptan emails con tildes. | Índice `UNIQUE` sobre `lower(email)`: Hibernate no lo genera desde las anotaciones y no resuelve las variantes Unicode (NFC). |
| **Email inmutable** | Es la identidad de la persona y la clave de idempotencia del CSV: se fija en el alta y no se sincroniza con los contactos. | Copiar el último contacto `EMAIL`: cambiaría la identidad de la persona sin que nadie lo pida. |
| **Bloqueo optimista (revisión)** | Las raíces de agregado tienen una columna `version`. Si dos pedidos modifican lo mismo a la vez, el segundo responde **409** en lugar de pisar al primero. La última interacción de la persona queda fuera de la versión (`@OptimisticLock(excluded = true)`): es una marca de tiempo que pisa la anterior, y dos donaciones simultáneas de la misma persona se tienen que guardar las dos. | Bloqueo pesimista (`SELECT ... FOR UPDATE`): retiene la fila mientras dura el pedido. |
| **Documento validado (revisión)** | Tipo y número se validan juntos (`Documento`, `@Embeddable`): DNI de 7 u 8 dígitos, CUIT y CUIL de 11 dígitos. El número se guarda sin puntos ni guiones; una organización tiene que tener CUIT. Inválido → **400**. El CUIT se valida por su forma y no por el dígito verificador: 580 de los 6.055 CUIT del CSV del enunciado no lo cumplen, y tampoco el CUIT de ejemplo del propio enunciado (30-12345678-9). | Dos `String` sueltos sin reglas; exigir el dígito verificador (rechazaba datos que el enunciado da como válidos). |
| **Contacto predeterminado dentro de la lista (revisión)** | El contacto preferido es un contacto de la persona marcado como predeterminado. Antes era una referencia aparte (`medio_predeterminado_id`) que podía apuntar a un contacto fuera de su lista. | - |
| **Baja lógica de donaciones (revisión)** | `DELETE /donaciones/{id}` marca `fecha_baja`; `@SQLRestriction` la saca de todas las consultas y conserva su historial. Si estaba asignada, descuenta su recepción. | Borrado físico: se pierde la trazabilidad de la donación. |
| **Borrados con donaciones → 409** | No se puede borrar una persona o entidad con donaciones (`PersonaConDonacionesException`, `EntidadConDonacionesException`). `donacion` referencia a ambas por id (límite de agregado), sin FK. | FK real con `ON DELETE RESTRICT` (ver sección 10). |
| **Agregados encapsulados (revisión)** | Las raíces no exponen setters ni listas modificables: cada cambio permitido es un método del agregado (`asignarA`, `agregarBien`, `reemplazarContactos`, ...). | - |
| **Sin columnas duplicadas** | `campania.id_entidad` era una segunda copia del id de la entidad, que podía quedar distinta de la FK `entidad_id`. Ahora `Campania.idEntidad` se lee de `entidad_id` (`insertable = false, updatable = false`). | - |

---

## 8. Avisos a otros servicios: bandeja de salida (revisión)

**Problema.** Los avisos a notificaciones e incentivos se mandaban en el medio del caso de uso. Si el otro servicio no respondía, el aviso se perdía; si la transacción fallaba después de mandarlo, el aviso salía por un cambio que nunca se guardó.

**Decisión.** Patrón *outbox*:

- Los agregados registran **eventos de dominio** (`DonacionAsignada`, `DonacionEntregada`, `DonacionesRegistradas`, ...) que Spring publica al guardarlos. Un oyente escribe el aviso en `mensaje_saliente` **antes del commit**, en la misma transacción que el cambio: o se guardan los dos o ninguno.
- Un **relay** (cada 5 s) envía los pendientes por el enviador de su destino (REST o AMQP para notificaciones, REST para incentivos). Si falla, reintenta con una espera que se duplica (30 s hasta 1 h), hasta 10 intentos; después queda `FALLIDO`. Con AMQP espera la confirmación del broker.
- **Cada destino en su propio hilo.** En cada corrida, los pendientes de cada destino se envían en un hilo aparte, en orden de llegada y de a 50, hasta terminarlos. Si un envío falla, el resto de ese destino espera a la corrida siguiente; los otros destinos siguen. Así un servicio lento o caído no demora los avisos de los demás (con Incentivos respondiendo en 1 o 2 s y cientos de avisos suyos atrasados, los avisos a Notificaciones quedaban detrás de todos), no se paga un timeout por cada aviso a un servicio que ya falló, y un destino rápido no queda limitado a 50 avisos cada 5 s (con 20.000 avisos de inactividad salen unos 2.500 por minuto).
- **Una corrida no dura más que su bloqueo.** El relay toma un bloqueo de ShedLock de 1 minuto (sección 9) y cada corrida deja de tomar mensajes a los 30 s. Con los timeouts (2 s de conexión, 5 s de respuesta o de confirmación de RabbitMQ), el envío en curso termina antes de que el bloqueo venza. Si una corrida pasara del minuto, el bloqueo vencería con la corrida a medias y la otra instancia empezaría a enviar los mismos pendientes; y como ShedLock libera por nombre de tarea, al terminar la primera liberaría el bloqueo de la segunda.
- Los enviados se borran a los 7 días.

**Costo.** Los avisos salen unos segundos después del cambio y pueden llegar dos veces si el relay se cae entre enviar y marcar como enviado, o si el otro servicio procesa el aviso pero responde después del timeout (entrega "al menos una vez": quien recibe tiene que tolerar el duplicado).

**Alternativa descartada.** *Mandar el aviso después del commit* (`AFTER_COMMIT`): no se pierde un aviso de algo que no ocurrió, pero sí uno de algo que ocurrió si el otro servicio no responde en ese momento.

---

## 9. Esquema y procesos

- **Migraciones con Flyway (revisión).** El esquema lo crean y lo cambian migraciones versionadas: V1 es el esquema de la Entrega 5, V2 los cambios de la revisión (con la migración de los datos guardados), V3 la renormalización de subcategorías y documentos y V4 la tabla de bloqueo de tareas. Hibernate solo valida al arrancar (`ddl-auto=validate`). Una base creada antes con `ddl-auto=update` se marca en la versión 1 y recibe las demás. Antes era `update`, que agrega columnas pero nunca borra ni migra datos.
- **Zona horaria de los procesos nocturnos**: los `@Scheduled` usan `America/Argentina/Buenos_Aires` (`FechaHoraArgentina.ZONA_ID`) y no la zona del servidor. La imagen `eclipse-temurin` del Dockerfile corre en UTC si no se le configura `TZ`, y ahí "medianoche" serían las 21 h de Argentina.
- **Procesos por lotes (revisión).** El matchmaking y el despacho recorren las donaciones de a 100, ordenadas por id y cada lote en una transacción corta, en lugar de cargarlas todas juntas. El despacho manda cada lote al broker por separado: un lote que falla espera a la noche siguiente sin frenar a los demás. El aviso de inactividad también confirma cada lote de 100 personas en su propia transacción, con sus avisos y su marca de avisado: a los 20 días de importar el CSV las 20.000 personas quedan inactivas a la vez, y con una sola transacción sus filas quedaban bloqueadas hasta el final (una persona que donaba en ese momento esperó 12,7 s; con los lotes, menos de un segundo) y un error al final deshacía todos los avisos.
- **Una instancia por tarea (revisión).** Cada tarea programada toma una fila de la tabla `shedlock` antes de correr (ShedLock, con la hora de la base): con dos instancias del servicio, el matchmaking, el despacho y el relay corren en una sola. Las tareas nocturnas **retienen el bloqueo al menos 5 minutos** después de terminar (`lockAtLeastFor`, `donaciones.procesos.bloqueo-minimo`): una reconciliación tarda milisegundos, y sin ese mínimo una segunda instancia que dispara unos segundos después (por desfase de reloj o un arranque tarde) la encontraba libre y la repetía. Si una instancia se cae a mitad de una tarea, el bloqueo dura hasta su máximo (1 h en las nocturnas), así nadie repite una corrida que pudo quedar a medias. Las tareas corren en un grupo de 4 hilos (`spring.task.scheduling.pool.size`): con el hilo único de Spring, el relay esperaba a que terminara el matchmaking.
- **Observabilidad (revisión).** `/actuator/health` (con liveness y readiness) para el healthcheck del contenedor, `/actuator/metrics` y un id de correlación (`X-Correlation-Id`) en cada línea de log y en las llamadas a otros servicios.

| Hora | Proceso |
|---|---|
| 00:00 | Aviso de inactividad a donantes |
| 03:00 | Reconciliación de `entidad_recepcion_diaria` |
| 03:30 | Matchmaking nocturno (rankings) |
| 04:00 | Despacho de las donaciones asignadas al broker de logística |
| 04:30 | Limpieza de los avisos ya enviados |
| cada 5 s | Envío de los avisos pendientes de la bandeja de salida |

---

## 10. Límites conocidos y pendientes

- **Medido en H2, no en PostgreSQL.** La cantidad de sentencias es la misma, pero no se corrió `EXPLAIN` sobre PostgreSQL para confirmar que el planificador use cada índice.
- **Tests contra PostgreSQL solo con Docker.** `PostgresIntegracionTest` y `MigracionDeDatosPostgresTest` corren las migraciones, el código de error de la clave duplicada, las sentencias nativas y el bloqueo de tareas contra PostgreSQL 16 con Testcontainers; sin Docker se saltean. Los mismos casos de migración corren siempre contra H2. Con Docker Engine 29, Testcontainers necesita la versión 1.44 de la API de Docker (la fija el `pom.xml` del servicio para los tests).
- **Probado con el sistema completo**: PostgreSQL 16, RabbitMQ, el broker, las dos logísticas, Notificaciones e Incentivos, con una y con dos instancias de Donaciones (cada tarea corrió en una sola, el relay nunca se superpuso y, con RabbitMQ o el broker caídos, el servicio siguió atendiendo y los avisos salieron solos al volver).
- **Incentivos no tolera avisos repetidos.** La entrega es "al menos una vez" (sección 8): si Incentivos procesa un aviso pero responde después del timeout de 5 s, el aviso se reintenta. Incentivos guarda las donaciones de cada persona con una restricción de unicidad y ante el repetido responde 500. En la prueba pasó con una persona con 1.500 donaciones, para la que cada pedido a Incentivos tardaba varios segundos. Se resuelve del lado de Incentivos, haciendo idempotente el registro de una donación.
- **Borrado sin FK.** El bloqueo de borrados es una verificación del servicio. Si se registra una donación justo mientras se borra a su donante, podría quedar huérfana. Una FK con `ON DELETE RESTRICT` lo cerraría del lado de la base.
- **Seguridad**: corresponde a la Entrega 6.

---

## Anexo: filas para la hoja "Justificaciones diseño tp anual" (pestaña ENTREGA 5)

Mismas columnas que las pestañas anteriores; el ámbito usa los valores de la hoja (Objetos / Arquitectura / Persistencia / Otro).

| Ámbito de decisión | Componente/s impactado/s | Decisión | Otras alternativas | Justificación de la decisión |
|---|---|---|---|---|
| Persistencia | servicio-donaciones (`donaciones_db`) | Base relacional (PostgreSQL 16, `donaciones_db`) con ORM (Spring Data JPA + Hibernate) | Base documental (MongoDB) como Logística | Entidades con identidad y relaciones obligatorias; reglas de integridad (email único, borrados bloqueados) y transacciones que abarcan varias tablas. |
| Persistencia | RankingPrecalculado, PuestoRanking, CandidatosAsignacionRepository, AsignacionBatchService | Tablas de lectura `ranking_precalculado` + `ranking_puesto` (algoritmo, posición), recalculadas a las 03:30 y reemplazadas de una vez, descartando las donaciones que dejaron el depósito durante la corrida; al leerlas, cada ranking se completa con todos los algoritmos | Calcular a demanda; vista materializada; columna JSON; mapa en memoria; una tabla por algoritmo | Bandeja de 3.000 a 7 sentencias; sobrevive a reinicios, es la misma en todas las instancias y un algoritmo nuevo no cambia el esquema. |
| Persistencia | RecepcionDiariaEntidad, DonacionesRepository, AlgoritmoPrioridadSubAtendidos, ReconciliacionRecepcionesService | Contador `entidad_recepcion_diaria (entidad, fecha, cantidad)`, actualizado en la transacción de la asignación y reconciliado a las 03:00 | `COUNT ... GROUP BY` sobre `donacion`; trigger; contador mensual | Ranking a demanda de 2.603 a 9 sentencias; la lectura no crece con el historial. Asignar cuesta 14 sentencias. |
| Persistencia | Donacion (`estado`, `subcategoria_*`) | `donacion.estado` y `donacion.subcategoria_*` guardados (derivables de `cambio_estado` y `bien`) | Derivarlos con subconsulta o JOIN | Todos los listados y procesos filtran por esas columnas; se actualizan en la misma transacción que su origen. |
| Persistencia | Donacion, PersonaDonante, RecepcionDiariaEntidad, RankingPrecalculado, MensajeSaliente y colecciones | Índices en `donacion` (5), `persona_donante` (2), UNIQUE de email, FK de colecciones, `entidad_recepcion_diaria (fecha)`, `ranking_puesto (id_donacion)` y `mensaje_saliente (estado, proximo_intento)` | Sin índices (solo PK) | Cada índice corresponde a un filtro, orden o proceso concreto (ver sección 4). |
| Arquitectura | DonacionesRepository, PersonaDonanteRepository, EntidadesBeneficiariasRepository, Pagina, SolicitudPagina, PaginaResponseDTO | Filtros con `Specification` y paginación propia (page 0, size 20, máx. 100, 400 fuera de rango) en donaciones, donantes y entidades | `Page` de Spring en la API; keyset; `JOIN FETCH` | `GET /donantes` de 401 a 3 sentencias; `GET /donaciones` de 121 a 4; contrato de API independiente del framework. |
| Persistencia | Entidades JPA, `application.properties` | Colecciones `LAZY` con `default_batch_fetch_size=100` global | `EAGER` con `@BatchSize` en cada colección; DTO liviano para listados | Cada caso de uso lee solo lo que usa; una colección nueva queda cubierta sin anotarla. |
| Persistencia | CsvImportService, EntidadPersistible (`Persistable`) | `Persistable`, `jdbc.batch_size=50`, lotes de 500 por transacción con búsqueda por `IN` | `COPY` nativo; una transacción por archivo | CSV de 20.000 filas: de 90,3 s a 8-12 s (80.040 a 413 sentencias; 17,9 s contra PostgreSQL); reimportación de 161,6 s a 1,9-3,4 s. |
| Persistencia | PersonaDonante, Representante, Email, NormalizadorEmail, GlobalExceptionHandler | `UNIQUE` + forma canónica (trim, NFC, minúsculas), inmutable, 409 ante duplicado | Solo validación en el servicio; `UNIQUE (lower(email))` | Cubre altas concurrentes y diferencias de mayúsculas o Unicode. |
| Objetos | Documento, TipoDocumento, DonanteCsvRowParser | Documento como objeto de valor validado por tipo: DNI de 7 u 8 dígitos, CUIT y CUIL de 11 dígitos (por su forma), sin puntos ni guiones; una organización con CUIT | Dos `String` sin reglas; exigir el dígito verificador del CUIT | Datos válidos en la base sin rechazar los del enunciado: 580 de los 6.055 CUIT del CSV y el CUIT de ejemplo (30-12345678-9) no cumplen el dígito verificador. |
| Persistencia | EntidadPersistible (`version`), PersonaDonante (`ultimaInteraccion`), GlobalExceptionHandler | Bloqueo optimista en las raíces de agregado, 409 ante modificación concurrente; la última interacción de la persona fuera de la versión | Bloqueo pesimista; sin control; versión también para la interacción | Dos pedidos simultáneos no se pisan y la fila no queda bloqueada mientras dura el pedido; dos donaciones simultáneas de la misma persona se guardan las dos. |
| Persistencia | PersonaDonanteService, EntidadesBeneficiariasService | 409 si la persona o entidad tiene donaciones | FK con `ON DELETE RESTRICT` | Respeta el límite de agregado (referencia por id) y devuelve un error de dominio claro. |
| Persistencia | Donacion, DonacionComandosService | Baja lógica (`fecha_baja` + `@SQLRestriction`) | Borrado físico | Conserva la donación y su historial; deja de aparecer en las consultas y descuenta su recepción. |
| Arquitectura | Eventos de dominio, AvisosOyente, BandejaDeSalida, RelayDeMensajes, RelayProgramado | Bandeja de salida (outbox): el aviso se guarda en la transacción del cambio y un relay lo envía con reintentos, un hilo por destino y con un corte de 30 s dentro de su bloqueo de 1 min | Enviar dentro del caso de uso; enviar después del commit; un relay que toma los más viejos sin mirar el destino | No se pierden avisos si el otro servicio no responde ni salen avisos de cambios que no se guardaron; un servicio lento no demora los avisos de los demás y dos instancias nunca envían a la vez. |
| Persistencia | `db/migration` (Flyway), `application.properties` | Migraciones versionadas (V1 esquema de la Entrega 5, V2-V4 cambios) y `ddl-auto=validate` | `ddl-auto=update`; Liquibase | Los cambios de esquema quedan versionados y migran los datos; un mapeo sin su migración falla al arrancar. |
| Arquitectura | AsignacionBatchService, PlanificacionEntregasService, InactividadDonantesService, BloqueoDeTareasConfig (ShedLock) | Procesos nocturnos por lotes de 100 (recorridos por id, o el aviso de inactividad de a 100 personas), cada lote en su transacción, y una sola instancia por tarea, que retiene el bloqueo al menos 5 min; 4 hilos para las tareas | Cargar todo en una transacción; correr en todas las instancias; liberar el bloqueo al terminar | Memoria, transacciones y bloqueos de filas acotados; un lote que falla no frena a los demás; con dos instancias no se despacha ni se avisa dos veces, aunque una dispare unos segundos más tarde. |
