from django.http import JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action
from decorators import *
from S3.base import BaseBucket
import json
import os

class ProfileView(viewsets.ViewSet):
    """
    ViewSet for handling profile image operations.
    """

    @action(detail=False, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        """
        get user's profile image. (base64 url)
        """

        try:
            image_url = BaseBucket().get_image_url(str(user.id))
        except Exception as e:
            return JsonResponse({"message": "S3 GET FAILED, maybe NOT FOUND"}, status=500)

        return JsonResponse({"image_url": image_url}, status=200)


    @action(detail=False, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        """
        post user's profile on storage. (base64 url)
        """

        body = json.loads(request.body.decode('utf-8'))
        image_url = body.get("image_url")

        if image_url is None:
            return JsonResponse({"message": "IMAGE REQUIRED"}, status=400)

        try:
            BaseBucket().put_image(str(user.id), image_url)
        except Exception as e:
            return JsonResponse({"message": "S3 PUT FAILED"}, status=500)

        return JsonResponse({"message": "PROFILE IMAGE UPLOAD SUCCESS"}, status=200)


    @action(detail=False, methods=['delete'])
    @default_error_handler
    @require_auth
    def delete(self, request, user):
        """
        delete user's profile on storage.
        """

        BaseBucket().delete(str(user.id))

        return JsonResponse({"message": "PROFILE DELETE SUCCESS"}, status=200)