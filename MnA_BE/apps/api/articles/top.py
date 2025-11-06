from django.http import HttpRequest
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceS3Client
from decorators import default_error_handler
from utils.pagination import get_pagination
from apps.api.constants import *
from utils.for_api import *

from Mocks.mock_data import MOCK_ARTICLES

class TopArticleView(viewsets.ViewSet):

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_top(self, request: HttpRequest):
        """
        GET /api/articles/top?limit=<int>&offset=<int>
        상위 기사: 페이지네이션 적용
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=50)

        if ARTICLES_SOURCE == "s3":
            try:
                data, ts = FinanceS3Client().get_latest_json(
                    FINANCE_BUCKET,
                    S3_PREFIX_ARTICLE
                )

                if data:
                    items = data.get("items", [])
                    total = len(items)
                    page_items = items[offset:offset + limit]
                    return ok({
                        "items": page_items,
                        "total": total,
                        "limit": limit,
                        "offset": offset,
                        "asOf": ts,
                        "source": "s3"
                    })
            except Exception as e:
                return degraded(str(e), source="s3", total=0, limit=limit, offset=offset)

        # Mock fallback
        items = MOCK_ARTICLES.get("items", [])
        total = len(items)
        page_items = items[offset:offset + limit]
        return ok({
            "items": page_items,
            "total": total,
            "limit": limit,
            "offset": offset,
            "asOf": MOCK_ARTICLES.get("asOf", iso_now()),
            "source": "mock"
        })