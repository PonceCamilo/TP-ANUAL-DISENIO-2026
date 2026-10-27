# Servicio de Donaciones: justificaciones de la Entrega 5

La Entrega 5 pide persistir el modelo de cada servicio (relacional con ORM, salvo Logística, que va a una base documental con ODM) y aplicar **estrategias de desnormalización** donde mejoren las lecturas. Además hay que actualizar el modelo de dominio, el DER, las justificaciones y el diagrama de componentes.

Este documento cubre solo `servicio-donaciones`. Los diagramas están en esta misma carpeta:

| Archivo | Contenido |
|---|---|
| `donaciones-modelo.puml/.svg/.pdf` | Modelo de dominio (paquete `models`) |
| `donaciones-servicios.puml/.svg/.pdf` | Services, repositorios, utilitarios e integraciones |
| `donaciones-der.puml/.svg/.pdf` | DER físico de `donaciones_db`, tal como lo genera Hibernate |
| `donaciones-componentes.puml/.svg/.pdf` | Diagrama de componentes del servicio |

---

## 1. Modelo de persistencia: relacional con ORM

**Decisión.** Donaciones sigue en PostgreSQL 16 (base `donaciones_db`, en el servidor compartido del `docker-compose`), con Spring Data JPA + Hibernate 6 como ORM.

**Por qué.** Los datos del servicio son entidades con identidad propia y relaciones obligatorias: una donación pertenece a una persona donante, se asigna a una entidad beneficiaria y tiene historial de estados. Estas son las reglas que importan:

- el email de una persona es único;
- no se puede borrar una persona o entidad que tenga donaciones;
- el estado de una donación cambia de forma transaccional junto con su historial;
- una asignación y su contador de recepciones se guardan juntos o no se guardan.

Una base relacional las expresa con restricciones y transacciones. Un modelo documental obligaría a elegir un agregado raíz (¿la persona con sus donaciones? ¿la entidad con las que recibió?) y a repetir datos en los dos lados.

**Alternativa descartada.** MongoDB, como Logística. Logística guarda documentos que se leen y escriben enteros (una entrega con su ruta y sus eventos), así que el documental le sirve. Las consultas de Donaciones cruzan agregados: donaciones por donante, por entidad y por estado, y rankings que combinan donaciones con necesidades de todas las entidades.

---

## 2. Cómo se midió

Antes de cambiar algo se midió cuántas sentencias SQL ejecuta cada operación. Con eso cada decisión de este documento se apoya en un número y no en una intuición.

- **Test de medición**: `MedicionConsultasTest` (en `src/test/.../medicion`). Levanta el servicio completo sobre H2 en modo PostgreSQL y carga 200 donantes, 20 entidades y 60 donaciones (30 asignadas). Cuenta las sentencias con las estadísticas de Hibernate (`prepareStatementCount`). Quedó como **test de regresión**: cada operación tiene un tope y el test falla si alguien vuelve a introducir un N+1.
- **Importación masiva**: `ImportacionCsvMedicionTest` importa el CSV real de 20.000 filas. Solo corre si se pasa `-Ddonatrack.csv.medicion=<ruta del CSV>`, porque el archivo no se sube al repo.
- **Qué vale de los números**: la cantidad de sentencias no depende del motor, así que es la métrica que se compara. Los milisegundos son de H2 en memoria y solo sirven como referencia: en PostgreSQL, con red de por medio, cada sentencia ahorrada pesa más.

### Resultados (sentencias SQL por operación)

| Operación | Línea base | Lecturas resueltas en la base | + ranking persistido y contador | Final (escrituras) | Tope del test |
|---|---:|---:|---:|---:|---:|
| Matchmaking nocturno (batch) | 75.091 | 1.808 | 852 | **192** | 400 |
| `GET /donaciones/{id}/candidatos` | 2.603 | 72 | 13 | **13** | 30 |
| `GET /donaciones/candidatos` (bandeja) | 3.000 | 5 | 9 | **9** | 20 |
| `GET /donaciones` (y con filtros) | 121 | 3-4 | 3-4 | **3-4** | 10 |
| `GET /donantes` | 401 | 4 | 4 | **4** | 10 |
| Proceso de inactividad (batch) | 401 | 3 | 3 | **3** | 10 |
| `POST /donaciones` (4 bienes) | 20 | 20 | 20 | **7** | 15 |
| `PATCH /donaciones/{id}/asignar` | 13 | 13 | 23 | **23** | 35 |
| `POST /donantes/import` (300 altas) | 1.200 | 1.200 | 1.200 | **6** | 30 |
| `POST /donantes/import` (300 actualizaciones) | 900 | 900 | 900 | **7** | 30 |

| CSV de 20.000 filas | Antes | Después |
|---|---|---|
| Primera importación | 90,3 s · 80.040 sentencias | **7,9 s · 439 sentencias** |
| Reimportación (todas actualizaciones) | 161,6 s · 72.162 sentencias | **2,6 s · 693 sentencias** |

La única operación que empeora es asignar (13 → 23): es el costo de mantener el contador desnormalizado (sección 3.2), y se acepta a propósito.

---

## 3. Desnormalizaciones

Las tablas desnormalizadas se marcan en amarillo en el DER y las columnas derivadas, con `D`.

### 3.1 Ranking de candidatas precalculado (`ranking_precalculado`, `ranking_coincidencia`, `ranking_semantico`, `ranking_subatendidos`)

**Problema.** Para recomendar a qué entidad asignar una donación corren dos algoritmos: compatibilidad semántica y prioridad a entidades sub-atendidas. Cada uno recorre todas las entidades con sus campañas y necesidades. El proceso nocturno guardaba el resultado en un `ConcurrentHashMap`, que tiene dos problemas:

- se perdía al reiniciar el servicio, y la bandeja quedaba vacía hasta la noche siguiente;
- cada instancia del servicio tenía su propia bandeja.

**Decisión.** El resultado se persiste como una tabla de lectura: una fila por donación con la fecha del cálculo, y una tabla por lista de candidatas, con la posición en el ranking como columna de orden (`@ElementCollection` + `@OrderColumn`). `ResultadoAsignacion` pasa a ser un `@Embeddable` (id de entidad y puntaje). La bandeja (`GET /donaciones/candidatos`) lo lee tal cual: con `@Fetch(SUBSELECT)` toda la bandeja cuesta 4 consultas, no 4 por donación.

**Cómo se mantiene consistente.**

- El proceso de las 03:30 borra los rankings de la corrida anterior y los recalcula: si las necesidades cambiaron durante el día, una recomendación vieja es peor que ninguna.
- Una donación sale de la bandeja en cuanto deja de estar en depósito: al asignarla, al borrarla o al pasar a otro estado (por ejemplo, `VENCIDA`). En los tres casos se borra su ranking.
- `fecha_calculo` le dice a quien administra qué tan vigente es lo que está viendo.

**Costo.** Durante el día el ranking puede quedar desactualizado respecto de las necesidades. Se acepta porque el enunciado plantea el matchmaking como proceso nocturno. Las donaciones que entran al depósito durante el día (por ejemplo, después de una entrega fallida) recién tienen ranking en la corrida siguiente.

**Alternativas descartadas.**

- *Calcular a demanda*: es lo que hace `GET /donaciones/{id}/candidatos` para una donación (13 sentencias). Para la bandeja completa serían los dos algoritmos por cada donación en depósito.
- *Vista materializada en PostgreSQL*: los algoritmos están en Java (Strategy + Template Method). Pasarlos a SQL duplicaría la lógica del dominio.
- *Una columna JSON con las tres listas*: menos tablas, pero se pierde el tipado y no se puede consultar por entidad.

### 3.2 Recepciones por entidad y día (`entidad_recepcion_diaria`)

**Problema.** El algoritmo de prioridad a sub-atendidas necesita saber cuántas donaciones recibió cada entidad en los últimos 3 meses. Lo calculaba leyendo todas las donaciones, con sus bienes e historial, una vez por entidad. Con los datos de prueba, un solo ranking costaba más de mil sentencias.

**Decisión.** Un contador con clave `(entidad_id, fecha)` y la cantidad de donaciones asignadas ese día. El algoritmo lo lee en una sola consulta agregada para todas las entidades. Para eso `AlgoritmoAsignacionBase` tiene un nuevo paso del template method, `prepararPuntaje(donacion, entidades)`, que se ejecuta una vez por ranking.

- **Diario y no mensual**, para respetar exactamente la regla del algoritmo ("últimos 3 meses" contados día por día). Un contador mensual obligaría a redondear el trimestre.

**Cómo se mantiene consistente.**

- `DonacionesRepository.guardarAsignacion` guarda la donación y mueve el contador **en la misma transacción**. Si la donación ya estaba asignada a otra entidad u otra fecha, descuenta de la anterior y suma a la nueva. La notificación al donante sale recién después de confirmar la transacción.
- Borrar una donación asignada descuenta su recepción.
- Una **reconciliación nocturna** (03:00, antes del matchmaking de las 03:30) vacía el contador y lo recalcula desde `donacion` con un `INSERT ... SELECT ... GROUP BY`. Si alguna vez se desfasa, se corrige antes de que lo use el algoritmo.

**Costo.** Asignar pasa de 13 a 23 sentencias: buscar la asignación previa y actualizar o insertar el contador.

**Alternativas descartadas.**

- *Un `COUNT ... GROUP BY` sobre `donacion` en cada ranking*, usando el índice `(id_entidad_beneficiaria, fecha_asignacion)`. Es válida y también resuelve el N+1 en una consulta. Se eligió el contador porque su lectura queda acotada a entidades × 90 días y no crece con el historial de donaciones, que solo aumenta.
- *Trigger en la base*: deja lógica de negocio en PostgreSQL y no corre igual en H2, donde están los tests.

### 3.3 Desnormalizaciones que ya existían y ahora quedan documentadas

| Columna | Se podría derivar de | Por qué se guarda |
|---|---|---|
| `donacion.estado` | El último registro de `cambio_estado` | Todos los listados y procesos filtran por estado. Derivarlo pediría una subconsulta por fila. Se actualiza en el mismo objeto y la misma transacción que el historial (`Donacion.cambiarEstado`). |
| `donacion.subcategoria_tipo` | La subcategoría de sus bienes | `CargaDonacion` segmenta la carga por subcategoría, así que todos los bienes de una donación comparten la misma. Filtrar por subcategoría no necesita un JOIN con `bien`. |
| `donacion.subcategoria_tipo_normalizado` | `subcategoria_tipo` (trim + minúsculas) | Es la forma de búsqueda indexada: "Sillas" y " sillas " son la misma subcategoría. |

---

## 4. Índices

Hibernate los crea a partir de `@Table(indexes = ...)`. PostgreSQL no indexa solo las columnas FK, así que también se declaran.

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
| Columnas FK de `bien`, `cambio_estado`, `medio_de_contacto` (3), `representante` (2), `campania`, `necesidad` | Carga de colecciones por lotes (`@BatchSize`) |

---

## 5. Lecturas: consultas en la base y paginación

**Problema.** `GET /donaciones` traía todas las donaciones y filtraba en memoria. `GET /donantes` devolvía la tabla entera con sus contactos: después de importar el CSV eran 20.000 personas en una respuesta. Además, cada colección `EAGER` se cargaba con una consulta por fila (N+1).

**Decisiones.**

- **Filtros en la base**: `JpaSpecificationExecutor` arma el `WHERE` según los filtros presentes (estado, donante, subcategoría y, nuevo, entidad).
- **Paginación siempre activa**:
  - `page` empieza en 0; `size` vale 20 si no se indica y como máximo 100.
  - Un pedido fuera de rango o un parámetro no numérico responde **400**. No se corrige en silencio, porque es un error de quien llama.
  - El orden es estable: donaciones por fecha descendente y luego id; donantes por email.
- **Página propia** (`Pagina` en el dominio, `PaginaResponseDTO` en la API: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`), en lugar de devolver el `Page` de Spring. El contrato de la API no depende del framework.
- **`@BatchSize(100)`** en las colecciones `EAGER` (y en `MedioDeContacto`). Cargar una página cuesta un `IN (...)` por colección, no una consulta por fila.
- **Carga de entidades en bloque** (`obtenerPorIds`) para armar la bandeja y los candidatos. El batch nocturno carga las entidades una sola vez por corrida.
- **Inactividad en la base**: `findByUltimaInteraccionBeforeOrUltimaInteraccionIsNull` reemplaza el recorrido en memoria de todas las personas.

**Alternativas descartadas.**

- *Paginación por cursor (keyset)*: escala mejor en páginas profundas, pero complica la API. Para el volumen de un panel de administración alcanza con `OFFSET`, y el índice por fecha cubre el orden.
- *`JOIN FETCH` de las colecciones*: con varias listas en la misma entidad genera producto cartesiano, y Hibernate no lo permite con más de un `bag`.

---

## 6. Escrituras: altas directas, lotes JDBC e importación CSV por lotes

**Problema.** Los ids son UUID asignados por la aplicación, así que Spring Data no podía saber si una entidad era nueva: hacía un `merge`, con un `SELECT` antes de cada `INSERT`. La importación CSV procesaba fila por fila (buscar por email y guardar), unas 4 sentencias por fila: 90 s para 20.000 filas.

**Decisiones.**

- `PersonaDonante` y `Donacion` implementan **`Persistable<UUID>`**. Un indicador transitorio `nueva` vale `true` hasta `@PostPersist`/`@PostLoad`, así que las altas van directo al `INSERT`.
- **Lotes JDBC**: `hibernate.jdbc.batch_size=50` con `order_inserts`/`order_updates`. Funciona porque los ids no son `IDENTITY`, que desactiva el batch.
- **CSV por lotes de 500 filas, cada uno en su transacción** (`TransactionOperations`):
  - Los emails del lote se buscan en una sola consulta (`obtenerPorEmails`, con `IN`) y se guardan todos juntos (`guardarTodos`).
  - Si el lote falla, se reprocesa fila por fila para informar en qué línea estuvo el error.
  - Al actualizar, el teléfono se reemplaza solo si cambió.
  - En el alta, la persona queda con el email como medio de contacto predeterminado y el teléfono como contacto `TELEFONO`.

**Alternativas descartadas.**

- *`COPY` o `INSERT` nativos masivos*: más rápidos, pero se saltean el dominio (validaciones, estado inicial, normalización del email).
- *Todo el archivo en una transacción*: una fila mala aborta las 20.000, y la transacción retiene bloqueos durante toda la importación.

---

## 7. Integridad

| Decisión | Detalle | Alternativa descartada |
|---|---|---|
| **Email único en la base** | `persona_donante.email` tiene `UNIQUE`. Antes la unicidad dependía de una consulta previa: dos altas simultáneas, o el mismo email con otras mayúsculas, dejaban duplicados. Si la base rechaza el `INSERT` (SQLState 23505), el handler responde **409**. | Solo validar en el servicio (no cubre la concurrencia). |
| **Forma canónica del email** | `NormalizadorEmail`: trim, Unicode NFC y minúsculas (`Locale.ROOT`). Se aplica en `@PrePersist`/`@PreUpdate`, venga la persona de la API, del builder o del CSV. Los contactos `EMAIL` se guardan igual. Se aceptan emails con tildes. | Índice `UNIQUE` sobre `lower(email)`: Hibernate no lo genera desde las anotaciones y no resuelve las variantes Unicode (NFC). |
| **Email inmutable** | Es la identidad de la persona y la clave de idempotencia del CSV: se fija en el alta y no se sincroniza con los contactos. | Copiar el último contacto `EMAIL`: cambiaría la identidad de la persona sin que nadie lo pida. |
| **Borrados con donaciones → 409** | No se puede borrar una persona o entidad con donaciones (`PersonaConDonacionesException`, `EntidadConDonacionesException`). `donacion` referencia a ambas por id (límite de agregado), sin FK. | FK real con `ON DELETE RESTRICT` (ver sección 9). |
| **Sin columnas duplicadas** | `campania.id_entidad` era una segunda copia del id de la entidad, que podía quedar distinta de la FK `entidad_id`. Ahora `Campania.idEntidad` se lee de `entidad_id` (`insertable = false, updatable = false`). | - |
| **Código muerto fuera del modelo** | `Categoria` y `TipoBien` no estaban mapeadas ni se usaban: se borraron. | - |

---

## 8. Esquema y procesos

- **`spring.jpa.hibernate.ddl-auto=update`**: el esquema se genera desde las entidades, y de ahí sale el DER. Se mantiene por el alcance del TP. En producción correspondería una herramienta de migraciones versionadas (Flyway o Liquibase).
- **Zona horaria de los procesos nocturnos**: los cuatro `@Scheduled` usan `America/Argentina/Buenos_Aires` (`FechaHoraArgentina.ZONA_ID`) y no la zona del servidor. La imagen `eclipse-temurin` del Dockerfile corre en UTC si no se le configura `TZ`, y ahí "medianoche" serían las 21 h de Argentina.

| Hora | Proceso |
|---|---|
| 00:00 | Aviso de inactividad a donantes |
| 03:00 | Reconciliación de `entidad_recepcion_diaria` |
| 03:30 | Matchmaking nocturno (rankings) |
| 04:00 | Despacho de las donaciones asignadas al broker de logística |

---

## 9. Límites conocidos y pendientes

- **Medido en H2, no en PostgreSQL.** La cantidad de sentencias es la misma, pero no se corrió `EXPLAIN` sobre PostgreSQL para confirmar que el planificador use cada índice.
- **No se probó con el stack de Docker.** La colección de Postman se verificó contra el servicio levantado localmente (87 requests, 172 tests en verde), sin PostgreSQL, RabbitMQ ni el broker. La carpeta 05, que necesita broker y logística, no se corrió.
- **Base existente de la Entrega 4.** No se probó `ddl-auto=update` sobre una base ya creada. Hibernate no borra columnas: la vieja `campania.id_entidad` quedaría sin uso. Si había emails duplicados, el `UNIQUE` no se puede crear. Para arrancar limpio: `docker compose down -v`.
- **Borrado sin FK.** El bloqueo de borrados es una verificación del servicio. Si se registra una donación justo mientras se borra a su donante, podría quedar huérfana. Una FK con `ON DELETE RESTRICT` lo cerraría del lado de la base.
- **Fuera del alcance de la Entrega 5:**
  - `Necesidad.cantidadRecibida` no se actualiza al asignar (`recibirDonacion` existe pero nadie la llama).
  - En la respuesta de una persona jurídica, el campo `tipo` (ONG, EMPRESA, ...) choca con el discriminador JSON `tipo` (`JURIDICA`) y la clave sale repetida.

---

## Anexo: filas para la hoja "Justificaciones diseño tp anual" (pestaña ENTREGA 5)

Mismas columnas que las pestañas anteriores; el ámbito usa los valores de la hoja (Objetos / Arquitectura / Persistencia / Otro).

| Ámbito de decisión | Componente/s impactado/s | Decisión | Otras alternativas | Justificación de la decisión |
|---|---|---|---|---|
| Persistencia | servicio-donaciones (`donaciones_db`) | Base relacional (PostgreSQL 16, `donaciones_db`) con ORM (Spring Data JPA + Hibernate) | Base documental (MongoDB) como Logística | Entidades con identidad y relaciones obligatorias; reglas de integridad (email único, borrados bloqueados) y transacciones que abarcan varias tablas. |
| Persistencia | RankingPrecalculado, CandidatosAsignacionRepository, AsignacionBatchService | Tablas de lectura `ranking_precalculado` + `ranking_coincidencia`/`semantico`/`subatendidos`, recalculadas a las 03:30 | Calcular a demanda; vista materializada; columna JSON; mapa en memoria | Bandeja de 3.000 a 9 sentencias; sobrevive a reinicios y es la misma en todas las instancias. |
| Persistencia | RecepcionDiariaEntidad, DonacionesRepository, AlgoritmoPrioridadSubAtendidos, ReconciliacionRecepcionesService | Contador `entidad_recepcion_diaria (entidad, fecha, cantidad)`, actualizado en la transacción de la asignación y reconciliado a las 03:00 | `COUNT ... GROUP BY` sobre `donacion`; trigger; contador mensual | Ranking a demanda de 72 a 13 sentencias; la lectura no crece con el historial. Asignar cuesta 23 sentencias en vez de 13. |
| Persistencia | Donacion (`estado`, `subcategoria_*`) | `donacion.estado` y `donacion.subcategoria_*` guardados (derivables de `cambio_estado` y `bien`) | Derivarlos con subconsulta o JOIN | Todos los listados y procesos filtran por esas columnas; se actualizan en la misma transacción que su origen. |
| Persistencia | Donacion, PersonaDonante, RecepcionDiariaEntidad y colecciones | 5 índices en `donacion`, 2 en `persona_donante`, UNIQUE de email, FK de colecciones y `entidad_recepcion_diaria (fecha)` | Sin índices (solo PK) | Cada índice corresponde a un filtro, orden o proceso concreto (ver sección 4). |
| Arquitectura | DonacionesRepository, PersonaDonanteRepository, Pagina, SolicitudPagina, PaginaResponseDTO | Filtros con `Specification`, paginación propia (page 0, size 20, máx. 100, 400 fuera de rango) y `@BatchSize(100)` | `Page` de Spring en la API; keyset; `JOIN FETCH` | `GET /donantes` de 401 a 4 sentencias; `GET /donaciones` de 121 a 4; contrato de API independiente del framework. |
| Persistencia | CsvImportService, PersonaDonante, Donacion (`Persistable`) | `Persistable`, `jdbc.batch_size=50`, lotes de 500 por transacción con búsqueda por `IN` | `COPY` nativo; una transacción por archivo | CSV de 20.000 filas: de 90,3 s a 7,9 s (80.040 a 439 sentencias); reimportación de 161,6 s a 2,6 s. |
| Persistencia | PersonaDonante, Email, NormalizadorEmail, GlobalExceptionHandler | `UNIQUE` + forma canónica (trim, NFC, minúsculas), inmutable, 409 ante duplicado | Solo validación en el servicio; `UNIQUE (lower(email))` | Cubre altas concurrentes y diferencias de mayúsculas o Unicode. |
| Persistencia | PersonaDonanteService, EntidadesBeneficiariasService | 409 si la persona o entidad tiene donaciones | FK con `ON DELETE RESTRICT` | Respeta el límite de agregado (referencia por id) y devuelve un error de dominio claro. |
