from django.http import HttpRequest
from django.views.decorators.http import require_GET
from S3.finance import FinanceS3Client
from apps.api.constants import *
from utils.for_api import *
from decorators import require_auth
from apps.user.models import User
from utils.pagination import get_pagination
import datetime as _dt
from apps.api.models import RecommendationBatch, RecommendationItem
from Mocks.mock_data import mock_recommendations

@require_GET
@require_auth
def recommendations_personalized(request: HttpRequest, user: User):
    try:
        llm_output = FinanceS3Client().get_json(
            bucket=os.environ.get("FINANCE_BUCKET_NAME"),
            key=f"llm_output/{get_path_with_date('all_industry_picks')}"
        )
    except Exception as e:
        return JsonResponse({"message": "Recommendations Not Updated"}, status=404)

    filtered_output = []
    style_of_user = user.style_set.first()
    if style_of_user:
        strategy = style_of_user.get("strategy").get("strategy")
        interests = style_of_user.get("interests").get("interests")

        for tag in interests:
            datum = llm_output.get(f"{strategy}_{tag}")
            if datum:
                filtered_output.append(datum)
            else:
                pass

    return JsonResponse({"llm_output": filtered_output}, status=200)

    # """
    # GET /api/recommendations/personalized?limit=<int>&offset=<int>
    # 개인화 추천 (Iter3에서 프로필 DB 연동)
    # """
    # limit, offset = get_pagination(request, default_limit=10, max_limit=100)
    #
    # try:
    #     # TODO: 프로필 interests[] 연동 + P17 호출
    #     data = mock_recommendations()
    #     items = data.get("items", [])
    #     total = len(items)
    #     page_items = items[offset:offset + limit]
    #
    #     return ok({
    #         "items": page_items,
    #         "total": total,
    #         "limit": limit,
    #         "offset": offset,
    #         "asOf": iso_now(),
    #         "source": "mock-personalized"
    #     })
    # except Exception as e:
    #     return degraded(str(e), source="mock", total=0, limit=limit, offset=offset)