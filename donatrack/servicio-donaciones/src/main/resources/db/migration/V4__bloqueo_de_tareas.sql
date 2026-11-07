-- Bloqueo de tareas programadas (ShedLock): una fila por tarea con hasta cuándo
-- la tiene tomada una instancia. La que no consigue la fila no corre la tarea.
create table if not exists shedlock (
    name varchar(64) not null,
    lock_until timestamp(3) not null,
    locked_at timestamp(3) not null,
    locked_by varchar(255) not null,
    primary key (name)
);
