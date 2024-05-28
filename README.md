# PharmacIST

PharmacIST, a simple yet powerful pharmacy management app.

## Table of Contents

- [Used Technologies](#used-technologies)
- [Application](#application---android-studio)
- [Database](#database---mariadb)
	- [Install](#install)
	- [Create admin user](#create-admin-user)
	- [Importing databases](#importing-databases)
- [Web Server](#web-server---python-flask)
	- [Install requirements](#install-requirements)
- [Scripts](#scripts)
	- [Nginx](#nginx)
	- [Deployment Script](#deployment-script)
	- [GitHub Continuous Deployment (CD)](#github-continuous-deployment-cd)
- [Authors](#authors)




## Used Technologies

- [Flask](https://flask.palletsprojects.com/en/3.0.x/)
- [MariaDB](https://mariadb.org/)
- [Android Studio](https://developer.android.com/studio)

## Application - Android Studio

To run the application, simply build the project with ````gradle build``

For the application to work as intended, for now, its required that the [web server](#web-server---python-flask) is running.

## Database - MariaDB

### Install

(Debian)
``` bash
sudo apt install mariadb-server
sudo systemctl start mariadb.service
sudo systemctl status mariadb.service
sudo mysql_secure_installation
```
If it does not work:
``` bash
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

``` sql
GRANT ALL ON *.* TO 'admin'@'localhost' IDENTIFIED BY 'crepes' WITH GRANT OPTION;
```

Update and exit:
``` sql
FLUSH PRIVILEGES;
exit
```
Tutorial [**Here.**](https://www.digitalocean.com/community/tutorials/how-to-install-mariadb-on-ubuntu-20-04)

### Importing databases

Login as an admin user and create a new database if necessary. 
``` bash
mysql -u 'admin_username' -p
CREATE DATABASE pharmacist;
```

The correct way to import the schema to the new_database is with:
``` bash
mysql -u username -p pharmacist < schema.sql
```
However, to simplify, there will be a script in the database folder with the alternative code which is using **sudo mariadb**.
``` bash
bash insert_schemas.sql
```

Tutorial [**Here.**](https://www.digitalocean.com/community/tutorials/how-to-import-and-export-databases-in-mysql-or-mariadb)

## Web Server - Python flask

### Install requirements

(Debian) may need to install some missing libraries
```
sudo apt-get install libmariadb3 libmariadb-dev
```
Run requirements script: ```pip3 install -r requirements.txt```

**To run the webserver:** ```python3 app.py```

## Scripts

### Nginx
``` nginx
server {
    listen 80 default_server;
    server_name pharmacist.francisco-sousa.pt;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name pharmacist.francisco-sousa.pt;

    ssl_certificate /etc/letsencrypt/live/pharmacist.francisco-sousa.pt/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pharmacist.francisco-sousa.pt/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Deployment Script

```bash
#!/bin/bash

APP_DIR="/root/PharmacIST"
SUPERVISORCTL="/usr/bin/supervisorctl"

cd $APP_DIR

git pull origin main

# Reload supervisor
$SUPERVISORCTL reread
$SUPERVISORCTL update
$SUPERVISORCTL restart all

echo "$(date) - Deployed and reloaded supervisor" >> /var/log/deploy.log
```

### GitHub Continuous Deployment (CD)

```yaml
name: Deploy to Linode

on:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v2

    - name: Set up SSH
      uses: webfactory/ssh-agent@v0.5.3
      with:
        ssh-private-key: ${{ secrets.SSH_CI_CD_PRIVATE_KEY }}

    - name: Deploy to Linode
      run: |
        ssh -o StrictHostKeyChecking=no root@172.232.42.26 'bash /root/update-server.sh'
      env:
        SSH_PRIVATE_KEY: ${{ secrets.SSH_CI_CD_PRIVATE_KEY }}
```

### Authors
- [Francisco Sousa](https://github.com/franciscomcsousa)
- [Miguel Porfírio](https://github.com/miguelporfirio19)
- [Sara Aguincha](https://github.com/SaraAguincha)