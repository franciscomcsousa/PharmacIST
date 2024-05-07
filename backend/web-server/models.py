import mariadb
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

#print("Connected to database")

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

def get_closest_pharmacies(latitude, longitude):
    con = connect_db()
    cur = con.cursor()
    data = (latitude, longitude)
    query = 'select * from pharmacies order by (latitude - %s) + (longitude - %s) limit 3'
    cur.execute(query, data)
    pharmacies = cur.fetchall()
    return pharmacies