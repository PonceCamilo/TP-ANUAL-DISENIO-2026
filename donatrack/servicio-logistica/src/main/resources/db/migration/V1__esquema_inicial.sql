-- Esquema inicial de logistica_db (base propia de servicio-logistica),
-- según el DER de Logística, con claves primarias UUID.
--
-- id_donacion e id_entidad_beneficiaria son referencias lógicas a datos de
-- servicio-donaciones: se guardan como UUID sin foreign key, porque esos datos
-- viven en otra base (database per service).
--
-- Los enums (estado_*, motivo_fallo) se guardan como texto con un CHECK de los
-- valores válidos. Si se agrega un valor a un enum de Java, hay que agregarlo
-- también al CHECK en una migración nueva.
--
-- Una vez aplicada, esta migración no se modifica: los cambios de esquema van
-- en un archivo nuevo (V2__..., V3__...).

CREATE TABLE direccion (
    id_direccion  UUID         PRIMARY KEY,
    calle         VARCHAR(150) NOT NULL,
    numero        INTEGER,
    localidad     VARCHAR(100),
    provincia     VARCHAR(100),
    codigo_postal VARCHAR(20)
);

CREATE TABLE camion (
    id_camion            UUID           PRIMARY KEY,
    patente              VARCHAR(20)    NOT NULL UNIQUE,
    capacidad_volumen_m3 NUMERIC(10, 2),
    altura_m             NUMERIC(5, 2),
    capacidad_carga_kg   NUMERIC(10, 2),
    estado               VARCHAR(30)    NOT NULL
        CHECK (estado IN ('DISPONIBLE', 'EN_RUTA', 'MANTENIMIENTO'))
);

CREATE TABLE lote_planificacion (
    id_lote           UUID         PRIMARY KEY,
    estado            VARCHAR(30)  NOT NULL
        CHECK (estado IN ('ENVIADO', 'EN_PROCESO', 'COMPLETADO', 'ERROR')),
    token_correlacion VARCHAR(100) NOT NULL UNIQUE,
    fecha_envio       TIMESTAMP    NOT NULL,
    fecha_respuesta   TIMESTAMP
);

CREATE TABLE lote_camion (
    id_lote   UUID NOT NULL REFERENCES lote_planificacion (id_lote),
    id_camion UUID NOT NULL REFERENCES camion (id_camion),
    PRIMARY KEY (id_lote, id_camion)
);

-- Snapshot de cada donación enviada al proveedor de ruteo en el lote.
CREATE TABLE donacion_lote (
    id_donacion_lote        UUID PRIMARY KEY,
    id_donacion             UUID NOT NULL,
    id_entidad_beneficiaria UUID NOT NULL,
    id_lote                 UUID NOT NULL REFERENCES lote_planificacion (id_lote),
    id_direccion_entrega    UUID NOT NULL REFERENCES direccion (id_direccion)
);

CREATE TABLE ruta (
    id_ruta      UUID        PRIMARY KEY,
    id_lote      UUID        NOT NULL REFERENCES lote_planificacion (id_lote),
    id_camion    UUID        NOT NULL REFERENCES camion (id_camion),
    estado       VARCHAR(30) NOT NULL
        CHECK (estado IN ('PLANIFICADA', 'INICIADA', 'FINALIZADA')),
    fecha_inicio TIMESTAMP
);

CREATE TABLE parada (
    id_parada               UUID    PRIMARY KEY,
    id_entidad_beneficiaria UUID    NOT NULL,
    id_direccion            UUID    NOT NULL REFERENCES direccion (id_direccion),
    id_ruta                 UUID    NOT NULL REFERENCES ruta (id_ruta),
    orden                   INTEGER NOT NULL
);

-- La ruta, el camión y la entidad de una entrega se obtienen a través de su parada.
CREATE TABLE entrega (
    id_entrega    UUID        PRIMARY KEY,
    id_donacion   UUID        NOT NULL,
    id_parada     UUID        NOT NULL REFERENCES parada (id_parada),
    estado        VARCHAR(30) NOT NULL
        CHECK (estado IN ('LISTO_PARA_ENTREGAR', 'EN_TRASLADO', 'ENTREGADA', 'NO_RECIBIDA')),
    observacion   TEXT,
    fecha_entrega TIMESTAMP
);

-- Historial de estados de cada entrega.
CREATE TABLE cambio_estado_entrega (
    id_cambio    UUID        PRIMARY KEY,
    id_entrega   UUID        NOT NULL REFERENCES entrega (id_entrega),
    estado       VARCHAR(30) NOT NULL
        CHECK (estado IN ('LISTO_PARA_ENTREGAR', 'EN_TRASLADO', 'ENTREGADA', 'NO_RECIBIDA')),
    motivo_fallo VARCHAR(30)
        CHECK (motivo_fallo IN ('ENTIDAD_AUSENTE', 'DIRECCION_INCORRECTA', 'RECHAZADA_POR_ENTIDAD',
                                'MERCADERIA_ROTA', 'MERCADERIA_PERDIDA', 'ROBO')),
    observacion  TEXT,
    fecha_hora   TIMESTAMP   NOT NULL
);

-- Fotos de comprobante de la confirmación de una entrega, en orden.
CREATE TABLE foto_comprobante_entrega (
    id_entrega UUID          NOT NULL REFERENCES entrega (id_entrega),
    posicion   INTEGER       NOT NULL,
    url_foto   VARCHAR(2048) NOT NULL,
    PRIMARY KEY (id_entrega, posicion)
);

-- Índices para las consultas de los repositorios.
CREATE INDEX idx_ruta_camion        ON ruta (id_camion);
CREATE INDEX idx_parada_ruta        ON parada (id_ruta);
CREATE INDEX idx_entrega_parada     ON entrega (id_parada);
CREATE INDEX idx_entrega_estado     ON entrega (estado);
CREATE INDEX idx_donacion_lote_lote ON donacion_lote (id_lote);
CREATE INDEX idx_cambio_entrega     ON cambio_estado_entrega (id_entrega);
