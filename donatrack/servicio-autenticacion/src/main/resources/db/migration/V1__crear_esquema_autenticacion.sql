CREATE TABLE cuenta (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    email VARCHAR(150) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    rol VARCHAR(255) NOT NULL CHECK (rol IN ('DONANTE', 'ENTIDAD_BENEFICIARIA', 'CHOFER', 'ADMINISTRADOR')),
    password_hash VARCHAR(60),
    UNIQUE (email, rol)
);

CREATE TABLE cuenta_donante (
    cuenta_id UUID PRIMARY KEY REFERENCES cuenta (id),
    tipo_donante VARCHAR(255) NOT NULL CHECK (tipo_donante IN ('HUMANA', 'JURIDICA')),
    donante_id UUID UNIQUE,
    google_sub VARCHAR(255) UNIQUE
);

CREATE TABLE cuenta_entidad_beneficiaria (
    cuenta_id UUID PRIMARY KEY REFERENCES cuenta (id),
    entidad_beneficiaria_id UUID UNIQUE,
    google_sub VARCHAR(255) UNIQUE
);

CREATE TABLE cuenta_chofer (
    cuenta_id UUID PRIMARY KEY REFERENCES cuenta (id),
    camion_id UUID
);

CREATE TABLE cuenta_administrador (
    cuenta_id UUID PRIMARY KEY REFERENCES cuenta (id)
);
