from flask import Flask, render_template, make_response, request, jsonify
from datetime import datetime, timedelta
from flask import json
from functools import wraps
from models import *
import jwt

# Decorator to require login
# Token verification in headers
def login_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('Authorization')
        
        print(token)

        if not token:
            return jsonify({'error': 'Token is missing'}), TOKEN_IS_MISSING

        try:
            data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
            # TODO - maybe in the future also add user
        except jwt.ExpiredSignatureError:
            return jsonify({'error': 'Token has expired'}), TOKEN_AS_EXPIRED
        except jwt.InvalidTokenError:
            return jsonify({'error': 'Invalid token'}), INVALID_TOKEN

        return f(*args, **kwargs)

    return decorated

app = Flask(__name__)
# used to encrypt the Jtokens, for simplicity is now here
app.config['SECRET_KEY'] = 'manteiga de amendoim'


@app.route('/')
def home():
    return render_template('home.html') 


# TODO 
# add salt to passwords + encryption ?
# verify here if parameters are not null
@app.route('/register', methods=['GET', 'POST'])
def register_user():
    if request.method == 'GET':
        return render_template('register.html') 
        
    # TODO - verify if all data is correct
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        status = create_user(username, password)
        
        # if register successful send token and user stays logged in!
        # maybe also use login_user(), but for simplicity tokens do the job
        
        #token_encode = jwt.encode({'username': username, 'exp': datetime.utcnow() 
        #                    + timedelta(days=1)}, app.config['SECRET_KEY'])
        
        token = jwt.encode({'exp': datetime.utcnow() + timedelta(days=1)}, app.config['SECRET_KEY'])
                
        return make_response(jsonify({'token': token}), status)
    
    return make_response({"status":400}, 400)


# TODO 
# verify here if parameters are not null
@app.route('/login', methods=['GET', 'POST'])
def login_user():
    if request.method == 'GET':
        pass
        
    # TODO - verify if all data is correct
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        # in case of the guests logins: 
        # if they dont exist, create a new one
        # if they exist, simply login
        if username.startswith("guest_"):
            status = login_guest(username, password)
        
        else:
            status = verify_user(username, password)
        
        # TODO - is sendong token even if unsucessful
        # if register successful send token and user stays logged in!
        # maybe also use login_user(), but for simplicity tokens do the job
        token = jwt.encode({'exp': datetime.utcnow() + timedelta(days=1)}, app.config['SECRET_KEY'])
        
        return make_response(jsonify({'token': token}), status)
    
    return make_response({"status":400}, 400)

@app.route('/authorized',  methods=['GET'])
@login_required
def auto_login():
    return make_response(jsonify({'status': 200}), 200)


@app.route('/pharmacies', methods=['GET', 'POST'])
def get_pharmacies():
    if request.method == 'GET':
        pass

    # Get a list with the 3 closest pharmacies to the user location
    if request.method == 'POST':
        data = request.get_json()
        latitude = data['latitude']
        longitude = data['longitude']
        pharmacies = get_closest_pharmacies(latitude, longitude)
        print(pharmacies)

        return make_response(jsonify({"pharmacies": pharmacies}), 200)
    
    return make_response({"status": 400}, 400)

@app.route('/create_pharmacy', methods=['GET', 'POST'])
def create_pharmacy():
    if request.method == 'GET':
        pass

    if request.method == 'POST':
        data = request.get_json()
        name = data['name']
        address = data['address']
        latitude = data['latitude']
        longitude = data['longitude']
        image = data['image']
        serialize_pharmacy(name, address, latitude, longitude, image)

        return make_response({"status": 200}, 200)
    
    return make_response({"status": 400}, 400)


# TODO - Extra feature (i swear this was made on purpose and its not a bad interpretation of requirements turned into a feature, help :C)
#@login_required
@app.route('/upload_photo', methods=['GET', 'POST'])
def upload_photo():
    if request.method == 'GET':
        pass

    if request.method == 'POST':
        data = request.get_json()
        name = data['name']
        image = data['image']

        print(image)
        save_image(image, path="name.png")

        return make_response({"status": 200}, 200)
    
    return make_response({"status": 400}, 400)

if __name__ == '__main__':
    app.run(host='127.0.0.1', debug=True)
