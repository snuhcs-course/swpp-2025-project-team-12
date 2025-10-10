from django.http import JsonResponse
from apps.user.models import User
from utils.token_handler import *
import os
import jwt

def require_auth(function):
    """
    This decorator ensure that user is authorized (and existence of user),
    and inject user instance into the decorated function as a keyword argument.
    """
    def wrapper(*args, **kwargs):
        refresh_flag = False
        request = args[0]
        access_token = request.COOKIES.get("access_token")

        # unauthorized case (cannot find  access token)
        if access_token is None:
            return JsonResponse({ "message": "Unauthorized" }, status=401)

        # check access token
        try:
            user_id = decode_token(access_token).get("id")
        except jwt.ExpiredSignatureError:
            # access token expired case
            # check refresh token

            refresh_token = request.COOKIES.get("refresh_token")

            if refresh_token is None:
                return JsonResponse({ "message": "UNEXPECTED ERROR" }, status=500)

            try:
                refresh_token = request.COOKIES.get('refresh_token')
                user_id = decode_token(refresh_token).get("id")
                refresh_flag = True
            except:
                return JsonResponse({ "message": "TOKEN EXPIRED" }, status=401)

        try:
            user = User.objects.get(id=user_id)
        except Exception as e:
            return JsonResponse({"message": "UNEXPECTED ERROR (USER NOT FOUND)"}, status=500)
        kwargs["user"] = user


        response = function(*args, **kwargs)
        if refresh_flag:
            set_cookie(response, "access_token", make_access_token(user_id))
        return response

    return wrapper
