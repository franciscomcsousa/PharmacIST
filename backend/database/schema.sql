create database if not exists pharmacist;
use pharmacist;

drop table if exists favorite_pharmacies;
drop table if exists medicine_notification;
drop table if exists user_tokens;
drop table if exists users;
drop table if exists medicine_stock;
drop table if exists pharmacies;
drop table if exists medicine;

    create table users (
        user_id integer AUTO_INCREMENT primary key not null,
        username varchar(255) not null,
        password varchar(255) not null
    );

    create table user_tokens (
        user_id integer,
        device_id varchar(255), -- Unique identifier for the device
        -- maybe add the login_token (?)
        notif_token varchar(255), -- one for device
        primary key (user_id, device_id),
        foreign key (user_id) references users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
    );

    create table pharmacies (
        pharmacy_id integer AUTO_INCREMENT primary key not null,
        name varchar(255) not null,
        address varchar(255) not null,
        latitude double not null,
        longitude double not null
    );

    create table favorite_pharmacies (
        favorite_id integer AUTO_INCREMENT primary key not null,
        user_id integer not null,
        pharmacy_id integer not null,
        foreign key (user_id) references users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
        foreign key (pharmacy_id) references pharmacies(pharmacy_id) ON DELETE CASCADE ON UPDATE CASCADE
    );

    create table medicine (
        medicine_id integer primary key not null,
        name varchar(255) not null,
        purpose varchar(255) not null
    );

    create table medicine_stock (
        medicine_stock_id integer AUTO_INCREMENT primary key not null,
        pharmacy_id integer not null,
        medicine_id integer not null,
        quantity integer not null,
        foreign key (pharmacy_id) references pharmacies(pharmacy_id) ON DELETE CASCADE ON UPDATE CASCADE,
        foreign key (medicine_id) references medicine(medicine_id) ON DELETE CASCADE ON UPDATE CASCADE,
        UNIQUE KEY (pharmacy_id, medicine_id)
    );

    create table medicine_notification (
        medicine_notification_id integer AUTO_INCREMENT primary key not null,
        user_id integer not null,
        medicine_id integer not null,
        foreign key (user_id) references users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
        foreign key (medicine_id) references medicine(medicine_id) ON DELETE CASCADE ON UPDATE CASCADE
    );

insert into users (username, password) values ('a', 'a');

insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Rossio', 'Rua Augusta 1, 1250-162 Lisboa', 38.712790, -9.137420);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Marquês', 'Rua do Marquês de Pombal 1, 1251-162 Lisboa', 38.728467, -9.148590);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Bandeira', 'Rua Sá da Bandeira 1, 1252-162 Lisboa', 38.724075, -9.150967);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Avenida', 'Avenida da Liberdade 1, 1253-162 Lisboa', 38.725360, -9.148243);

insert into favorite_pharmacies (user_id, pharmacy_id) values (1, 1);

insert into medicine (medicine_id, name, purpose) values (1, 'Paracetamol', 'Painkiller');
insert into medicine (medicine_id, name, purpose) values (2, 'Ibuprofen', 'Anti-inflammatory');
insert into medicine (medicine_id, name, purpose) values (3, 'Zyrtec', 'Antihistamine');

insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (4, 1, 50);
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (4, 2, 30);
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (3, 3, 25);
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (2, 2, 40);