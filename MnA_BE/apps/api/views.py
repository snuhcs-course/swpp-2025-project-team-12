# apps/api/views.py

from django.http import HttpRequest
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceS3Client
from Mocks.mock_data import MOCK_INDICES, MOCK_ARTICLES
from decorators import default_error_handler
from utils.pagination import get_pagination
from utils.for_api import *
from apps.api.constants import *

class APIView(viewsets.ViewSet):

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_health(self, request: HttpRequest):
        """
        GET /api/health
        각 소스별 최신 객체의 존재/시각 확인
        """

        company_profile_head = (FinanceS3Client()
        .check_source(
            FINANCE_BUCKET,
            S3_PREFIX_COMPANY
        ))
        price_financial_head = (FinanceS3Client()
        .check_source(
            FINANCE_BUCKET,
            S3_PREFIX_PRICE
        ))

        s3_status = {
            "ok": True,
            "latest": {
                "companyProfile": company_profile_head["latest"],
                "priceFinancial": price_financial_head["latest"]
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

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_indices(self, request: HttpRequest):
        """
        GET /api/indices
        지수: S3 있으면 읽고, 없으면 mock
        """
        if INDICES_SOURCE == "s3":
            try:
                data, ts = FinanceS3Client().get_latest_json(
                    FINANCE_BUCKET,
                    S3_PREFIX_INDICES
                )

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

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_profiles(self, request: HttpRequest):
        """
        GET /api/company-profiles?limit=<int>&offset=<int>&market=kospi|kosdaq&date=YYYY-MM-DD
        회사 프로필: parquet 최신 파일에서 페이지네이션 적용
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        market = request.GET.get("market", "kosdaq")
        date = request.GET.get("date")
        symbol = request.GET.get("symbol")  # 특정 심볼 조회용 (선택)

        try:
            df, ts = FinanceS3Client().get_latest_parquet_df(
                FINANCE_BUCKET,
                S3_PREFIX_COMPANY
            )
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
            page_df = df.iloc[offset:offset + limit]

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

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_reports_detail(self, request: HttpRequest, symbol: str):
        """
        GET /api/reports/{symbol}
        상세 리포트: 회사 프로필 + (선택) 가격/지표 + 기사 요약
        """
        try:
            profile_df, ts_prof = FinanceS3Client().get_latest_parquet_df(
                FINANCE_BUCKET,
                S3_PREFIX_COMPANY
            )
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