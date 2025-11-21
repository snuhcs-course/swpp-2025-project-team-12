from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from decorators import *
from S3.base import BaseBucket
import json
import os


# ============================================================================
# Serializers
# ============================================================================

class ProfileImageResponseSerializer(serializers.Serializer):
    image_url = serializers.CharField()

class ProfileImageRequestSerializer(serializers.Serializer):
    image_url = serializers.CharField(help_text="Base64-encoded image data URL")

class ProfileMessageResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================

class ProfileView(viewsets.ViewSet):
    """
    User Profile Views - Profile image operations (S3 storage)
    """

    @swagger_auto_schema(
        operation_description="Get user's profile image URL from S3 (requires authentication)",
        responses={
            200: ProfileImageResponseSerializer(),
            500: openapi.Response(description="S3 retrieval failed or image not found")
        }
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        try:
            image_url = BaseBucket().get_image_url(str(user.id))
        except Exception as e:
            return JsonResponse({"message": "S3 GET FAILED, maybe NOT FOUND"}, status=500)

        return JsonResponse({"image_url": image_url}, status=200)

    @swagger_auto_schema(
        operation_description="Upload user's profile image to S3 (requires authentication)",
        request_body=ProfileImageRequestSerializer,
        responses={
            200: ProfileMessageResponseSerializer(),
            400: openapi.Response(description="Image data required"),
            500: openapi.Response(description="S3 upload failed")
        }
    )
    @action(detail=False, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        body = json.loads(request.body.decode('utf-8'))
        image_url = body.get("image_url")

        if image_url is None:
            return JsonResponse({"message": "IMAGE REQUIRED"}, status=400)

        try:
            BaseBucket().put_image(str(user.id), image_url)
        except Exception as e:
            return JsonResponse({"message": "S3 PUT FAILED"}, status=500)

        return JsonResponse({"message": "PROFILE IMAGE UPLOAD SUCCESS"}, status=200)

    @swagger_auto_schema(
        operation_description="Delete user's profile image from S3 (requires authentication)",
        responses={
            200: ProfileMessageResponseSerializer()
        }
    )
    @action(detail=False, methods=['delete'])
    @default_error_handler
    @require_auth
    def delete(self, request, user):
        BaseBucket().delete(str(user.id))

        return JsonResponse({"message": "PROFILE DELETE SUCCESS"}, status=200)