create table cars (
    id bigserial primary key,
    title varchar(160) not null,
    country varchar(80) not null,
    price bigint not null check (price > 0),
    year integer not null check (year between 1950 and 2100),
    engine varchar(120),
    description varchar(2000) not null,
    image_url varchar(500)
);

insert into cars (title, country, price, year, engine, description, image_url)
values
    ('Nissan Juke', 'Япония', 1050000, 2015, '1.5 л, бензин, 114 л.с.', 'Компактный городской кроссовер из Японии. Экономичный двигатель, удобная посадка и хороший вариант для города.', 'images/car-1.jpg'),
    ('Suzuki Solio', 'Япония', 1250000, 2022, '1.2 л, бензин, мягкий гибрид', 'Практичный компактный минивэн с удобным салоном, низким расходом и отличной обзорностью.', 'images/car-2.jpg'),
    ('Geely Monjaro', 'Китай', 3450000, 2023, '2.0 л, бензин, автомат', 'Современный кроссовер с богатой комплектацией, мощным турбомотором и комфортным салоном.', 'images/car-3.jpg');
