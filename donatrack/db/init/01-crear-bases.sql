-- Crea una base por servicio dentro de un único servidor PostgreSQL.
--
-- POR QUÉ ESTE ARCHIVO: la imagen oficial de postgres solo crea la base que se
-- indica en POSTGRES_DB. Para tener varias hay que dejar un script en
-- /docker-entrypoint-initdb.d/, que la imagen ejecuta UNA SOLA VEZ, cuando el
-- volumen de datos está vacío. Si la base ya existe, este script no corre.
--
-- DECISIÓN DE ARQUITECTURA: se mantiene el aislamiento a nivel de BASE DE DATOS
-- (ningún servicio lee las tablas de otro) pero se comparte el SERVIDOR, en
-- lugar de levantar un contenedor de PostgreSQL por servicio. El motivo es el
-- costo operativo: un contenedor de PostgreSQL por cada servicio relacional
-- multiplica la memoria y los healthchecks, sin ganar nada que se pueda
-- demostrar en la defensa. Logística no usa este servidor: tiene MongoDB.
--
-- Lo que se pierde respecto de un servidor por servicio: los servicios comparten
-- recursos (CPU, memoria, conexiones) y un reinicio del servidor los afecta a
-- todos a la vez. En producción real cada uno tendría su propia instancia.

-- Logística no está acá: usa MongoDB (base logistica).
CREATE DATABASE donaciones_db;
CREATE DATABASE incentivos_db;
CREATE DATABASE notificaciones_db;

-- El usuario donatrack (creado por POSTGRES_USER) queda dueño de las tres.
-- Si más adelante se quiere que el aislamiento lo imponga el motor y no sólo la
-- convención, acá se crearía un usuario por servicio con permisos únicamente
-- sobre su propia base.
