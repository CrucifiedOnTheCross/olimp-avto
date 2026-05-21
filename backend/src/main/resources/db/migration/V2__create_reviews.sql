create table reviews (
    id bigserial primary key,
    rating integer not null check (rating between 1 and 5),
    review_text varchar(2000) not null,
    customer_name varchar(120) not null,
    car_model varchar(120) not null,
    country varchar(40) not null,
    status varchar(20) not null,
    moderation_token varchar(80) not null unique,
    created_at timestamp with time zone not null default now(),
    moderated_at timestamp with time zone
);

create index idx_reviews_status_created_at on reviews (status, created_at desc);
