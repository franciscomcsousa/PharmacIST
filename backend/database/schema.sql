drop table if exists users;
    create table users (
        user_id integer AUTO_INCREMENT primary key not null,
        username varchar(255) not null,
        password varchar(255) not null
    );

-- TODO: Populate the database
drop table if exists pharmacies;
    create table pharmacies (
        pharmacy_id integer AUTO_INCREMENT primary key not null,
        name varchar(255) not null,
        address varchar(255) not null,
        latitude double not null,
        longitude double not null
    );


-- TODO maybe (?) - Path: database/data.sql
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Marquês', 'Rua do Marquês de Pombal 1, 1250-162 Lisboa', 38.728467, -9.148590);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Bandeira', 'Rua Sá da Bandeira 1, 1250-162 Lisboa', 38.724075, -9.150967);
insert into pharmacies (name, address, latitude, longitude) values ('Farmácia Avenida', 'Avenida da Liberdade 1, 1250-162 Lisboa', 38.725360, -9.148243);