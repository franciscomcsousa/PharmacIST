#Common Response Messages
#Successful Responses: 200 - 300

OK_STATUS = 200 
CREATED_STATUS = 201 
ACCEPTED_STATUS = 202  

# Client Bad Input Error Responses: 400 - 450

BAD_REQUEST_STATUS = 400
UNAUTHORIZED_STATUS = 401
NOT_FOUND_STATUS = 404

# Specific Error Responses (Client Error): 450 - 500

WRONG_LOGIN_INFO_STATUS = 450
USER_DOES_NOT_EXIST_STATUS = 451
USER_ALREADY_EXISTS_STATUS = 452
MEDICINE_DOES_NOT_EXIST_STATUS = 453

# Server Error Responses: 500 - 600

INTERNAL_SERVER_ERROR_STATUS = 500 
BAD_GATEWAY_STATUS = 502

# Token Error Responses: 600 - 650

TOKEN_IS_MISSING = 600
TOKEN_AS_EXPIRED = 601
INVALID_TOKEN = 602 