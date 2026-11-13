-- Enlaces de activación (Entrega 6, requisito 20): las personas que cargó la
-- administración eligen su contraseña con un enlace que reciben por email.
-- Se guarda solo el hash SHA-256 del código.
CREATE TABLE activacion (
    id UUID PRIMARY KEY,
    cuenta_id UUID NOT NULL REFERENCES cuenta (id),
    codigo_hash VARCHAR(64) NOT NULL UNIQUE,
    creada_en TIMESTAMP(6) NOT NULL,
    vence_en TIMESTAMP(6) NOT NULL,
    usada_en TIMESTAMP(6)
);

CREATE INDEX idx_activacion_cuenta ON activacion (cuenta_id, creada_en);
