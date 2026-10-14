-- Golden-Oddjobs BFF — tablas de informes y notificaciones (Supabase / PostgreSQL)
-- Ejecutar UNA vez en Supabase > SQL Editor ANTES de levantar el BFF.
-- (Hibernate también las crea con ddl-auto=update, pero la Lambda A usa la API REST de Supabase
--  y necesita que existan desde el inicio.)

create table if not exists informe (
    id              bigserial primary key,
    event_id        uuid        not null unique,
    solicitante_sub varchar(255) not null,
    email           varchar(255),
    tipo_informe    varchar(255),
    estado          varchar(20) not null default 'PENDIENTE',
    resumen         varchar(2000),
    creado_en       timestamptz,
    generado_en     timestamptz
);

create table if not exists notificacion (
    id               bigserial primary key,
    event_id         uuid         not null unique,   -- idempotencia: un evento = una notificación
    destinatario_sub varchar(255) not null,
    tipo             varchar(255),
    titulo           varchar(255),
    mensaje          varchar(1000),
    leida            boolean      not null default false,
    creada_en        timestamptz
);

create index if not exists idx_informe_solicitante on informe (solicitante_sub, creado_en desc);
create index if not exists idx_notificacion_destinatario on notificacion (destinatario_sub, creada_en desc);

-- Seguridad: el acceso público (anon key) queda bloqueado. El BFF entra como rol "postgres" y la
-- Lambda A con la service_role key; ambos ignoran RLS.
alter table informe       enable row level security;
alter table notificacion  enable row level security;
