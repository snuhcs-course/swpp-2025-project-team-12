from django.db import IntegrityError
from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from decorators import *
from utils.validation import validate_name
import json


# ============================================================================
# Serializers
# ============================================================================

class NameResponseSerializer(serializers.Serializer):
    name = serializers.CharField()

class NameRequestSerializer(serializers.Serializer):
    name = serializers.CharField(help_text="New username")

class NameMessageResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================

class NameView(viewsets.ViewSet):
    """
    User Name Views - Get and update username
    """

    @swagger_auto_schema(
        operation_description="Get current username (requires authentication)",
        responses={
            200: NameResponseSerializer()
        }
    )
    @action(detail=True, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        return JsonResponse({"name": user.name}, status=200)

    @swagger_auto_schema(
        operation_description="Update username (requires authentication)",
        request_body=NameRequestSerializer,
        responses={
            200: NameMessageResponseSerializer(),
            400: openapi.Response(description="Invalid name format or missing"),
            409: openapi.Response(description="Name already exists"),
            500: openapi.Response(description="Save failed")
        }
    )
    @action(detail=True, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
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