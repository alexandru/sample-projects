CREATE TABLE sample.users
(
    id bigint not null generated always as identity primary key,
    email varchar(255) not null,
    password varchar(255) default null,
    timezone varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

CREATE TABLE sample.stuff
(
    id bigint not null generated always as identity primary key,
    user_id bigint not null,
    json_data jsonb not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    foreign key (user_id) references sample.users(id)
        on delete cascade
        on update cascade
);

GRANT
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    TRUNCATE
ON ALL TABLES IN SCHEMA sample
TO "${dbUsername}";
