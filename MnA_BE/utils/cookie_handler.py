import jwt
import os
from datetime import datetime, timedelta

def set_cookie(response, id):
    token = jwt.encode(
        {
            "id": id,
            "exp": datetime.utcnow() + timedelta(minutes=int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES")))
        },
        os.getenv("SECRET_KEY"),
        algorithm=os.getenv("HASH_ALGORITHM")
    )
    response.set_cookie(
        key="access_token",
        value=token,
        httponly=True,
        secure=True,
        samesite='None',
    )

def delete_cookie(response):
    response.delete_cookie("access_token")