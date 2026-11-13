-- Entrega 6, correcciones de la prueba completa.
-- La necesidad dice en qué se mide su cantidad (kg, unidades, litros...).
alter table necesidad add column unidad varchar(30);
-- La donación guarda la ruta de logística que la lleva, para enlazar el mapa.
alter table donacion add column id_ruta_logistica uuid;
