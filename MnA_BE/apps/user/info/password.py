from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from decorators import *
from utils.validation import validate_password
import bcrypt
import json


# ============================================================================
# Serializers
# ============================================================================


class PasswordRequestSerializer(serializers.Serializer):
    password = serializers.CharField(help_text="New password")


class PasswordMessageResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================


class PasswordView(viewsets.ViewSet):
    """
    User Password Views - Update password
    """

    @swagger_auto_schema(
        operation_description="Change user password (requires authentication)",
        request_body=PasswordRequestSerializer,
        responses={
            200: PasswordMessageResponseSerializer(),
            400: openapi.Response(description="Invalid password format or missing"),
            500: openapi.Response(description="Save failed"),
        },
    )
    @action(detail=False, methods=["put"])
    @default_error_handler
    @require_auth
    def put(self, request, user):
        body = json.loads(request.body.decode("utf-8"))
        password = body.get("password")

        if password is None:
            return JsonResponse({"message": "PASSWORD IS REQUIRED"}, status=400)

        try:
            validate_password(password)
        except Exception as e:
            return JsonResponse({"message": f"{e}"}, status=400)

        hashed = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

        try:
            user.password = hashed
            user.save()
        except Exception as e:
            return JsonResponse({"message": "PASSWORD SAVE FAILED"}, status=500)

        return JsonResponse({"message": "PASSWORD UPDATE SUCCESS"}, status=200)
