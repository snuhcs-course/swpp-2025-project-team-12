import re

### Utils ###
def raise_if_password_is_none(password:str):
    if password is None:
        raise Exception("Password cannot be None")
def raise_if_password_too_long(password:str, length):
    if len(password) > length:
        raise Exception(f"Password cannot be longer than {length} characters")

def raise_if_password_too_short(password:str, length):
    if len(password) < length:
        raise Exception(f"Password must be at least {length} characters long")

def raise_if_password_has_not_number(password:str):
    if not re.search(r'\d', password):
        raise Exception("Password must contain at least one number")

def raise_if_password_has_not_uppercase(password:str):
    if not re.search(r'[A-Z]', password):
        raise Exception("Password must contain at least one uppercase letter")

def raise_if_password_has_not_lowercase(password:str):
    if not re.search(r'[a-z]', password):
        raise Exception("Password must contain at least one lowercase letter")

def raise_if_name_is_none(user_id:str):
    if user_id is None:
        raise Exception("User ID cannot be None")

def raise_if_name_is_too_long(name:str, length):
    if len(name) > length:
        raise Exception(f"Name cannot be longer than {length} characters")

### Main parts ###
def validate_password(password:str):
    raise_if_password_is_none(password)
    raise_if_password_too_long(20)
    raise_if_password_too_short(password, 8)
    raise_if_password_has_not_lowercase(password)
    raise_if_password_has_not_uppercase(password)
    raise_if_password_has_not_number(password)

def validate_name(name:str):
    raise_if_name_is_none(name)
    raise_if_name_is_too_long(name, 20)