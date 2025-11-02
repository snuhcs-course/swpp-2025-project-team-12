# apps/user/views.py  (merged)

from django.http import JsonResponse
from django.views.decorators.http import require_POST
from rest_framework import viewsets
from rest_framework.decorators import action

from .models import User
import bcrypt
from utils.token_handler import *
from utils.validation import validate_password, validate_name
from decorators import *
from S3.base import S3Client
from django.views.decorators.csrf import csrf_exempt
import os
import json

class UserView(viewsets.ModelViewSet):
    """
    user/ views
    ---
    hello!
    """

    @action(detail=False, methods=['post'])
    @default_error_handler
    @require_POST
    def login(self, request):
        """
        POST: login
        """
        try:
            body = json.loads(request.body.decode("utf-8"))
        except Exception:
            return JsonResponse({"message": "INVALID JSON"}, status=400)

        user_id = body.get("id")  # 기존 클라이언트 호환: 'id'를 username으로 사용
        password = body.get("password")

        if not user_id:
            return JsonResponse({"message": "ID REQUIRED"}, status=400)
        if not password:
            return JsonResponse({"message": "PASSWORD REQUIRED"}, status=400)

        try:
            # PK가 AutoField이므로 name으로 조회
            user = User.objects.get(name=user_id)
        except User.DoesNotExist:
            return JsonResponse({"message": "USER NOT FOUND"}, status=401)

        if not bcrypt.checkpw(password.encode("utf-8"), user.password.encode("utf-8")):
            return JsonResponse({"message": "INVALID PASSWORD"}, status=401)

        response = JsonResponse({"message": "LOGIN SUCCESS"}, status=200)
        try:
            refresh_token = make_refresh_token(str(user.id))
            user.refresh_token = refresh_token
            user.save(update_fields=["refresh_token"])
            set_cookie(response, "refresh_token", refresh_token)
        except Exception:
            return JsonResponse({"message": "TOKEN ISSUE"}, status=500)

        set_cookie(response, "access_token", make_access_token(str(user.id)))
        return response

    @action(detail=False, methods=['post'])
    @default_error_handler
    @require_auth
    def logout(self, request, user):
        """
        POST: logout user
        """
        response = JsonResponse({"message": "LOGOUT SUCCESS"}, status=200)
        delete_cookie(response)
        return response

    @action(detail=False, methods=['post'])
    @default_error_handler
    def signup(self, request):
        """
        POST: create account. require 'id'(username) and 'password'
        - PK는 AutoField, 'id'는 User.name으로 저장
        """

        try:
            body = json.loads(request.body.decode("utf-8"))
        except Exception:
            return JsonResponse({"message": "INVALID JSON"}, status=400)

        user_id = body.get("id")
        password = body.get("password")

        if not user_id:
            return JsonResponse({"message": "ID REQUIRED"}, status=400)
        if not password:
            return JsonResponse({"message": "PASSWORD REQUIRED"}, status=400)

        if User.objects.filter(name=user_id).exists():
            return JsonResponse({"message": "USER ALREADY EXISTS"}, status=409)

        try:
            validate_password(password)
            validate_name(user_id)
        except Exception as e:
            return JsonResponse({"message": f"INVALID ID OR PASSWORD FORMAT {e}"}, status=400)

        hashed = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

        try:
            # 새 유저 생성 (PK는 자동)
            user = User.objects.create(
                name=user_id,
                password=hashed,
                refresh_token="",  # 아래에서 발급 후 업데이트
            )
            refresh_token = make_refresh_token(str(user.id))
            user.refresh_token = refresh_token
            user.save(update_fields=["refresh_token"])

            response = JsonResponse({"message": "User created successfully"}, status=201)
            set_cookie(response, "refresh_token", refresh_token)
            set_cookie(response, "access_token", make_access_token(str(user.id)))
            return response
        except Exception as e:
            print(e)
            return JsonResponse({"message": "USER CREATE FAILED"}, status=500)

    @action(detail=False, methods=['delete'])
    @default_error_handler
    @require_auth
    def withdraw(self, request, user):
        """
        DELETE: delete user account
        """

        if request.method != "DELETE":
            return JsonResponse({"message": "METHOD NOT ALLOWED"}, status=405)

        response = JsonResponse({"message": "WITHDRAWAL SUCCESS"}, status=200)
        delete_cookie(response)

        # remove profile from S3 (키는 문자열로)
        try:
            S3Client().get(os.environ.get("PROFILE_BUCKET_NAME"), str(user.id))

            try:
                S3Client().delete(os.environ.get("PROFILE_BUCKET_NAME"), str(user.id))
            except Exception:
                return JsonResponse({"message": "PROFILE DELETE FAILED"}, status=500)
        except:  # File Not exists
            pass

        # remove user from SQL table
        try:
            user.delete()
        except Exception:
            return JsonResponse({"message": "USER DELETE FAILED"}, status=500)

        return response
