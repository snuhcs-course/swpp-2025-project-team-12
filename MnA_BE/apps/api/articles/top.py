from django.http import HttpRequest
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from S3.finance import FinanceBucket
from decorators import default_error_handler
from utils.pagination import get_pagination
from apps.api.constants import *
from utils.for_api import *
import json

from Mocks.mock_data import MOCK_ARTICLES


# Response Serializer 정의
class ArticleItemSerializer(serializers.Serializer):
    title = serializers.CharField()
    url = serializers.CharField()
    source = serializers.CharField()
    section = serializers.CharField()
    published_at = serializers.CharField()
    content = serializers.CharField()
    content_length = serializers.IntegerField()


class ArticleResponseSerializer(serializers.Serializer):
    items = ArticleItemSerializer(many=True)
    total = serializers.IntegerField()
    limit = serializers.IntegerField()
    offset = serializers.IntegerField()
    source = serializers.CharField()


class TopArticleView(viewsets.ViewSet):
    """
    Articles API - Financial news articles
    """

    @swagger_auto_schema(
        operation_description="Get top financial news articles",
        manual_parameters=[
            openapi.Parameter(
                "limit",
                openapi.IN_QUERY,
                description="Number of items (default: 10, max: 50)",
                type=openapi.TYPE_INTEGER,
            ),
            openapi.Parameter(
                "offset",
                openapi.IN_QUERY,
                description="Pagination offset (default: 0)",
                type=openapi.TYPE_INTEGER,
            ),
        ],
        responses={200: ArticleResponseSerializer()},
    )
    @action(detail=False, methods=["get"])
    @default_error_handler
    def get_top(self, request: HttpRequest):
        limit, offset = get_pagination(request, default_limit=10, max_limit=50)

        if ARTICLES_SOURCE == "s3":
            try:
                data, ts = FinanceBucket().get_latest_json(S3_PREFIX_ARTICLE)

                if data:
                    items = data.get("articles", data.get("items", []))
                    total = len(items)
                    page_items = items[offset : offset + limit]
                    return ok(
                        {
                            "items": page_items,
                            "total": total,
                            "limit": limit,
                            "offset": offset,
                            "source": "s3",
                        }
                    )
            except Exception as e:
                return degraded(str(e), source="s3", total=0, limit=limit, offset=offset)

        items = MOCK_ARTICLES.get("items", [])
        total = len(items)
        page_items = items[offset : offset + limit]
        return ok(
            {
                "items": page_items,
                "total": total,
                "limit": limit,
                "offset": offset,
                "asOf": MOCK_ARTICLES.get("asOf", iso_now()),
                "source": "mock",
            }
        )
