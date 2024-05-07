from flask import Flask, render_template, make_response, request, jsonify
from datetime import datetime, timedelta
from flask import json
from models import *
import jwt

app = Flask(__name__)
# used to encrypt the Jtokens, for simplicity is now here
app.config['SECRET_KEY'] = 'manteiga de amendoim'

@app.route('/')
def home():
    return render_template('home.html') 


# TODO 
# add salt to passwords + encryption ?
# verify here and on android if parameters are not null
@app.route('/register', methods=['GET', 'POST'])
def register_user():
    if request.method == 'GET':
        return render_template('register.html') 
        
    # verify if all data is correct
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        status = create_user(username, password)
        
        # if register successful send token and user stays logged in!
        # maybe also use login_user(), but for simplicity tokens do the job
        token_encode = jwt.encode({'username': username, 'exp': datetime.utcnow() 
                            + timedelta(days=1)}, app.config['SECRET_KEY'])
        token = token_encode.decode('UTF-8')
        
        return make_response(jsonify({'token': token}), status)
    
    return make_response({"status":400}, 400)


# TODO 
# verify here and on android if parameters are not null
@app.route('/login', methods=['GET', 'POST'])
def login_user():
    if request.method == 'GET':
        pass
        
    # verify if all data is correct
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        status = verify_user(username, password)
        
        # if register successful send token and user stays logged in!
        # maybe also use login_user(), but for simplicity tokens do the job
        token_encode = jwt.encode({'username': username, 'exp': datetime.utcnow() 
                            + timedelta(days=1)}, app.config['SECRET_KEY'])
        token = token_encode.decode('UTF-8')
        
        return make_response(jsonify({'token': token}), status)
    
    return make_response({"status":400}, 400)


if __name__ == '__main__':
    app.run(host='127.0.0.1', debug=True)
