from django.http import JsonResponse
from apps.user.models import User
from utils.token_handler import *
import jwt

def require_auth(function):
    """
    This decorator ensure that user is authorized (and existence of user),
    and inject user instance into the decorated function as a keyword argument.
    """
    def wrapper(*args, **kwargs):
        # variables for refresh token handling
        refresh_flag = False
        refresh_token = None

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
                user_id = decode_token(refresh_token).get("id")
                refresh_flag = True # it means refresh token exists and decoded successfully.
            except jwt.ExpiredSignatureError:
                return JsonResponse({ "message": "TOKEN EXPIRED" }, status=401)

        try:
            user = User.objects.get(id=user_id)
        except Exception as e:
            return JsonResponse({"message": "UNEXPECTED ERROR (USER NOT FOUND)"}, status=500)
        kwargs["user"] = user

        # if given refresh token is different from the one in DB, duplicated refresh_token detected
        if refresh_flag and user.refresh_token != refresh_token:
            try:
                user.refresh_token = ""
                user.save()
            except:
                return JsonResponse({ "message": "UNEXPECTED ERROR" }, status=500)
            print("*** DUPLICATED REFRESH TOKEN DETECTED ***")
            return JsonResponse({ "message": "ACCESS DENIED: invalid refresh token detected" }, status=401)

        # valid refresh token case / assume access token exists
        response = function(*args, **kwargs)

        # RTR (Refresh Token Rotation)
        if refresh_flag:
            try:
                new_refresh_token = rotate_refresh_token(refresh_token)
                user.refresh_token = new_refresh_token
                user.save()
                set_cookie(response, "refresh_token", new_refresh_token)
                set_cookie(response, "access_token", make_access_token(user_id))
            except:
                return JsonResponse({ "message": "UNEXPECTED ERROR in RTR(Refresh Token Rotation)" }, status=500)
        return response

    return wrapper
