from django.http import JsonResponse
from decorators import *
from utils.validation import validate_password
import bcrypt
import json

@default_error_handler
@require_auth
def password_view(request, user):
    """
    PUT: change user's password
    """

    if request.method != "PUT":
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)

    ### PUT ###
    body = json.loads(request.body.decode('utf-8'))
    password = body.get("password")


    if password is None:
        return JsonResponse({ "message": "PASSWORD IS REQUIRED" }, status=400)

    try:
        validate_password(password)
    except Exception as e:
        return JsonResponse({ "message": f"{e}" }, status=400)

    hashed = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

    try:
        user.password = hashed
        user.save()
    except Exception as e:
        return JsonResponse({ "message": "PASSWORD SAVE FAILED" }, status=500)


    return JsonResponse({ "message": "PASSWORD UPDATE SUCCESS" }, status=200)