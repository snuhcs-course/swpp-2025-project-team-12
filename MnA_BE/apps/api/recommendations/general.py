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
def recommendations_general(request: HttpRequest):
    """
    GET /api/recommendations/general?limit=&offset=&date=YYYY-MM-DD&risk=공격투자형
    1) DB 배치가 있으면 DB 결과 서빙
    2) 없으면 기존 mock 결과 폴백
    """
    limit, offset = get_pagination(request, default_limit=10, max_limit=100)
    risk = request.GET.get("risk", "공격투자형")
    date_q = request.GET.get("date")

    # KST 기준 market_date 결정
    if date_q:
        try:
            market_date = _dt.datetime.strptime(date_q, "%Y-%m-%d").date()
        except ValueError:
            market_date = _dt.datetime.now(KST).date()
    else:
        market_date = _dt.datetime.now(KST).date()

    # --- DB 우선 경로 ---
    batch = RecommendationBatch.objects.filter(
        market_date=market_date, risk_profile=risk
    ).first()

    if batch:
        qs = RecommendationItem.objects.filter(batch=batch).order_by("rank")
        total = qs.count()
        items = []
        for it in qs[offset:offset+limit]:
            # ✅ 신규 news(JSON) 우선 사용, 없으면 legacy(titles/urls) 병합
            news_payload = getattr(it, "news", None)
            if not news_payload:
                titles = getattr(it, "news_titles", []) or []
                urls   = getattr(it, "news_urls", []) or []
                merged = []
                n = max(len(titles), len(urls))
                for i in range(n):
                    t = titles[i] if i < len(titles) else ""
                    u = urls[i] if i < len(urls) else ""
                    merged.append({"title": t or "", "url": u or "", "source": ""})
                news_payload = merged

            items.append({
                "ticker": it.ticker,
                "name": it.name,
                "market": getattr(it, "market", None),
                "news": news_payload,
                "reason": it.reason,
                "rank": it.rank,
                "expected_direction": getattr(it, "expected_direction", None),
                "conviction": getattr(it, "conviction", None),
            })

        return ok({
            "items": items,
            "total": total,
            "limit": limit,
            "offset": offset,
            "asOf": batch.as_of_utc.isoformat(),
            "source": batch.source,   # "mock" or "llm"
            "marketDate": market_date.strftime("%Y-%m-%d"),
            "personalized": False,
        })

    # --- 폴백: 기존 mock ---
    data = mock_recommendations()
    src_items = data.get("items", [])
    page_items = src_items[offset:offset+limit]
    return ok({
        "items": page_items,
        "total": len(src_items),
        "limit": limit,
        "offset": offset,
        "asOf": data.get("asOf", iso_now()),
        "source": data.get("source", "mock"),
        "marketDate": market_date_kst(),
        "personalized": False,
    })
