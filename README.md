# Database - MariaDB

## Install

(WSL2)
``` bash
sudo apt install mariadb-server
sudo /etc/init.d/mysql start
sudo /etc/init.d/mysql status
sudo mysql_secure_installation
```
If it does not work, because WSL is bad:
```bash
sudo apt remove --purge *mysql*
sudo apt remove --purge *mariadb*
sudo rm -rf /etc/mysql /var/lib/mysql
sudo apt autoremove
sudo apt autoclean
```
and try again.

### Create admin user
Open MariaDB ```sudo mariadb ```

Create a new user with root privileges and password-based access. For simplicity and test purposes, we will have a user named admin with a password crepes.

```sql
GRANT ALL ON *.* TO 'admin'@'localhost' IDENTIFIED BY 'crepes' WITH GRANT OPTION;
```

Update and exit:
```
FLUSH PRIVILEGES;
exit
```
Tutorial [**Here.**](https://www.digitalocean.com/community/tutorials/how-to-install-mariadb-on-ubuntu-20-04)

## Importing databases

Login as an admin user and create a new database if necessary. 
```
mysql -u 'admin_username' -p
CREATE DATABASE pharmacist;
```

The correct way to import the schema to the new_database is with:
```
mysql -u username -p pharmacist < schema.sql
```
However, to simplify, there will be a script in the database folder with the alternative code which is using **sudo mariadb**.
``` bash
bash insert_schemas.sql
```

Tutorial [**Here.**](https://www.digitalocean.com/community/tutorials/how-to-import-and-export-databases-in-mysql-or-mariadb)

# Backend - Python flask

## Install requirements

(WSL) may need to install some missing libraries
```
sudo apt-get install libmariadb3 libmariadb-dev
```
Run requirements script: ```pip3 install -r requirements.txt```