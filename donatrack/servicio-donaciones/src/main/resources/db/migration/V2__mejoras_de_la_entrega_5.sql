-- Cambios de esquema de la revisión posterior a la Entrega 5, en el orden en que
-- se hicieron. Lo que crea o agrega usa IF NOT EXISTS y lo que borra IF EXISTS:
-- el script se puede aplicar también sobre una base que ya había actualizado
-- Hibernate con ddl-auto=update. Corre igual en PostgreSQL y en H2 (tests).

-- ── Bloqueo optimista: versión en las raíces de agregado ──────────────────
alter table donacion add column if not exists version bigint default 0 not null;
alter table entidad_beneficiaria add column if not exists version bigint default 0 not null;
alter table persona_donante add column if not exists version bigint default 0 not null;

-- ── Aviso de inactividad único: fecha del último aviso ────────────────────
alter table persona_donante add column if not exists ultimo_aviso_inactividad timestamp(6);

-- ── Contacto predeterminado como marca dentro de la lista de contactos ────
-- Antes la persona apuntaba a su medio predeterminado (medio_predeterminado_id),
-- que podía no estar en su lista. Ahora el medio se marca y pertenece a la lista.
alter table medio_de_contacto add column if not exists predeterminado boolean default false not null;

update medio_de_contacto
   set predeterminado = true,
       persona_donante_id = (select p.id from persona_donante p
                              where p.medio_predeterminado_id = medio_de_contacto.id)
 where id in (select p.medio_predeterminado_id from persona_donante p
               where p.medio_predeterminado_id is not null);

-- La clave foránea y la unicidad de la columna se van con ella.
alter table persona_donante drop column if exists medio_predeterminado_id;

-- ── Subcategoría de las necesidades ───────────────────────────────────────
-- Las necesidades cargadas antes quedan sin subcategoría: el matching las sigue
-- comparando por nombre y descripción.
alter table necesidad add column if not exists subcategoria_tipo varchar(255);
alter table necesidad add column if not exists subcategoria_tipo_normalizado varchar(255);

-- ── Rankings precalculados: una tabla de puestos para todos los algoritmos ─
create table if not exists ranking_puesto (
    posicion integer not null,
    puntaje float(53),
    id_donacion uuid not null references ranking_precalculado (id_donacion),
    id_entidad uuid,
    algoritmo varchar(255) not null
);
create index if not exists idx_ranking_puesto_donacion on ranking_puesto (id_donacion);

-- La bandeja vigente no se pierde: cada tabla vieja era el ranking de un
-- algoritmo, con la misma posición desde cero. Las coincidencias eran un dato
-- derivado de los otros dos y ya no se guardan.
insert into ranking_puesto (id_donacion, algoritmo, posicion, id_entidad, puntaje)
select r.id_donacion, 'COMPATIBILIDAD_SEMANTICA', r.posicion, r.id_entidad, r.puntaje
  from ranking_semantico r
 where not exists (select 1 from ranking_puesto p
                    where p.id_donacion = r.id_donacion and p.algoritmo = 'COMPATIBILIDAD_SEMANTICA');

insert into ranking_puesto (id_donacion, algoritmo, posicion, id_entidad, puntaje)
select r.id_donacion, 'PRIORIDAD_SUBATENDIDOS', r.posicion, r.id_entidad, r.puntaje
  from ranking_subatendidos r
 where not exists (select 1 from ranking_puesto p
                    where p.id_donacion = r.id_donacion and p.algoritmo = 'PRIORIDAD_SUBATENDIDOS');

drop table if exists ranking_coincidencia;
drop table if exists ranking_semantico;
drop table if exists ranking_subatendidos;

-- ── Bandeja de salida (outbox) de avisos a otros servicios ────────────────
create table if not exists mensaje_saliente (
    intentos integer not null,
    creado timestamp(6) not null,
    enviado timestamp(6),
    proximo_intento timestamp(6) not null,
    id uuid not null,
    estado varchar(20) not null check (estado in ('PENDIENTE','ENVIADO','FALLIDO')),
    destino varchar(60) not null,
    ultimo_error varchar(1000),
    payload varchar(10000) not null,
    primary key (id)
);
create index if not exists idx_mensaje_saliente_listos on mensaje_saliente (estado, proximo_intento);

-- ── Proveedor de logística como enumerado ─────────────────────────────────
-- Hibernate lee el nombre exacto del enumerado: se normaliza antes de exigirlo.
update donacion set proveedor_logistica = upper(trim(proveedor_logistica))
 where proveedor_logistica is not null;
alter table donacion add constraint ck_donacion_proveedor_logistica
    check (proveedor_logistica in ('DONATRACK','EXTERNA'));

-- ── Baja lógica de donaciones ─────────────────────────────────────────────
alter table donacion add column if not exists fecha_baja timestamp(6);
