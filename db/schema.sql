create table if not exists informe (
    id              bigserial primary key,
    event_id        uuid        not null unique,
    solicitante_sub varchar(255) not null,
    solicitante_nombre varchar(255),
    email           varchar(255),
    tipo_informe    varchar(255),
    estado          varchar(20) not null default 'PENDIENTE',
    resumen         varchar(2000),
    creado_en       timestamptz,
    generado_en     timestamptz
);

-- Si la tabla informe ya existía de antes, agrega la columna nueva
alter table informe add column if not exists solicitante_nombre varchar(255);

-- Lambda 1 escribe aquí quién pidió cada informe y cuándo
create table if not exists registro_informe (
    id              bigserial primary key,
    event_id        uuid         not null unique,
    usuario_nombre  varchar(255) not null,
    usuario_email   varchar(255),
    fecha_solicitud timestamptz  not null,
    registrado_en   timestamptz  not null default now()
);

create table if not exists notificacion (
    id               bigserial primary key,
    event_id         uuid         not null unique,  
    destinatario_sub varchar(255) not null,
    tipo             varchar(255),
    titulo           varchar(255),
    mensaje          varchar(1000),
    leida            boolean      not null default false,
    creada_en        timestamptz
);

create index if not exists idx_informe_solicitante on informe (solicitante_sub, creado_en desc);
create index if not exists idx_notificacion_destinatario on notificacion (destinatario_sub, creada_en desc);


alter table informe       enable row level security;
alter table notificacion  enable row level security;
alter table registro_informe enable row level security;
