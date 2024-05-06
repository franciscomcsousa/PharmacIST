drop table if exists users;
    create table users (
        user_id integer AUTO_INCREMENT primary key not null,
        username varchar(255) not null,
        password varchar(255) not null
    );

-- TODO: Populate the database 