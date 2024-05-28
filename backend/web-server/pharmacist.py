from flask import Flask, render_template, make_response, request, jsonify
from datetime import datetime, timedelta
from flask import json
from functools import wraps
from models import *
import jwt
import firebase_admin
from firebase_admin import credentials

app = Flask(__name__)

cred = credentials.Certificate("serviceAccountKey.json")    # used for cloud messaging
app.config['SECRET_KEY'] = 'manteiga de amendoim'           # used to encrypt the Jtokens

firebase_admin.initialize_app(cred)

# Decorator to require login
# Token verification in headers
def login_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('Authorization')
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


@app.route('/')
def home():
    return render_template('home.html') 


# TODO 
# add salt to passwords + encryption ?
# verify here if parameters are not null
@app.route('/register', methods=['POST'])
def register_user():
        
    # TODO - verify if all data is correct
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        fcm_token = data['fcmToken']
        device_id = data['deviceId']
        
        # TODO - here or in the android part
        # verify if the fcm_token device_id are not null!
        user_id, status = create_user(username, password)
        
        if (status == OK_STATUS):
            
            register_device(user_id, fcm_token, device_id)
            
            # if register successful send token and user stays logged in!
            # maybe also use login_user(), but for simplicity tokens do the job
            
            #token_encode = jwt.encode({'username': username, 'exp': datetime.utcnow() 
            #                    + timedelta(days=1)}, app.config['SECRET_KEY'])
            
            token = jwt.encode({'exp': datetime.utcnow() + timedelta(days=1)}, app.config['SECRET_KEY'])
                    
            return make_response(jsonify({'token': token}), status)
        
        return make_response(jsonify({'token': ""}), status)

    
    return make_response({"status":BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)


# TODO 
# verify here if parameters are not null
@app.route('/login', methods=['POST'])
def login_user():
        
    # TODO - verify if all data is correct
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        fcm_token = data['fcmToken']
        device_id = data['deviceId']
        # in case of the guests logins: 
        # if they dont exist, create a new one
        # if they exist, simply login
        if username.startswith("guest_"):
            status, user_id = login_guest(username, password)
        
        else:
            status, user_id = verify_user(username, password)
        
        if (status == OK_STATUS):
            
            # makes sure it has been registered in the devices
            # pbbly used when the user signs in a new device
            register_device(user_id, fcm_token, device_id)
            
            # TODO - is sendong token even if unsucessful
            # if register successful send token and user stays logged in!
            # maybe also use login_user(), but for simplicity tokens do the job
            token = jwt.encode({'exp': datetime.utcnow() + timedelta(days=1)}, app.config['SECRET_KEY'])
            
            return make_response(jsonify({'token': token}), status)
        return make_response(jsonify({'token': ""}), status)
    
    return make_response({"status":BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/authorized')
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

        return make_response(jsonify({"pharmacies": pharmacies}), 200)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/nearby_pharmacies', methods=['GET', 'POST'])
def get_nearby_pharmacies():
    if request.method == 'GET':
        pass

    if request.method == 'POST':
        data = request.get_json()
        latitude = data['latitude']
        longitude = data['longitude']
        pharmacies = get_nearby_pharmacies_db(latitude, longitude)

        return make_response(jsonify({"pharmacies": pharmacies}), 200)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/pharmacy_image', methods=['GET', 'POST'])
def get_pharmacy_image():
    if request.method == 'GET':
        pass
    #HERE
    if request.method == 'POST':
        data = request.get_json()
        id = int(float(data))
        image = get_image(str(id), "P")

    return make_response(jsonify({"image": base64.b64encode(image).decode()}), OK_STATUS)

@app.route('/medicine_image', methods=['GET'])
def get_medicine_image():

    if request.method == 'GET':
        id = int(float(request.args.get("id")))
        image = get_image(str(id), "M")

    return make_response(jsonify({"image": base64.b64encode(image).decode()}), OK_STATUS)

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
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/pharmacy_favorite', methods=['GET', 'POST'])
def favorite_pharmacy():
    
    # get user favorite pharmacies
    if request.method == 'GET':
        pass
    
    # update favorite state based on the boolean isfavorite
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        pharmacy_id = data['pharmacyId']
        status = update_favorite_pharmacies(username, pharmacy_id)
        
        return make_response({"status": status}, status)
        
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/is_pharmacy_favorite', methods=['GET', 'POST'])
def is_favorite_pharmacy():
    if request.method == 'GET':
        pass
    
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        pharmacy_id = data['pharmacyId']
        status = is_pharmacy_favorite(username, pharmacy_id)
        
        return make_response({"status": status}, status)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/get_favorite_pharmacies', methods=['GET', 'POST'])
def get_favorite_pharmacies():
    if request.method == 'GET':
        pass
    
    if request.method == 'POST':
        data = request.get_json()
        username = data
        pharmacies = get_user_favorite_pharmacies(username)
        
        return make_response(jsonify({"pharmacies": pharmacies}), 200)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)
        
@app.route('/medicine', methods=['GET', 'POST'])
def get_medicines():
    if request.method == 'GET':
        # TODO - further verification?
        medicine_id = request.args.get("id")
        medicine, status = get_medicine_by_id(medicine_id)
        return make_response(jsonify({"medicine": medicine}), status)

    if request.method == 'POST':
        # get medicines that contain the substring in their name passed in the request
        data = request.get_json()
        substring = data['name']
        medicines = get_medicines_with_substring(substring)

        return make_response(jsonify({"medicine": medicines}), 200)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)


@app.route('/create_medicine', methods=['POST'])
def create_medicines():
    if request.method == 'POST':
        data = request.get_json()
        
        medicine_id = data['id']
        medicine_name = data['name']
        quantity = data['stock']
        purpose = data['purpose']
        pharmacy_id = data['pharmacyId']
        image = data['image']
        
        status = create_medicine(medicine_id, medicine_name, quantity, purpose, pharmacy_id, image)
        return make_response(jsonify({"status": status}), status)
    
    return make_response(jsonify({"status": BAD_REQUEST_STATUS}), BAD_REQUEST_STATUS)
        
        

@app.route('/medicine_location', methods=['GET', 'POST'])
def get_medicine_location():
    if request.method == 'GET':
        pass

    if request.method == 'POST':
        data = request.get_json()
        name = data['name']
        latitude = data['latitude']
        longitude = data['longitude']

        # verify if medicine exists
        status = verify_medicine(name)
        if status == MEDICINE_DOES_NOT_EXIST_STATUS:
            return make_response({"status": status}, status)
        
        # get the medicine info and also the nearest pharmacy that has it
        medicine = get_requested_medicine(name)
        pharmacy = get_closest_pharmacy_with_medicine(name, latitude, longitude)
        response = (medicine, pharmacy)

        return make_response(jsonify({"medicine": response}), 200)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/pharmacy_stock', methods=['GET', 'POST'])
def pharmacy_stock():
    if request.method == 'GET':
        # TODO - further verification?
        medicine_id = request.args.get("medicineId")
        pharmacy_id = request.args.get("pharmacyId")
        result, status = get_pharmacy_stock_id(medicine_id, pharmacy_id)
        return make_response(jsonify({"stock": result}), status)
        
    
    if request.method == 'POST':
        data = request.get_json()
        substring = data['substring']
        pharmacy_id = data['pharmacyId']

        medicine, status = get_pharmacy_stock(substring=substring, pharmacy_id=pharmacy_id)
        return make_response(jsonify({"medicine": medicine}), status)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/medicine_near_pharmacies', methods=['GET', 'POST'])
def medicine_near_pharmacies():
    if request.method == 'GET':
        pass
    
    if request.method == 'POST':
        data = request.get_json()
        medicineName = data['name']
        latitude = data['latitude']
        longitude = data['longitude']

        pharmacies_stock = get_near_pharmacies(medicineName, latitude, longitude)

        return make_response(jsonify({"pharmaciesStock": pharmacies_stock}), 200)
    
    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)

@app.route('/update_stock', methods=['POST'])
def update_stock():
    if request.method == 'POST':
        data = request.get_json()
        pharmacy_id = data[0]['pharmacyId']
        
        # Extracting medicine_id and stock and creating a list of lists
        medicine_stock_list = [[entry['id'], entry['stock']] for entry in data]
        status = update_pharmacy_stock(medicine_stock_list, pharmacy_id)

        return make_response({"status": status}, status)

    return make_response({"status": BAD_REQUEST_STATUS}, BAD_REQUEST_STATUS)


if __name__ == '__main__':
    app.run(host='127.0.0.1', debug=True)
