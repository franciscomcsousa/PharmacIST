import mariadb
from response_status import *

# for now use local admin user
con = mariadb.connect(
            host = "localhost",
            user = "admin",
            password = "crepes",
            port = 3306,
            database = "pharmacist"
            )

print("Connected to database")

# encrypt passwords !
def create_user(username,password):
    cur = con.cursor()
    # Verify if the user already exists
    data = (username,)
    query = 'select * from users where username = %s'
    cur.execute(query, data)
    user = cur.fetchall()
    if len(user) > 0:
        cur.close()
        return USER_ALREADY_EXISTS_STATUS
    
    # Create the user
    data = (username, password)
    query = 'insert into users (username, password) values (%s, %s)'
    cur.execute(query, data)
    con.commit()
    return OK_STATUS