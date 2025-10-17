from django.db import IntegrityError
from django.http import JsonResponse
from decorators import *
from utils.validation import validate_name
import json

@default_error_handler
@require_auth
def name_view(request, user):
    """
    GET: get user's name. (check user by cookie)
    POST: change user's name.
    """

    if request.method == "GET":
        ### GET ###

        return JsonResponse({ "name": user.name }, status=200)


    elif request.method == "POST":
        ### POST ###

        body = json.loads(request.body.decode('utf-8'))
        name = body.get("name")

        if name is None:
            return JsonResponse({ "message": "NAME IS REQUIRED" }, status=400)

        if not validate_name(name):
            return JsonResponse({ "message": "INVALID NAME" }, status=400)

        try:
            user.name = name
            user.save()
        except IntegrityError:
            return JsonResponse({ "message": "NAME ALREADY EXISTS" }, status=409)
        except Exception as e:
            return JsonResponse({ "message": "NAME SAVE FAILED" }, status=500)

        return JsonResponse({ "message": "NAME UPDATE SUCCESS" }, status=200)

    else:
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)