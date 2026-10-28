-- Esquema de la Entrega 5, tal como lo generaba Hibernate (ddl-auto=update) a
-- partir de las entidades de esa entrega. Es la línea de base: una base que ya
-- tiene estas tablas (creada antes de usar migraciones) se marca en la versión 1
-- sin ejecutar este script (spring.flyway.baseline-on-migrate), y una base vacía
-- arranca desde acá. Los cambios posteriores van en V2 y siguientes.

-- Tablas
create table bien (cantidad integer, es_nuevo boolean, fecha_vencimiento date, donacion_id uuid, id uuid not null, tipo_bien varchar(31) not null, descripcion varchar(255), foto varchar(255), subcategoria_tipo varchar(255), subcategoria_tipo_normalizado varchar(255), unidad varchar(255), primary key (id));
create table cambio_estado (fecha_hora timestamp(6) not null, donacion_id uuid, id uuid not null, justificacion varchar(1000), estado varchar(255) not null, estado_previo varchar(255), nombre_transicion varchar(255), primary key (id));
create table campania (fecha_fin date, fecha_inicio date, entidad_id uuid, id_campania uuid not null, descripcion_general varchar(1000), primary key (id_campania));
create table donacion (fecha_asignacion date, fecha_donacion timestamp(6), id uuid not null, id_donante uuid, id_entidad_beneficiaria uuid, descripcion varchar(1000), estado varchar(255) not null, id_seguimiento_logistica varchar(255), proveedor_logistica varchar(255), subcategoria_tipo varchar(255), subcategoria_tipo_normalizado varchar(255), primary key (id));
create table entidad_beneficiaria (direccion_numero integer, id uuid not null, direccion_calle varchar(255), direccion_codigo_postal varchar(255), localidad_nombre varchar(255), provincia_nombre varchar(255), razon_social varchar(255), primary key (id));
create table entidad_recepcion_diaria (fecha date not null, cantidad bigint not null, entidad_id uuid not null, primary key (fecha, entidad_id));
create table medio_de_contacto (entidad_id uuid, id uuid not null, persona_donante_id uuid, representante_id uuid, tipo_medio varchar(31) not null, valor varchar(255), primary key (id));
create table necesidad (cantidad_objetivo integer, cantidad_recibida integer, fecha_registro date, campania_id uuid, id uuid not null, descripcion varchar(1000), nombre varchar(255), primary key (id));
create table necesidad_extraordinaria (id uuid not null, primary key (id));
create table necesidad_recurrente (fecha_inicio_periodo date, id uuid not null, periodo varchar(255) check (periodo in ('SEMANAL','MENSUAL','CUATRIMESTRAL','ANUAL')), primary key (id));
create table persona_donante (direccion_numero integer, ultima_interaccion timestamp(6), id uuid not null, medio_predeterminado_id uuid unique, direccion_calle varchar(255), direccion_codigo_postal varchar(255), email varchar(255) unique, estado varchar(255) not null, localidad_nombre varchar(255), numero_documento varchar(255), provincia_nombre varchar(255), tipo_documento varchar(255), primary key (id));
create table persona_humana (fecha_nacimiento date, id uuid not null, apellido varchar(255), genero varchar(255) check (genero in ('MASCULINO','FEMENINO','NO_BINARIO','OTRO','PREFIERO_NO_DECIR')), nombre varchar(255), primary key (id));
create table persona_juridica (id uuid not null, razon_social varchar(255), rubro varchar(255), tipo varchar(255) check (tipo in ('GUBERNAMENTAL','ONG','EMPRESA','INSTITUCION')), primary key (id));
create table ranking_coincidencia (posicion integer not null, puntaje float(53), id_donacion uuid not null, id_entidad uuid, primary key (posicion, id_donacion));
create table ranking_precalculado (fecha_calculo timestamp(6), id_donacion uuid not null, primary key (id_donacion));
create table ranking_semantico (posicion integer not null, puntaje float(53), id_donacion uuid not null, id_entidad uuid, primary key (posicion, id_donacion));
create table ranking_subatendidos (posicion integer not null, puntaje float(53), id_donacion uuid not null, id_entidad uuid, primary key (posicion, id_donacion));
create table representante (entidad_id uuid, id uuid not null, persona_juridica_id uuid, apellido varchar(255), email varchar(255), nombre varchar(255), primary key (id));

-- Índices
create index idx_bien_donacion on bien (donacion_id);
create index idx_cambio_estado_donacion on cambio_estado (donacion_id);
create index idx_campania_entidad on campania (entidad_id);
create index idx_donacion_estado on donacion (estado);
create index idx_donacion_donante_estado on donacion (id_donante, estado);
create index idx_donacion_entidad_fecha on donacion (id_entidad_beneficiaria, fecha_asignacion);
create index idx_donacion_subcategoria on donacion (subcategoria_tipo_normalizado);
create index idx_donacion_fecha on donacion (fecha_donacion desc, id);
create index idx_recepcion_fecha on entidad_recepcion_diaria (fecha);
create index idx_contacto_persona on medio_de_contacto (persona_donante_id);
create index idx_contacto_representante on medio_de_contacto (representante_id);
create index idx_contacto_entidad on medio_de_contacto (entidad_id);
create index idx_necesidad_campania on necesidad (campania_id);
create index idx_persona_estado on persona_donante (estado);
create index idx_persona_ultima_interaccion on persona_donante (ultima_interaccion);
create index idx_representante_juridica on representante (persona_juridica_id);
create index idx_representante_entidad on representante (entidad_id);

-- Claves foráneas
alter table if exists bien add constraint FKn6ypidmb0tmc9387l1oyk900h foreign key (donacion_id) references donacion;
alter table if exists cambio_estado add constraint FK9i5deuffvbmdlh35byrnn5lbx foreign key (donacion_id) references donacion;
alter table if exists campania add constraint FKa5ftm5h64lifce2if1a6dj1c9 foreign key (entidad_id) references entidad_beneficiaria;
alter table if exists medio_de_contacto add constraint FKm0j7lmw7218xwcxcg7htgkxpc foreign key (entidad_id) references entidad_beneficiaria;
alter table if exists medio_de_contacto add constraint FKsr6qhq65ienmid250dph7ae3v foreign key (representante_id) references representante;
alter table if exists medio_de_contacto add constraint FK6cuvll0ko2m367r3toinbor2r foreign key (persona_donante_id) references persona_donante;
alter table if exists necesidad add constraint FKb3fvhhr6seibh978241s2s6yc foreign key (campania_id) references campania;
alter table if exists necesidad_extraordinaria add constraint FKl6137oj08cj6uii9gmih66tlh foreign key (id) references necesidad;
alter table if exists necesidad_recurrente add constraint FKl9phr1tc67j6w29ckfj5x23xk foreign key (id) references necesidad;
alter table if exists persona_donante add constraint FKsnd9jj5r5hdpgguhm6r48o161 foreign key (medio_predeterminado_id) references medio_de_contacto;
alter table if exists persona_humana add constraint FKla1ee73tk6g3h0tcltmwq233i foreign key (id) references persona_donante;
alter table if exists persona_juridica add constraint FKthxnu8b5tqp8ycvtc9tfbju3 foreign key (id) references persona_donante;
alter table if exists ranking_coincidencia add constraint FK1hx80kbbjjbxidcbukid74k10 foreign key (id_donacion) references ranking_precalculado;
alter table if exists ranking_semantico add constraint FK9pfbmkn714ygppvj1sb8hlbdm foreign key (id_donacion) references ranking_precalculado;
alter table if exists ranking_subatendidos add constraint FKgbwpjdw5hfgl3l6ptri6pgtun foreign key (id_donacion) references ranking_precalculado;
alter table if exists representante add constraint FK6kjd29slh9xve63d830acevor foreign key (entidad_id) references entidad_beneficiaria;
alter table if exists representante add constraint FKtgoahk57sy6w4cpiteco37uyq foreign key (persona_juridica_id) references persona_juridica;
