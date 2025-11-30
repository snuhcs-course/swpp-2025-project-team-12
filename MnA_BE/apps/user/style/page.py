from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from decorators import *
from django.core.paginator import Paginator

PAGE_SIZE = 10


# ============================================================================
# Serializers
# ============================================================================


class StyleItemSerializer(serializers.Serializer):
    interests = serializers.ListField(child=serializers.CharField())
    strategy = serializers.CharField()
    create_at = serializers.DateTimeField()


class StylePageResponseSerializer(serializers.Serializer):
    style_page = StyleItemSerializer(many=True)


# ============================================================================
# Views
# ============================================================================


class StylePageView(viewsets.ViewSet):
    """
    Style Page Views - Paginated user interests history
    """

    @swagger_auto_schema(
        operation_description="Get paginated user interests history (requires authentication, 10 items per page)",
        manual_parameters=[
            openapi.Parameter(
                "page_index",
                openapi.IN_PATH,
                description="Page number (starts from 1)",
                type=openapi.TYPE_INTEGER,
                required=True,
            ),
        ],
        responses={200: StylePageResponseSerializer()},
    )
    @action(detail=False, methods=["get"])
    @default_error_handler
    @require_auth
    def get_page(self, request, page_index, user):
        try:
            style_list = user.style_set.all()
            paginator = Paginator(style_list, PAGE_SIZE)
            style_page = [
                {
                    "interests": entry.interests,
                    "strategy": entry.strategy,
                    "create_at": entry.create_at,
                }
                for entry in paginator.page(page_index).object_list
            ]

        except Exception as e:
            style_page = []

        return JsonResponse({"style_page": style_page}, status=200)
