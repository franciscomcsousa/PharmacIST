import os
import mariadb
import base64
from response_status import *

# for now use local admin user
def connect_db():
    return mariadb.connect(
                host = "localhost",
                user = "admin",
                password = "crepes",
                port = 3306,
                database = "pharmacist"
                )


# TODO encrypt passwords !
def create_user(username,password):
    con = connect_db()
    cur = con.cursor()
    # Verify if the user already exists
    data = (username,)
    query = 'select * from users where username = %s'
    cur.execute(query, data)
    user = cur.fetchall()
    try:
        if len(user) > 0:
            cur.close()
            return USER_ALREADY_EXISTS_STATUS
        
        # Create the user
        data = (username, password)
        query = 'insert into users (username, password) values (%s, %s)'
        cur.execute(query, data)
        con.commit()
        
    finally: 
        con.close()
    return OK_STATUS

def verify_user(username, password):
    con = connect_db()
    try:
        cur = con.cursor()
        # Verify if the user already exists
        data = (username, password)
        query = 'SELECT * FROM users WHERE username = %s AND password = %s'
        cur.execute(query, data)
        user = cur.fetchone()
        if user:
            return OK_STATUS
        
    finally:
        con.close()
    return USER_DOES_NOT_EXIST_STATUS

def login_guest(username, password):
    con = connect_db()
    try:
        cur = con.cursor()
        # Verify if the user already exists
        data = (username, password)
        query = 'SELECT * FROM users WHERE username = %s AND password = %s'
        cur.execute(query, data)
        user = cur.fetchone()
        if user:
            return OK_STATUS
        # if it doesn't exist, create new user
        data = (username, password)
        query = 'insert into users (username, password) values (%s, %s)'
        cur.execute(query, data)
        con.commit()
        
    finally:
        con.close()
    return OK_STATUS


def get_closest_pharmacies(latitude, longitude):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (latitude, longitude)
        query = 'SELECT * FROM pharmacies ORDER BY ABS((latitude - %s)) + ABS((longitude - %s)) ASC LIMIT 3'
        cur.execute(query, data)
        pharmacies = cur.fetchall()
        return pharmacies
    finally:
        con.close()

def serialize_pharmacy(name, address, latitude, longitude, image):
    con = connect_db()
    try:
        # TODO - check if pharmacy is repeated (and perhaps other safety stuff)
        cur = con.cursor()
        data = (name, address, latitude, longitude)
        query = 'INSERT INTO pharmacies (name, address, latitude, longitude) VALUES (%s, %s, %s, %s)'
        cur.execute(query, data)
        con.commit()

        if image == "":
            return OK_STATUS

        # Save image of pharmacy in base64 encode of its name
        name_b64 = base64.b64encode(name.encode('utf-8'))
        save_image(image, name=name_b64.decode())
        return OK_STATUS
    
    finally:
        con.close()

def verify_medicine(name):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (name,)
        query = 'SELECT * FROM medicine WHERE name = %s'
        cur.execute(query, data)
        medicine = cur.fetchone()
        if medicine:
            return OK_STATUS
    finally:
        con.close()
    return MEDICINE_DOES_NOT_EXIST_STATUS

def get_requested_medicine(name):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (name,)
        query = 'SELECT * FROM medicine WHERE name = %s'
        cur.execute(query, data)
        medicine = cur.fetchone()
        return medicine
    finally:
        con.close()

def get_closest_pharmacy_with_medicine(medicine_name, latitude, longitude):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (medicine_name, latitude, longitude)
        query = 'SELECT * FROM pharmacies WHERE pharmacy_id IN (SELECT pharmacy_id FROM medicine_stock WHERE medicine_id IN (SELECT medicine_id FROM medicine WHERE name = %s) AND quantity > 0) ORDER BY ABS((latitude - %s)) + ABS((longitude - %s)) ASC LIMIT 1'
        cur.execute(query, data)
        pharmacy = cur.fetchone()
        return pharmacy
    finally:
        con.close()


def save_image(image, name):
    decoded_image = base64.b64decode(image)
    # TODO - create images dir if it doesn't exist
    path = f"images/{name}.png"

    with open(path, 'wb') as f:
        f.write(decoded_image)

def get_image(name):
    path = f"images/{name}.png"


    # TODO - make default image be loaded in the app by default
    if not os.path.exists(path=path):
        with open("images/default.png", 'rb') as f:
            image = f.read()
            return image
    else:
        with open(path, 'rb') as f:
            image = f.read()
            return image