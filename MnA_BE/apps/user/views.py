# apps/user/views.py

from django.http import JsonResponse
from django.views.decorators.http import require_POST
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi

from .models import User
import bcrypt
from utils.token_handler import *
from utils.validation import validate_password, validate_name
from decorators import *
from S3.base import BaseBucket
from django.views.decorators.csrf import csrf_exempt
import os
import json


# ============================================================================
# Serializers
# ============================================================================


class LoginRequestSerializer(serializers.Serializer):
    id = serializers.CharField(help_text="Username")
    password = serializers.CharField(help_text="User password")


class LoginResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


class SignupRequestSerializer(serializers.Serializer):
    id = serializers.CharField(help_text="Username (must be unique)")
    password = serializers.CharField(help_text="Password (must meet validation requirements)")


class SignupResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


class LogoutResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


class WithdrawResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


class UserErrorResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================


class UserView(viewsets.ModelViewSet):
    """
    User Views - Authentication and account management
    """

    @swagger_auto_schema(
        operation_description="Authenticate user and issue JWT tokens (stored in HttpOnly cookies)",
        request_body=LoginRequestSerializer,
        responses={
            200: LoginResponseSerializer(),
            400: UserErrorResponseSerializer(),
            401: UserErrorResponseSerializer(),
            500: UserErrorResponseSerializer(),
        },
    )
    @action(detail=False, methods=["post"])
    @default_error_handler
    def login(self, request):
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

        try:
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

    @swagger_auto_schema(
        operation_description="Logout user and clear authentication cookies",
        responses={
            200: LogoutResponseSerializer(),
            401: openapi.Response(description="Unauthorized (invalid or missing token)"),
        },
    )
    @action(detail=False, methods=["post"])
    @default_error_handler
    @require_auth
    def logout(self, request, user):
        response = JsonResponse({"message": "LOGOUT SUCCESS"}, status=200)
        delete_cookie(response)
        return response

    @swagger_auto_schema(
        operation_description="Create new user account and automatically login with JWT tokens",
        request_body=SignupRequestSerializer,
        responses={
            201: SignupResponseSerializer(),
            400: UserErrorResponseSerializer(),
            409: UserErrorResponseSerializer(),
            500: UserErrorResponseSerializer(),
        },
    )
    @action(detail=False, methods=["post"])
    @default_error_handler
    def signup(self, request):
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
            user = User.objects.create(
                name=user_id,
                password=hashed,
                refresh_token="",
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

    @swagger_auto_schema(
        operation_description="Permanently delete user account and all associated data (profile image, preferences, style history)",
        responses={
            200: WithdrawResponseSerializer(),
            401: openapi.Response(description="Unauthorized (invalid or missing token)"),
            500: UserErrorResponseSerializer(),
        },
    )
    @action(detail=False, methods=["delete"])
    @default_error_handler
    @require_auth
    def withdraw(self, request, user):
        response = JsonResponse({"message": "WITHDRAWAL SUCCESS"}, status=200)
        delete_cookie(response)

        # Remove profile from S3
        try:
            BaseBucket().get(str(user.id))

            try:
                BaseBucket().delete(str(user.id))
            except Exception:
                return JsonResponse({"message": "PROFILE DELETE FAILED"}, status=500)
        except:  # File doesn't exist
            pass

        # Remove user from database
        try:
            user.delete()
        except Exception:
            return JsonResponse({"message": "USER DELETE FAILED"}, status=500)

        return response
