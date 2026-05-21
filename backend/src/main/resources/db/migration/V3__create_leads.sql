create table leads (
    id bigserial primary key,
    name varchar(120) not null,
    phone varchar(30) not null,
    comment varchar(1000),
    policy_accepted boolean not null,
    created_at timestamptz not null
);

create index idx_leads_created_at on leads (created_at desc);
