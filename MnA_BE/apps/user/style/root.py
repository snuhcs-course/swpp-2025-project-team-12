from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from decorators import *
from apps.user.models import Style
import json


# ============================================================================
# Serializers
# ============================================================================

class StyleDataSerializer(serializers.Serializer):
    interests = serializers.ListField(child=serializers.CharField())
    strategy = serializers.CharField()
    create_at = serializers.DateTimeField()

class StyleGetResponseSerializer(serializers.Serializer):
    style = StyleDataSerializer(allow_null=True)

class StylePostRequestSerializer(serializers.Serializer):
    interests = serializers.ListField(child=serializers.CharField(), help_text="List of user interests")
    strategy = serializers.CharField(help_text="Investment strategy")

class StyleMessageResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================

class StyleView(viewsets.ViewSet):
    """
    Style Views - User interests and investment strategy
    """

    @swagger_auto_schema(
        operation_description="Get user's latest interests and strategy (requires authentication)",
        responses={
            200: StyleGetResponseSerializer()
        }
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        try:
            style_row = user.style_set.all()[0]

            # filtering style columns
            style = {
                "interests": style_row.interests,
                "strategy": style_row.strategy,
                "create_at": style_row.create_at
            }
        except Exception as e:
            style = None

        return JsonResponse({"style": style}, status=200)

    @swagger_auto_schema(
        operation_description="Create new interests and strategy entry (requires authentication)",
        request_body=StylePostRequestSerializer,
        responses={
            200: StyleMessageResponseSerializer(),
            400: openapi.Response(description="Missing required fields"),
            500: openapi.Response(description="Save failed")
        }
    )
    @action(detail=False, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        body = json.loads(request.body.decode('utf-8'))
        interests = body.get('interests')
        strategy = body.get('strategy')

        if interests is None:
            return JsonResponse({"message": "INTERESTS REQUIRED"}, status=400)

        if strategy is None:
            return JsonResponse({"message": "STRATEGY REQUIRED"}, status=400)

        try:
            new_style = Style.objects.create(
                user=user,
                interests=interests,
                strategy=strategy
            )
            new_style.save()
        except Exception as e:
            return JsonResponse({"message": "SAVE INTERESTS FAILED"}, status=500)

        return JsonResponse({"message": "INTERESTS UPDATE SUCCESS"}, status=200)