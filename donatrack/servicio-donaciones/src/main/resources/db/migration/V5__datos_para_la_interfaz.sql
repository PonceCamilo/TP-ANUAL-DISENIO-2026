-- Entrega 6: datos que necesita la interfaz de usuario (landing, mapa y fotos).
-- Corre igual en PostgreSQL y en H2 (MigracionesTest la aplica sobre H2).

-- Coordenadas de la dirección, para el mapa de donaciones entregadas
-- (requisito 2). Son opcionales: las direcciones cargadas antes no las tienen.
-- La dirección es un objeto embebido de entidades y donantes, por eso las
-- columnas van en las dos tablas.
alter table entidad_beneficiaria add column if not exists direccion_latitud float(53);
alter table entidad_beneficiaria add column if not exists direccion_longitud float(53);
alter table persona_donante add column if not exists direccion_latitud float(53);
alter table persona_donante add column if not exists direccion_longitud float(53);

-- Fecha de entrega, desnormalizada desde el historial: la landing muestra las
-- donaciones entregadas en el último mes (requisito 1) y buscarlas por el
-- historial obligaría a unir cambio_estado en cada consulta. Se completa para
-- las que ya estaban entregadas con la fecha de ese cambio de estado.
alter table donacion add column if not exists fecha_entrega timestamp(6);
update donacion
   set fecha_entrega = (select max(c.fecha_hora) from cambio_estado c
                         where c.donacion_id = donacion.id and c.estado = 'ENTREGADA')
 where estado = 'ENTREGADA' and fecha_entrega is null;
create index if not exists idx_donacion_entrega on donacion (fecha_entrega desc);

-- Las fotos de los bienes son URLs: 255 caracteres no alcanzan para una URL larga.
alter table bien alter column foto set data type varchar(2048);

-- Fotos que la entidad cargó al confirmar la recepción (requisito 12). Las
-- informa logística en el aviso de entrega exitosa; la landing las muestra.
create table if not exists donacion_foto_entrega (
    donacion_id uuid not null references donacion (id),
    posicion integer not null,
    url varchar(2048) not null,
    primary key (donacion_id, posicion)
);
