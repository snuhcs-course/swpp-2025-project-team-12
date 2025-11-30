from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from decorators import *
import json


# ============================================================================
# Serializers
# ============================================================================


class PortfolioResponseSerializer(serializers.Serializer):
    portfolio = serializers.ListField(child=serializers.CharField())


class PortfolioRequestSerializer(serializers.Serializer):
    portfolio = serializers.ListField(
        child=serializers.CharField(), help_text="List of stock tickers"
    )


class PortfolioMessageResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================


class PortfolioView(viewsets.ViewSet):
    """
    User Portfolio Views - Get and update user's stock portfolio
    """

    @swagger_auto_schema(
        operation_description="Get user's stock portfolio (requires authentication)",
        responses={200: PortfolioResponseSerializer()},
    )
    @action(detail=True, methods=["get"])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        return JsonResponse({"portfolio": user.portfolio}, status=200)

    @swagger_auto_schema(
        operation_description="Update user's stock portfolio (requires authentication)",
        request_body=PortfolioRequestSerializer,
        responses={
            200: PortfolioMessageResponseSerializer(),
            400: openapi.Response(description="Invalid input"),
            500: openapi.Response(description="Save failed"),
        },
    )
    @action(detail=True, methods=["post"])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        body = json.loads(request.body.decode("utf-8"))
        portfolio = body.get("portfolio")

        if portfolio is None:
            return JsonResponse({"message": "CANNOT FIND INPUT"}, status=400)

        # TODO: stock name validation

        try:
            user.portfolio = portfolio
            user.save()
        except Exception as e:
            return JsonResponse({"message": "PORTFOLIO SAVE FAILED"}, status=500)

        return JsonResponse({"message": "PORTFOLIO UPDATE SUCCESS"}, status=200)
