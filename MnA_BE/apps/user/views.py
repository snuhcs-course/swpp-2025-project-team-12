from django.http import JsonResponse
from .models import User
import bcrypt
from utils.token_handler import *
from utils.validation import validate_password
from decorators import *
from S3 import S3Client
import os
import json


@default_error_handler
def login(request):
    """
    POST: login with given 'id' and 'password'
    """

    if request.method == "POST":
        ### POST ###

        body = json.loads(request.body.decode('utf-8'))
        id = body.get("id")
        password = body.get("password")

        if id is None:
            return JsonResponse({"message": "ID REQUIRED"}, status=400)
        if password is None:
            return JsonResponse({"message": "PASSWORD REQUIRED"}, status=400)

        try:
            user = User.objects.get(id=id)
        except User.DoesNotExist:
            return JsonResponse({"message": "USER NOT FOUND"}, status=401)

        # compare passwords
        if not bcrypt.checkpw(password.encode('utf-8'), user.password.encode('utf-8')):
            return JsonResponse({"message": "INVALID PASSWORD"}, status=401)

        response = JsonResponse({"message": "LOGIN SUCCESS"}, status=200)
        try:
            refresh_token = make_refresh_token(id)
            user.refresh_token = refresh_token
            user.save()
            set_cookie(response, "refresh_token", refresh_token)
        except:
            return JsonResponse({ "message": "TOKEN ISSUE" }, status=500)
        set_cookie(response, "access_token", make_access_token(id))
        return response


    else:
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)



@default_error_handler
@require_auth
def logout(request, user):
    """
    POST: logout user
    """

    if request.method == "POST":
        ### POST ###

        response = JsonResponse({"message": "LOGOUT SUCCESS"}, status=200)
        delete_cookie(response)
        return response


    else:
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)



@default_error_handler
def signup(request):
    """
    POST: create account. require 'id' and 'password'
    """

    if request.method == "POST":
        ### POST ###

        body = json.loads(request.body.decode('utf-8'))
        id = body.get("id")
        password = body.get("password")

        if id is None:
            return JsonResponse({"message": "ID REQUIRED"}, status=400)
        if password is None:
            return JsonResponse({"message": "PASSWORD REQUIRED"}, status=400)

        # check if user already exists
        if User.objects.filter(id=id).exists():
            return JsonResponse({"message": "USER ALREADY EXISTS"}, status=409)

        # password validation
        if not validate_password(password):
            return JsonResponse({"message": "WRONG PASSWORD FORMAT"}, status=400)

        # assume login state, after signup
        response = JsonResponse({"message": "User created successfully"}, status=201)
        refresh_token = make_refresh_token(id)
        set_cookie(response, "refresh_token", refresh_token)
        set_cookie(response, "access_token", make_access_token(id))

        hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
        try:
            user = User.objects.create(
                id=id,
                password=hashed_password.decode('utf-8'),
                refresh_token=refresh_token,
            )
            user.save()
        except Exception as e:
            print(e)
            return JsonResponse({"message": "USER CREATE FAILED"}, status=500)

        return response


    else:
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)



@default_error_handler
@require_auth
def withdraw(request, user):
    """
    DELETE: delete user account
    """

    if request.method == "DELETE":
        ### DELETE ###

        response = JsonResponse({"message": "WITHDRAWAL SUCCESS"}, status=200)
        delete_cookie(response)

        # remove profile from S3
        try:
            S3Client().delete(os.environ.get("PROFILE_BUCKET_NAME"), user.id)
        except Exception as e:
            return JsonResponse({ "message": "PROFILE DELETE FAILED" }, status=500)


        # remove user from SQL table
        try:
            user.delete()
        except Exception as e:
            return JsonResponse({"message": "USER DELETE FAILED"}, status=500)


        return response


    else:
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)