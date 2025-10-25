# apps/api/views.py
import os
from typing import Any, Dict
from django.http import HttpRequest
from django.views.decorators.http import require_GET
from apps.common.s3_reader import read_latest_parquet_df, read_latest_json, find_latest_object
from apps.common.mock_data import MOCK_INDICES, MOCK_ARTICLES, mock_recommendations
from .utils import get_pagination, ok, degraded, iso_now
import pytz, datetime as _dt
from apps.api.models import RecommendationBatch, RecommendationItem

KST = pytz.timezone("Asia/Seoul")

S3_PREFIX_COMPANY = os.getenv("S3_PREFIX_COMPANY_PROFILE", "company-profile/")
S3_PREFIX_PRICE   = os.getenv("S3_PREFIX_PRICE_FIN",      "price-financial-info/")
S3_PREFIX_INDICES = os.getenv("S3_PREFIX_INDICES",        "stock-index/")
S3_PREFIX_ARTICLE = os.getenv("S3_PREFIX_ARTICLES",       "articles/")

ARTICLES_SOURCE   = os.getenv("ARTICLES_SOURCE", "mock")   # mock | s3
INDICES_SOURCE    = os.getenv("INDICES_SOURCE",  "mock")   # mock | s3

def _market_date_kst():
    return _dt.datetime.now(KST).strftime("%Y-%m-%d")


@require_GET
def health(request: HttpRequest):
    """
    GET /api/health
    각 소스별 최신 객체의 존재/시각 확인
    """
    def check_s3_source(prefix):
        obj = find_latest_object(prefix)
        if not obj:
            return {"ok": False, "latest": None}
        # 날짜 추출 시도 (파일명에서 YYYY-MM-DD 패턴)
        key = obj["Key"]
        date_str = None
        if "/" in key:
            filename = key.split("/")[-1]
            # 간단히 YYYY-MM-DD.확장자 형태 가정
            if filename.count("-") >= 2:
                date_str = filename.split(".")[0]  # 2025-10-01
        return {
            "ok": True,
            "latest": date_str or obj["LastModified"].strftime("%Y-%m-%d")
        }
    
    s3_status = {
        "ok": True,
        "latest": {
            "companyProfile": check_s3_source(S3_PREFIX_COMPANY)["latest"],
            "priceFinancial": check_s3_source(S3_PREFIX_PRICE)["latest"]
        }
    }
    
    # DB 체크는 간단히 (실제로는 DB 쿼리 시도)
    db_status = {"ok": True}
    
    return ok({
        "api": "ok",
        "s3": s3_status,
        "db": db_status,
        "asOf": iso_now()
    })

@require_GET
def indices(request: HttpRequest):
    """
    GET /api/indices
    지수: S3 있으면 읽고, 없으면 mock
    """
    if INDICES_SOURCE == "s3":
        try:
            data, ts = read_latest_json(S3_PREFIX_INDICES)
            if data:
                return ok({
                    "kospi": data.get("kospi", {"value": 0, "changePct": 0}),
                    "kosdaq": data.get("kosdaq", {"value": 0, "changePct": 0}),
                    "asOf": ts,
                    "source": "s3"
                })
        except Exception as e:
            return degraded(str(e), source="s3")
    
    # Mock fallback
    return ok(MOCK_INDICES)

@require_GET
def articles_top(request: HttpRequest):
    """
    GET /api/articles/top?limit=<int>&offset=<int>
    상위 기사: 페이지네이션 적용
    """
    limit, offset = get_pagination(request, default_limit=10, max_limit=50)
    
    if ARTICLES_SOURCE == "s3":
        try:
            data, ts = read_latest_json(S3_PREFIX_ARTICLE)
            if data:
                items = data.get("items", [])
                total = len(items)
                page_items = items[offset:offset+limit]
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
    page_items = items[offset:offset+limit]
    return ok({
        "items": page_items,
        "total": total,
        "limit": limit,
        "offset": offset,
        "asOf": MOCK_ARTICLES.get("asOf", iso_now()),
        "source": "mock"
    })

@require_GET
def company_profiles(request: HttpRequest):
    """
    GET /api/company-profiles?limit=<int>&offset=<int>&market=kospi|kosdaq&date=YYYY-MM-DD
    회사 프로필: parquet 최신 파일에서 페이지네이션 적용
    """
    limit, offset = get_pagination(request, default_limit=10, max_limit=100)
    market = request.GET.get("market", "kosdaq")
    date = request.GET.get("date")
    symbol = request.GET.get("symbol")  # 특정 심볼 조회용 (선택)
    
    try:
        df, ts = read_latest_parquet_df(S3_PREFIX_COMPANY)
        if df is None or len(df) == 0:
            return degraded("no parquet found", source="s3", total=0, limit=limit, offset=offset)
        
        # 특정 심볼 검색
        if symbol:
            if symbol in df.index:
                row = df.loc[symbol]
                items = [{"ticker": symbol, "explanation": str(row["explanation"])}]
                return ok({
                    "items": items,
                    "total": 1,
                    "limit": limit,
                    "offset": 0,
                    "source": "s3",
                    "asOf": ts or date or iso_now()
                })
            else:
                return ok({
                    "items": [],
                    "total": 0,
                    "limit": limit,
                    "offset": offset,
                    "source": "s3",
                    "asOf": ts or date or iso_now()
                })
        
        # 페이지네이션
        total = len(df)
        page_df = df.iloc[offset:offset+limit]
        
        items = [
            {"ticker": idx, "explanation": str(row["explanation"])}
            for idx, row in page_df.iterrows()
        ]
        
        return ok({
            "items": items,
            "total": total,
            "limit": limit,
            "offset": offset,
            "source": "s3",
            "asOf": ts or date or iso_now()
        })
        
    except Exception as e:
        return degraded(str(e), source="s3", total=0, limit=limit, offset=offset)

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
            # 신규 news(JSON) 우선 사용, 없으면 legacy(titles/urls) 병합
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
            "source": batch.source,               # "mock" or "llm"
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
        "marketDate": _market_date_kst(),
        "personalized": False,
    })

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
        page_items = items[offset:offset+limit]
        
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

@require_GET
def reports_detail(request: HttpRequest, symbol: str):
    """
    GET /api/reports/{symbol}
    상세 리포트: 회사 프로필 + (선택) 가격/지표 + 기사 요약
    """
    try:
        profile_df, ts_prof = read_latest_parquet_df(S3_PREFIX_COMPANY)
        profile = None
        
        if profile_df is not None and symbol in profile_df.index:
            row = profile_df.loc[symbol]
            profile = {
                "symbol": symbol,
                "explanation": str(row["explanation"])
            }
        
        # 추가 데이터는 아직 형식 미정 → 폴백
        resp = {
            "profile": profile,
            "price": None,
            "indicesSnippet": MOCK_INDICES if INDICES_SOURCE != "s3" else None,
            "articles": MOCK_ARTICLES.get("items", [])[:5] if ARTICLES_SOURCE != "s3" else [],
            "asOf": ts_prof or iso_now(),
            "source": "s3" if profile else "empty"
        }
        
        return ok(resp)
        
    except Exception as e:
        return degraded(str(e), source="s3")