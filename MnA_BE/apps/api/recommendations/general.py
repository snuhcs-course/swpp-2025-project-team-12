import os
from django.http import HttpRequest
from django.views.decorators.http import require_GET
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceS3Client
from apps.api.constants import *
from utils.for_api import *

from utils.pagination import get_pagination
import datetime as _dt
from apps.api.models import RecommendationBatch, RecommendationItem
from Mocks.mock_data import mock_recommendations

class GeneralRecommendationsView(viewsets.ViewSet):

    @action(detail=False, methods=['get'])
    def recommendations_general(self, request: HttpRequest):
        try:
            llm_output = FinanceS3Client().get_json(
                bucket=os.environ.get("FINANCE_BUCKET_NAME"),
                key=f"llm_output/{get_path_with_date('top_picks')}"
            )
        except Exception as e:
            return JsonResponse({"message": "Recommendations Not Updated"}, status=404)

        return JsonResponse({"llm_output": llm_output}, status=200)

    #     if batch:
    #         qs = RecommendationItem.objects.filter(batch=batch).order_by("rank")
    #         total = qs.count()
    #         items = []
    #         for it in qs[offset:offset+limit]:
    #             # ✅ 신규 news(JSON) 우선 사용, 없으면 legacy(titles/urls) 병합
    #             news_payload = getattr(it, "news", None)
    #             if not news_payload:
    #                 titles = getattr(it, "news_titles", []) or []
    #                 urls   = getattr(it, "news_urls", []) or []
    #                 merged = []
    #                 n = max(len(titles), len(urls))
    #                 for i in range(n):
    #                     t = titles[i] if i < len(titles) else ""
    #                     u = urls[i] if i < len(urls) else ""
    #                     merged.append({"title": t or "", "url": u or "", "source": ""})
    #                 news_payload = merged

    #             items.append({
    #                 "ticker": it.ticker,
    #                 "name": it.name,
    #                 "market": getattr(it, "market", None),
    #                 "news": news_payload,
    #                 "reason": it.reason,
    #                 "rank": it.rank,
    #                 "expected_direction": getattr(it, "expected_direction", None),
    #                 "conviction": getattr(it, "conviction", None),
    #             })

    #         return ok({
    #             "items": items,
    #             "total": total,
    #             "limit": limit,
    #             "offset": offset,
    #             "asOf": batch.as_of_utc.isoformat(),
    #             "source": batch.source,   # "mock" or "llm"
    #             "marketDate": market_date.strftime("%Y-%m-%d"),
    #             "personalized": False,
    #         })

    #     # --- 폴백: 기존 mock ---
    #     data = mock_recommendations()
    #     src_items = data.get("items", [])
    #     page_items = src_items[offset:offset+limit]
    #     return ok({
    #         "items": page_items,
    #         "total": len(src_items),
    #         "limit": limit,
    #         "offset": offset,
    #         "asOf": data.get("asOf", iso_now()),
    #         "source": data.get("source", "mock"),
    #         "marketDate": market_date_kst(),
    #         "personalized": False,
    #     })
