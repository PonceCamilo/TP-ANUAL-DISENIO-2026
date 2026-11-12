-- Completa el esquema de la versión 1 en una base creada ANTES de las migraciones
-- con una versión anterior de las entidades (por ejemplo, la de la Entrega 4).
--
-- No es una migración: lo ejecuta CompletarEsquemaDeBaseVieja, solo cuando la
-- base fue marcada en la versión 1 por baseline y todavía no tiene aplicada la 2.
-- Todo es idempotente: sobre una base que ya tiene el esquema completo no cambia nada.
--
-- Se genera a partir de V1__esquema_entrega_5.sql: tablas, columnas e índices.
-- Las columnas se agregan sin NOT NULL ni UNIQUE para no fallar sobre tablas con
-- filas, y las tablas que faltaban se crean sin claves foráneas.

-- Tablas que pueden faltar
create table if not exists bien (cantidad integer, es_nuevo boolean, fecha_vencimiento date, donacion_id uuid, id uuid not null, tipo_bien varchar(31) not null, descripcion varchar(255), foto varchar(255), subcategoria_tipo varchar(255), subcategoria_tipo_normalizado varchar(255), unidad varchar(255), primary key (id));
create table if not exists cambio_estado (fecha_hora timestamp(6) not null, donacion_id uuid, id uuid not null, justificacion varchar(1000), estado varchar(255) not null, estado_previo varchar(255), nombre_transicion varchar(255), primary key (id));
create table if not exists campania (fecha_fin date, fecha_inicio date, entidad_id uuid, id_campania uuid not null, descripcion_general varchar(1000), primary key (id_campania));
create table if not exists donacion (fecha_asignacion date, fecha_donacion timestamp(6), id uuid not null, id_donante uuid, id_entidad_beneficiaria uuid, descripcion varchar(1000), estado varchar(255) not null, id_seguimiento_logistica varchar(255), proveedor_logistica varchar(255), subcategoria_tipo varchar(255), subcategoria_tipo_normalizado varchar(255), primary key (id));
create table if not exists entidad_beneficiaria (direccion_numero integer, id uuid not null, direccion_calle varchar(255), direccion_codigo_postal varchar(255), localidad_nombre varchar(255), provincia_nombre varchar(255), razon_social varchar(255), primary key (id));
create table if not exists entidad_recepcion_diaria (fecha date not null, cantidad bigint not null, entidad_id uuid not null, primary key (fecha, entidad_id));
create table if not exists medio_de_contacto (entidad_id uuid, id uuid not null, persona_donante_id uuid, representante_id uuid, tipo_medio varchar(31) not null, valor varchar(255), primary key (id));
create table if not exists necesidad (cantidad_objetivo integer, cantidad_recibida integer, fecha_registro date, campania_id uuid, id uuid not null, descripcion varchar(1000), nombre varchar(255), primary key (id));
create table if not exists necesidad_extraordinaria (id uuid not null, primary key (id));
create table if not exists necesidad_recurrente (fecha_inicio_periodo date, id uuid not null, periodo varchar(255) check (periodo in ('SEMANAL','MENSUAL','CUATRIMESTRAL','ANUAL')), primary key (id));
create table if not exists persona_donante (direccion_numero integer, ultima_interaccion timestamp(6), id uuid not null, medio_predeterminado_id uuid unique, direccion_calle varchar(255), direccion_codigo_postal varchar(255), email varchar(255) unique, estado varchar(255) not null, localidad_nombre varchar(255), numero_documento varchar(255), provincia_nombre varchar(255), tipo_documento varchar(255), primary key (id));
create table if not exists persona_humana (fecha_nacimiento date, id uuid not null, apellido varchar(255), genero varchar(255) check (genero in ('MASCULINO','FEMENINO','NO_BINARIO','OTRO','PREFIERO_NO_DECIR')), nombre varchar(255), primary key (id));
create table if not exists persona_juridica (id uuid not null, razon_social varchar(255), rubro varchar(255), tipo varchar(255) check (tipo in ('GUBERNAMENTAL','ONG','EMPRESA','INSTITUCION')), primary key (id));
create table if not exists ranking_coincidencia (posicion integer not null, puntaje float(53), id_donacion uuid not null, id_entidad uuid, primary key (posicion, id_donacion));
create table if not exists ranking_precalculado (fecha_calculo timestamp(6), id_donacion uuid not null, primary key (id_donacion));
create table if not exists ranking_semantico (posicion integer not null, puntaje float(53), id_donacion uuid not null, id_entidad uuid, primary key (posicion, id_donacion));
create table if not exists ranking_subatendidos (posicion integer not null, puntaje float(53), id_donacion uuid not null, id_entidad uuid, primary key (posicion, id_donacion));
create table if not exists representante (entidad_id uuid, id uuid not null, persona_juridica_id uuid, apellido varchar(255), email varchar(255), nombre varchar(255), primary key (id));

-- Columnas que pueden faltar
alter table bien add column if not exists cantidad integer;
alter table bien add column if not exists es_nuevo boolean;
alter table bien add column if not exists fecha_vencimiento date;
alter table bien add column if not exists donacion_id uuid;
alter table bien add column if not exists id uuid;
alter table bien add column if not exists tipo_bien varchar(31);
alter table bien add column if not exists descripcion varchar(255);
alter table bien add column if not exists foto varchar(255);
alter table bien add column if not exists subcategoria_tipo varchar(255);
alter table bien add column if not exists subcategoria_tipo_normalizado varchar(255);
alter table bien add column if not exists unidad varchar(255);
alter table cambio_estado add column if not exists fecha_hora timestamp(6);
alter table cambio_estado add column if not exists donacion_id uuid;
alter table cambio_estado add column if not exists id uuid;
alter table cambio_estado add column if not exists justificacion varchar(1000);
alter table cambio_estado add column if not exists estado varchar(255);
alter table cambio_estado add column if not exists estado_previo varchar(255);
alter table cambio_estado add column if not exists nombre_transicion varchar(255);
alter table campania add column if not exists fecha_fin date;
alter table campania add column if not exists fecha_inicio date;
alter table campania add column if not exists entidad_id uuid;
alter table campania add column if not exists id_campania uuid;
alter table campania add column if not exists descripcion_general varchar(1000);
alter table donacion add column if not exists fecha_asignacion date;
alter table donacion add column if not exists fecha_donacion timestamp(6);
alter table donacion add column if not exists id uuid;
alter table donacion add column if not exists id_donante uuid;
alter table donacion add column if not exists id_entidad_beneficiaria uuid;
alter table donacion add column if not exists descripcion varchar(1000);
alter table donacion add column if not exists estado varchar(255);
alter table donacion add column if not exists id_seguimiento_logistica varchar(255);
alter table donacion add column if not exists proveedor_logistica varchar(255);
alter table donacion add column if not exists subcategoria_tipo varchar(255);
alter table donacion add column if not exists subcategoria_tipo_normalizado varchar(255);
alter table entidad_beneficiaria add column if not exists direccion_numero integer;
alter table entidad_beneficiaria add column if not exists id uuid;
alter table entidad_beneficiaria add column if not exists direccion_calle varchar(255);
alter table entidad_beneficiaria add column if not exists direccion_codigo_postal varchar(255);
alter table entidad_beneficiaria add column if not exists localidad_nombre varchar(255);
alter table entidad_beneficiaria add column if not exists provincia_nombre varchar(255);
alter table entidad_beneficiaria add column if not exists razon_social varchar(255);
alter table entidad_recepcion_diaria add column if not exists fecha date;
alter table entidad_recepcion_diaria add column if not exists cantidad bigint;
alter table entidad_recepcion_diaria add column if not exists entidad_id uuid;
alter table medio_de_contacto add column if not exists entidad_id uuid;
alter table medio_de_contacto add column if not exists id uuid;
alter table medio_de_contacto add column if not exists persona_donante_id uuid;
alter table medio_de_contacto add column if not exists representante_id uuid;
alter table medio_de_contacto add column if not exists tipo_medio varchar(31);
alter table medio_de_contacto add column if not exists valor varchar(255);
alter table necesidad add column if not exists cantidad_objetivo integer;
alter table necesidad add column if not exists cantidad_recibida integer;
alter table necesidad add column if not exists fecha_registro date;
alter table necesidad add column if not exists campania_id uuid;
alter table necesidad add column if not exists id uuid;
alter table necesidad add column if not exists descripcion varchar(1000);
alter table necesidad add column if not exists nombre varchar(255);
alter table necesidad_extraordinaria add column if not exists id uuid;
alter table necesidad_recurrente add column if not exists fecha_inicio_periodo date;
alter table necesidad_recurrente add column if not exists id uuid;
alter table necesidad_recurrente add column if not exists periodo varchar(255) check (periodo in ('SEMANAL','MENSUAL','CUATRIMESTRAL','ANUAL'));
alter table persona_donante add column if not exists direccion_numero integer;
alter table persona_donante add column if not exists ultima_interaccion timestamp(6);
alter table persona_donante add column if not exists id uuid;
alter table persona_donante add column if not exists medio_predeterminado_id uuid;
alter table persona_donante add column if not exists direccion_calle varchar(255);
alter table persona_donante add column if not exists direccion_codigo_postal varchar(255);
alter table persona_donante add column if not exists email varchar(255);
alter table persona_donante add column if not exists estado varchar(255);
alter table persona_donante add column if not exists localidad_nombre varchar(255);
alter table persona_donante add column if not exists numero_documento varchar(255);
alter table persona_donante add column if not exists provincia_nombre varchar(255);
alter table persona_donante add column if not exists tipo_documento varchar(255);
alter table persona_humana add column if not exists fecha_nacimiento date;
alter table persona_humana add column if not exists id uuid;
alter table persona_humana add column if not exists apellido varchar(255);
alter table persona_humana add column if not exists genero varchar(255) check (genero in ('MASCULINO','FEMENINO','NO_BINARIO','OTRO','PREFIERO_NO_DECIR'));
alter table persona_humana add column if not exists nombre varchar(255);
alter table persona_juridica add column if not exists id uuid;
alter table persona_juridica add column if not exists razon_social varchar(255);
alter table persona_juridica add column if not exists rubro varchar(255);
alter table persona_juridica add column if not exists tipo varchar(255) check (tipo in ('GUBERNAMENTAL','ONG','EMPRESA','INSTITUCION'));
alter table ranking_coincidencia add column if not exists posicion integer;
alter table ranking_coincidencia add column if not exists puntaje float(53);
alter table ranking_coincidencia add column if not exists id_donacion uuid;
alter table ranking_coincidencia add column if not exists id_entidad uuid;
alter table ranking_precalculado add column if not exists fecha_calculo timestamp(6);
alter table ranking_precalculado add column if not exists id_donacion uuid;
alter table ranking_semantico add column if not exists posicion integer;
alter table ranking_semantico add column if not exists puntaje float(53);
alter table ranking_semantico add column if not exists id_donacion uuid;
alter table ranking_semantico add column if not exists id_entidad uuid;
alter table ranking_subatendidos add column if not exists posicion integer;
alter table ranking_subatendidos add column if not exists puntaje float(53);
alter table ranking_subatendidos add column if not exists id_donacion uuid;
alter table ranking_subatendidos add column if not exists id_entidad uuid;
alter table representante add column if not exists entidad_id uuid;
alter table representante add column if not exists id uuid;
alter table representante add column if not exists persona_juridica_id uuid;
alter table representante add column if not exists apellido varchar(255);
alter table representante add column if not exists email varchar(255);
alter table representante add column if not exists nombre varchar(255);

-- Índices
create index if not exists idx_bien_donacion on bien (donacion_id);
create index if not exists idx_cambio_estado_donacion on cambio_estado (donacion_id);
create index if not exists idx_campania_entidad on campania (entidad_id);
create index if not exists idx_donacion_estado on donacion (estado);
create index if not exists idx_donacion_donante_estado on donacion (id_donante, estado);
create index if not exists idx_donacion_entidad_fecha on donacion (id_entidad_beneficiaria, fecha_asignacion);
create index if not exists idx_donacion_subcategoria on donacion (subcategoria_tipo_normalizado);
create index if not exists idx_donacion_fecha on donacion (fecha_donacion desc, id);
create index if not exists idx_recepcion_fecha on entidad_recepcion_diaria (fecha);
create index if not exists idx_contacto_persona on medio_de_contacto (persona_donante_id);
create index if not exists idx_contacto_representante on medio_de_contacto (representante_id);
create index if not exists idx_contacto_entidad on medio_de_contacto (entidad_id);
create index if not exists idx_necesidad_campania on necesidad (campania_id);
create index if not exists idx_persona_estado on persona_donante (estado);
create index if not exists idx_persona_ultima_interaccion on persona_donante (ultima_interaccion);
create index if not exists idx_representante_juridica on representante (persona_juridica_id);
create index if not exists idx_representante_entidad on representante (entidad_id);
