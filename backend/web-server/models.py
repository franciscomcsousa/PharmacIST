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

# ========== User Registration and Verification ========== #

def create_user(username,password):
    # TODO encrypt passwords !
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


# ==================== Pharmacy ==================== #

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
        # Get the last inserted ID
        cur.execute('SELECT LAST_INSERT_ID()')
        pharmacy_id = cur.fetchone()[0]
        id = str(pharmacy_id)
        
        
        save_image(image, id, "P")
        return OK_STATUS
    
    finally:
        con.close()
        
def is_pharmacy_favorite(username, pharmacyId):
    con = connect_db()
    try:
        cur = con.cursor()
        
        data = (username,)
        query = 'SELECT user_id FROM users WHERE username = %s'
        cur.execute(query, data)
        user_id = cur.fetchone()
        
        if user_id:
            data = (user_id[0], pharmacyId)
            query = 'SELECT * FROM favorite_pharmacies WHERE user_id = %s AND pharmacy_id = %s'
            cur.execute(query, data)
            favorite = cur.fetchone()
            if favorite:
                return PHARMACY_FAVORITED_STATUS
            else:
                return PHARMACY_NOT_FAVORITED_STATUS
        else:
            return USER_DOES_NOT_EXIST_STATUS
    finally:
        con.close()

def get_user_favorite_pharmacies(username):
    con = connect_db()
    try:
        cur = con.cursor()
        
        data = (username,)
        query = 'SELECT user_id FROM users WHERE username = %s'
        cur.execute(query, data)
        user_id = cur.fetchone()
        
        data = (user_id[0],)
        query = 'SELECT * FROM pharmacies WHERE pharmacy_id IN (SELECT pharmacy_id FROM favorite_pharmacies WHERE user_id = %s)'
        cur.execute(query, data)
        favorite_pharmacies = cur.fetchall()
        return favorite_pharmacies
    finally:
        con.close()

def update_favorite_pharmacies(username, pharmacyId):
    con = connect_db()
    try:
        cur = con.cursor()
        
        data = (username,)
        query = 'SELECT user_id FROM users WHERE username = %s'
        cur.execute(query, data)
        user_id = cur.fetchone()

        if user_id:
            # Check if the pharmacy is already in the favorites
            data = (user_id[0], pharmacyId)
            query = 'SELECT * FROM favorite_pharmacies WHERE user_id = %s AND pharmacy_id = %s'
            cur.execute(query, data)
            existing_favorite = cur.fetchone()

            if existing_favorite:
                # Pharmacy is already a favorite, delete it
                query = 'DELETE FROM favorite_pharmacies WHERE user_id = %s AND pharmacy_id = %s'
                cur.execute(query, data)
                con.commit()
                return PHARMACY_NOT_FAVORITED_STATUS
            else:
                # Pharmacy is not a favorite, add it
                query = 'INSERT INTO favorite_pharmacies (user_id, pharmacy_id) VALUES (%s, %s)'
                cur.execute(query, data)
                con.commit()
                return PHARMACY_FAVORITED_STATUS
        else:
            return USER_DOES_NOT_EXIST_STATUS
            
    except Exception as e:
        print("Error:", e)
        return DATABASE_ERROR_STATUS
    finally:
        cur.close()
        con.close()


def get_pharmacy_stock(pharmacy_id, substring):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (pharmacy_id, f'%{substring}%')
        query = 'SELECT name FROM (SELECT name FROM medicine WHERE medicine_id IN (SELECT medicine_id FROM medicine_stock WHERE pharmacy_id = %s)) AS subquery WHERE name LIKE %s'
        cur.execute(query, data)
        stock = cur.fetchone()
        return stock
    finally:
        con.close()
        
def get_pharmacy_stock_id(medicine_id, pharmacy_id):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (pharmacy_id, medicine_id)
        # ms is used as an alias for medicine_stock, and m is alias for medicine
        query = """
            SELECT ms.quantity, m.name 
            FROM medicine_stock ms 
            JOIN medicine m 
            ON ms.medicine_id = m.medicine_id 
            WHERE ms.pharmacy_id = %s AND ms.medicine_id = %s"""
        cur.execute(query, data)
        result = cur.fetchone()
        if result:
            return [result[0], result[1]], OK_STATUS
        return [], OK_STATUS
    finally:
        con.close()

def get_near_pharmacies(medicine_name, latitude, longitude):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (medicine_name, latitude, longitude)
        query = 'SELECT p.pharmacy_id, p.name, ms.quantity FROM pharmacies p JOIN medicine_stock ms ON p.pharmacy_id = ms.pharmacy_id JOIN medicine m ON ms.medicine_id = m.medicine_id WHERE m.name = %s ORDER BY ABS(p.latitude - %s) + ABS(p.longitude - %s) LIMIT 20;'
        cur.execute(query, data)
        pharmacies_stock = cur.fetchall()
        return pharmacies_stock
    finally:
        con.close()

def update_pharmacy_stock(medicine_stock_list, pharmacy_id):
    # medicine_stock_list -> [[medicine_id, stock], . . . ]
    con = connect_db()
    try:
        # TODO - is it be necessary to check if the pharmacy id and medicine id are correct?
        cur = con.cursor()       
        
        for entry in medicine_stock_list:
            stock = entry[1] 
            data = (pharmacy_id, entry[0], stock) 
            query = """
                    INSERT INTO medicine_stock (pharmacy_id, medicine_id, quantity)
                    VALUES (%s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                    quantity = quantity + VALUES(quantity)"""
            cur.execute(query, data)
                
        con.commit()

        return OK_STATUS
    finally:
        con.close()
            
        
# ==================== Medicine ==================== #

def get_medicine_by_id(id):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (id,)
        query = 'SELECT * FROM medicine WHERE medicine_id = %s'
        cur.execute(query, data)
        medicine = cur.fetchall()
        if medicine:
            return medicine, OK_STATUS
        return medicine, MEDICINE_DOES_NOT_EXIST_STATUS
    finally:
        con.close()

def get_medicines_with_substring(substring):
    con = connect_db()
    try:
        cur = con.cursor()
        data = (f'%{substring}%',)
        query = 'SELECT * FROM medicine WHERE name LIKE %s'
        cur.execute(query, data)
        medicines = cur.fetchall()
        return medicines
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


# ==================== Images ==================== #

def save_image(image, id, type):
    decoded_image = base64.b64decode(image)
    
    # TODO - create images dir if it doesn't exist
    path = f"images/{type}_{id}.png"

    with open(path, 'wb') as f:
        f.write(decoded_image)

def get_image(id, type):
    path = f"images/{type}_{id}.png"

    print(path)
    # TODO - make default image be loaded in the app by default
    if not os.path.exists(path=path):
        with open("images/default.png", 'rb') as f:
            image = f.read()
            return image
    else:
        with open(path, 'rb') as f:
            image = f.read()
            return image