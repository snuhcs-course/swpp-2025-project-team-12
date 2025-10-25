from django.http import HttpRequest
from django.views.decorators.http import require_GET
from S3.finance import FinanceS3Client
from utils.pagination import get_pagination
from apps.api.constants import *
from utils.for_api import *
import datetime as _dt
from apps.api.models import RecommendationBatch, RecommendationItem
from Mocks.mock_data import mock_recommendations

@require_GET
def recommendations_personalized(request: HttpRequest):
    """
    GET /api/recommendations/personalized?limit=<int>&offset=<int>
    개인화 추천 (Iter3에서 프로필 DB 연동)
    """
    limit, offset = get_pagination(request, default_limit=10, max_limit=100)

    try:
        # TODO: 프로필 interests[] 연동 + P17 호출
        data = mock_recommendations()
        items = data.get("items", [])
        total = len(items)
        page_items = items[offset:offset + limit]

        return ok({
            "items": page_items,
            "total": total,
            "limit": limit,
            "offset": offset,
            "asOf": iso_now(),
            "source": "mock-personalized"
        })
    except Exception as e:
        return degraded(str(e), source="mock", total=0, limit=limit, offset=offset)