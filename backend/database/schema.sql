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

insert into pharmacies (name, address, latitude, longitude) values 
('Farmácia Rossio', 'Rua Augusta 1, 1250-162 Lisboa', 38.712790, -9.137420),
('Farmácia Marquês', 'Rua do Marquês de Pombal 1, 1251-162 Lisboa', 38.728467, -9.148590),
('Farmácia Bandeira', 'Rua Sá da Bandeira 1, 1252-162 Lisboa', 38.724075, -9.150967),
('Farmácia Avenida', 'Avenida da Liberdade 1, 1253-162 Lisboa', 38.725360, -9.148243),
('Farmácia Oriente', 'Rua do Oriente 1, 1990-096 Lisboa', 38.768256, -9.098214);

-- Insert 25 unique medicines into the medicine table with explicit IDs
insert into medicine (medicine_id, name, purpose) values 
(1, 'Paracetamol', 'Painkiller'),
(2, 'Ibuprofen', 'Anti-inflammatory'),
(3, 'Zyrtec', 'Antihistamine'),
(4, 'Amoxicillin', 'Antibiotic'),
(5, 'Ciprofloxacin', 'Antibiotic'),
(6, 'Metformin', 'Diabetes'),
(7, 'Amlodipine', 'Hypertension'),
(8, 'Simvastatin', 'Cholesterol'),
(9, 'Omeprazole', 'Acid reflux'),
(10, 'Losartan', 'Hypertension'),
(11, 'Azithromycin', 'Antibiotic'),
(12, 'Doxycycline', 'Antibiotic'),
(13, 'Lisinopril', 'Hypertension'),
(14, 'Levothyroxine', 'Hypothyroidism'),
(15, 'Atorvastatin', 'Cholesterol'),
(16, 'Albuterol', 'Asthma'),
(17, 'Prednisone', 'Anti-inflammatory'),
(18, 'Warfarin', 'Blood thinner'),
(19, 'Gabapentin', 'Nerve pain'),
(20, 'Hydrochlorothiazide', 'Diuretic'),
(21, 'Tramadol', 'Pain relief'),
(22, 'Montelukast', 'Asthma'),
(23, 'Clindamycin', 'Antibiotic'),
(24, 'Hydrocodone', 'Pain relief'),
(25, 'Citalopram', 'Antidepressant');

-- Assigning medicines to each pharmacy with at least 5 different from each other
insert into medicine_stock (pharmacy_id, medicine_id, quantity) values 
(1, 1, 50), (1, 2, 50), (1, 3, 50), (1, 4, 50), (1, 5, 50), 
(1, 6, 50), (1, 7, 50), (1, 8, 50), (1, 9, 50), (1, 10, 50), 
(1, 11, 50), (1, 12, 50), (1, 13, 50), (1, 14, 50), (1, 15, 50);

insert into medicine_stock (pharmacy_id, medicine_id, quantity) values 
(2, 6, 50), (2, 7, 50), (2, 8, 50), (2, 9, 50), (2, 10, 50), 
(2, 11, 50), (2, 12, 50), (2, 13, 50), (2, 14, 50), (2, 15, 50), 
(2, 16, 50), (2, 17, 50), (2, 18, 50), (2, 19, 50), (2, 20, 50);

insert into medicine_stock (pharmacy_id, medicine_id, quantity) values 
(3, 1, 50), (3, 2, 50), (3, 3, 50), (3, 4, 50), (3, 5, 50), 
(3, 16, 50), (3, 17, 50), (3, 18, 50), (3, 19, 50), (3, 20, 50), 
(3, 21, 50), (3, 22, 50), (3, 23, 50), (3, 24, 50), (3, 25, 50);

insert into medicine_stock (pharmacy_id, medicine_id, quantity) values 
(4, 1, 50), (4, 2, 50), (4, 6, 50), (4, 7, 50), (4, 8, 50), 
(4, 9, 50), (4, 10, 50), (4, 11, 50), (4, 12, 50), (4, 13, 50), 
(4, 14, 50), (4, 15, 50), (4, 21, 50), (4, 22, 50), (4, 23, 50);

insert into medicine_stock (pharmacy_id, medicine_id, quantity) values 
(5, 3, 50), (5, 4, 50), (5, 5, 50), (5, 16, 50), (5, 17, 50), 
(5, 18, 50), (5, 19, 50), (5, 20, 50), (5, 21, 50), (5, 22, 50), 
(5, 23, 50), (5, 24, 50), (5, 25, 50), (5, 1, 50), (5, 2, 50);