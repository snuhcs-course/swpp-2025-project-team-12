from django.http import JsonResponse
from .models import User
import bcrypt
from utils.cookie_handler import *
from decorators import *
import json


@default_error_handler
def login(request):
    if request.method != "POST":
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)

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
    set_cookie(response, id)
    return response



@default_error_handler
@require_auth
def logout(request, user):
    if request.method != "POST":
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)

    response = JsonResponse({"message": "LOGOUT SUCCESS" }, status=200)
    delete_cookie(response)
    return response



@default_error_handler
def signup(request):
    if request.method != "POST":
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)

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

    # assume login state, after signup
    response = JsonResponse({"message": "User created successfully"}, status=201)
    set_cookie(response, id)

    hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
    try:
        user = User.objects.create(
            id=id,
            password=hashed_password.decode('utf-8')
        )
        user.save()
    except Exception as e:
        print(e)
        return JsonResponse({ "message": "UNEXPECTED ERROR (CREATE FAILED)" }, status=500)

    return response



@default_error_handler
@require_auth
def withdraw(request, user):
    if request.method != "DELETE":
        return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)

    response = JsonResponse({"message": "WITHDRAWAL SUCCESS"}, status=200)
    delete_cookie(response)

    try:
        user.delete()
    except Exception as e:
        return JsonResponse({ "message": "UNEXPECTED ERROR (DELETE FAILED)" }, status=500)

    return response