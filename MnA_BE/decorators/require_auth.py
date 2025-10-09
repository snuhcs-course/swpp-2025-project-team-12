from django.http import JsonResponse
from apps.user.models import User
import os
import jwt

def require_auth(function):
    """
    This decorator ensure that user is authorized (and existence of user),
    and inject user instance into the decorated function as a keyword argument.
    """
    def wrapper(*args, **kwargs):
        request = args[0]
        access_token = request.COOKIES.get('access_token')

        # unauthorized case (cannot find  access token)
        if access_token is None:
            return JsonResponse({ "message": "Unauthorized" }, status=401)

        user_id = jwt.decode(
            access_token,
            os.getenv("SECRET_KEY"),
            algorithms=[os.getenv("HASH_ALGORITHM")],
        ).get("id")

        try:
            user = User.objects.get(id=user_id)
        except Exception as e:
            return JsonResponse({"message": "UNEXPECTED ERROR (USER NOT FOUND)"}, status=500)
        kwargs["user"] = user

        return function(*args, **kwargs)

    return wrapper
