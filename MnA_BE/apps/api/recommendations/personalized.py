from django.http import HttpRequest
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceS3Client
from apps.api.constants import *
from utils.for_api import *
from decorators import require_auth
from apps.user.models import User
# from utils.pagination import get_pagination
# import datetime as _dt
# from apps.api.models import RecommendationBatch, RecommendationItem
# from Mocks.mock_data import mock_recommendations

class PersonalizedRecommendationsView(viewsets.ViewSet):

    @action(detail=False, methods=['get'])
    @require_auth
    def get(self, request: HttpRequest, year=None, month=None, day=None, user: User=None):
        bucket_name = os.environ.get('FINANCE_BUCKET_NAME')

        # if no date provided, get the latest
        if year is None and month is None and day is None:
            source = FinanceS3Client().check_source(bucket=bucket_name, prefix="llm_output")
            if not source["ok"]: return JsonResponse({"message": "No LLM output found"}, status=404)
            year, month, day = source["latest"].split("-")

        path = f"llm_output/{get_path_with_date('all_industry_picks', year, month, day)}"
        try:
            llm_output = FinanceS3Client().get_json(
                bucket=bucket_name,
                key=path
            )
        except Exception as e:
            return JsonResponse({"message": "Unexpected Server Error"}, status=500)

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