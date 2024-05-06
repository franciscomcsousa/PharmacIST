from flask import Flask, render_template, make_response, request
from flask import json
from models import *

app = Flask(__name__)

@app.route('/')
def home():
    return render_template('home.html') 


# add salt to passwords !
@app.route('/register', methods=['GET', 'POST'])
def register_user():
    if request.method == 'GET':
        return render_template('register.html') 
        
    
    if request.method == 'POST':
        username = request.form.get("username")
        password = request.form.get("password")
        status = create_user(username, password)
        return make_response({"status":status}, status)
    
    return make_response({"status":400}, 400)


if __name__ == '__main__':
    app.run(host='127.0.0.1', debug=True)
