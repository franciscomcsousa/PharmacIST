drop table if exists users;
drop table if exists medicine_stock;
drop table if exists pharmacies;
drop table if exists medicine;

    create table users (
        user_id integer AUTO_INCREMENT primary key not null,
        username varchar(255) not null,
        password varchar(255) not null
    );

    create table pharmacies (
        pharmacy_id integer AUTO_INCREMENT primary key not null,
        name varchar(255) not null,
        address varchar(255) not null,
        latitude double not null,
        longitude double not null
    );

    create table medicine (
        medicine_id integer AUTO_INCREMENT primary key not null,
        name varchar(255) not null,
        purpose varchar(255) not null
    );

    create table medicine_stock (
        medicine_stock_id integer AUTO_INCREMENT primary key not null,
        pharmacy_id integer not null,
        medicine_id integer not null,
        quantity integer not null,
        foreign key (pharmacy_id) references pharmacies(pharmacy_id) ON DELETE CASCADE ON UPDATE CASCADE,
        foreign key (medicine_id) references medicine(medicine_id) ON DELETE CASCADE ON UPDATE CASCADE
    );


insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Rossio', 'Rua Augusta 1, 1250-162 Lisboa', 38.712790, -9.137420);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Marquês', 'Rua do Marquês de Pombal 1, 1251-162 Lisboa', 38.728467, -9.148590);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Bandeira', 'Rua Sá da Bandeira 1, 1252-162 Lisboa', 38.724075, -9.150967);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Avenida', 'Avenida da Liberdade 1, 1253-162 Lisboa', 38.725360, -9.148243);

insert into medicine (name, purpose) values ('Paracetamol', 'Painkiller');
insert into medicine (name, purpose) values ('Ibuprofen', 'Anti-inflammatory');
insert into medicine (name, purpose) values ('Zyrtec', 'Antihistamine');

insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (4, 1, 50);
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (4, 2, 30);
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (3, 3, 25);
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values (2, 2, 40);