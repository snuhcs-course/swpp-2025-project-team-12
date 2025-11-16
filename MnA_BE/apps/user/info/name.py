from django.db import IntegrityError
from django.http import JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action
from decorators import *
from utils.validation import validate_name
import json

class NameView(viewsets.ViewSet):

    @action(detail=True, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        """
        GET: get user's name. (check user by cookie)
        """

        if request.method == "GET":
            ### GET ###

            return JsonResponse({"name": user.name}, status=200)

    @action(detail=True, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        """
        POST: change user's name.
        """
        body = json.loads(request.body.decode('utf-8'))
        name = body.get("name")

        if name is None:
            return JsonResponse({"message": "NAME IS REQUIRED"}, status=400)

        try:
            validate_name(name)
        except Exception as e:
            return JsonResponse({"message": f"{e}"}, status=400)

        try:
            user.name = name
            user.save()
        except IntegrityError:
            return JsonResponse({"message": "NAME ALREADY EXISTS"}, status=409)
        except Exception as e:
            return JsonResponse({"message": "NAME SAVE FAILED"}, status=500)

        return JsonResponse({"message": "NAME UPDATE SUCCESS"}, status=200)
