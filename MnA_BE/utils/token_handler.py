import jwt
import os
from datetime import datetime, timedelta

def decode_token(token):
    return jwt.decode(
                token,
                os.getenv("SECRET_KEY"),
                algorithms=[os.getenv("HASH_ALGORITHM")],
            )

def make_access_token(id):
    return jwt.encode(
        {
            "id": id,
            "exp": datetime.utcnow() + timedelta(minutes=int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES")))
        },
        os.getenv("SECRET_KEY"),
        algorithm=os.getenv("HASH_ALGORITHM")
    )

def make_refresh_token(id):
    return jwt.encode(
        {
            "id": id,
            "exp": datetime.utcnow() + timedelta(days=int(os.getenv("REFRESH_TOKEN_EXPIRE_DAYS")))
        },
        os.getenv("SECRET_KEY"),
        algorithm=os.getenv("HASH_ALGORITHM")
    )

def set_cookie(response, key, value):
    response.set_cookie(
        key=key,
        value=value,
        httponly=True,
        secure=True,
        samesite="None",
    )

def delete_cookie(response):
    response.delete_cookie("refresh_token")
    response.delete_cookie("access_token")