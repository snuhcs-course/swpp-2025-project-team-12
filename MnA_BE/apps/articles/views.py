# apps/articles/views.py
from django.http import JsonResponse, HttpResponseBadRequest
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi

from decorators import default_error_handler
from .services import list_articles, get_article_by_id


# ============================================================================
# Serializers
# ============================================================================


class ArticleDetailItemSerializer(serializers.Serializer):
    title = serializers.CharField()
    url = serializers.CharField()
    source = serializers.CharField()
    section = serializers.CharField()
    published_at = serializers.CharField()
    fetched_at = serializers.CharField()
    content = serializers.CharField()
    content_length = serializers.IntegerField()


class ArticleListResponseSerializer(serializers.Serializer):
    data = ArticleDetailItemSerializer(many=True)


class ArticleDateResponseSerializer(serializers.Serializer):
    date = serializers.CharField()
    data = ArticleDetailItemSerializer(many=True)


class ArticleDetailResponseSerializer(serializers.Serializer):
    title = serializers.CharField()
    url = serializers.CharField()
    source = serializers.CharField()
    section = serializers.CharField()
    published_at = serializers.CharField()
    fetched_at = serializers.CharField()
    content = serializers.CharField()
    content_length = serializers.IntegerField()


class ArticleErrorResponseSerializer(serializers.Serializer):
    message = serializers.CharField()


# ============================================================================
# Views
# ============================================================================


class ArticleView(viewsets.ViewSet):
    """
    Article Views - Financial news articles
    """

    @swagger_auto_schema(
        operation_description="Get today's financial news articles",
        responses={200: ArticleListResponseSerializer()},
    )
    @action(detail=False, methods=["get"])
    @default_error_handler
    def get(self, request):
        data = list_articles(None)
        return JsonResponse({"data": data}, status=200)

    @swagger_auto_schema(
        operation_description="Get financial news articles for a specific date",
        manual_parameters=[
            openapi.Parameter(
                "date",
                openapi.IN_PATH,
                description="Date in YYYY-MM-DD format (e.g., 2025-11-19)",
                type=openapi.TYPE_STRING,
                required=True,
            ),
        ],
        responses={
            200: ArticleDateResponseSerializer(),
            400: openapi.Response(description="Invalid date format"),
        },
    )
    @action(detail=False, methods=["get"])
    @default_error_handler
    def get_by_date(self, request, date):
        try:
            data = list_articles(date)
        except ValueError:
            return HttpResponseBadRequest("Invalid date format, expected YYYY-MM-DD")
        return JsonResponse({"date": date, "data": data}, status=200)

    @swagger_auto_schema(
        operation_description="Get detailed article by ID with optional date filter",
        manual_parameters=[
            openapi.Parameter(
                "id",
                openapi.IN_PATH,
                description="Article ID (integer index)",
                type=openapi.TYPE_INTEGER,
                required=True,
            ),
            openapi.Parameter(
                "date",
                openapi.IN_QUERY,
                description="Optional: Specific date to search in (YYYY-MM-DD format)",
                type=openapi.TYPE_STRING,
            ),
        ],
        responses={200: ArticleDetailResponseSerializer(), 404: ArticleErrorResponseSerializer()},
    )
    @action(detail=False, methods=["get"])
    @default_error_handler
    def get_detail(self, request, id):
        date = request.GET.get("date")
        doc = get_article_by_id(id, date)
        if not doc:
            return JsonResponse({"message": "Not found"}, status=404)
        return JsonResponse(doc, status=200)
