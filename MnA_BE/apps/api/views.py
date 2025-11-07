# apps/api/views.py

from django.http import HttpRequest
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceS3Client
from Mocks.mock_data import MOCK_INDICES, MOCK_ARTICLES
from decorators import default_error_handler
from utils.debug_print import debug_print
from utils.pagination import get_pagination
from utils.for_api import *
from apps.api.constants import *
from apps.api.apps import ApiConfig  # 추가!
import pandas as pd


def safe_float(value):
    """문자열이나 숫자를 안전하게 float로 변환"""
    if pd.isna(value) or value is None:
        return None
    try:
        return float(value)
    except (ValueError, TypeError):
        return None


def safe_int(value):
    """문자열이나 숫자를 안전하게 int로 변환"""
    if pd.isna(value) or value is None:
        return None
    try:
        return int(float(value))
    except (ValueError, TypeError):
        return None


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
        
        # 메모리 캐시 상태 추가
        cache_status = {
            "instant_loaded": ApiConfig.instant_df is not None,
            "profile_loaded": ApiConfig.profile_df is not None,
            "last_loaded": str(ApiConfig.last_loaded) if ApiConfig.last_loaded else None
        }

        return ok({
            "api": "ok",
            "s3": s3_status,
            "db": db_status,
            "cache": cache_status,
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
                return degraded(
                    str(e), 
                    source="s3",
                    kospi=MOCK_INDICES.get("kospi", {"value": 2500, "changePct": 0}),
                    kosdaq=MOCK_INDICES.get("kosdaq", {"value": 750, "changePct": 0})
                )

        # Mock fallback
        return ok(MOCK_INDICES)

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_list(self, request):
        """
        api/company-list?limit=<int>&offset=<int>&market=kospi|kosdaq
        provide list of names of companies.
        메모리에서 읽기!
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        market = request.GET.get("market", "kosdaq")

        try:
            # 메모리에서 instant 데이터 가져오기
            if ApiConfig.instant_df is None:
                return degraded("Data not loaded in memory", source="memory", total=0, limit=limit, offset=offset)
            
            df = ApiConfig.instant_df
            
            # 최신 날짜만 필터링
            latest_date = df['date'].max()
            df_latest = df[df['date'] == latest_date]
            
            # 시장 필터링
            if market and 'market' in df_latest.columns:
                df_latest = df_latest[df_latest['market'] == market]

            # 페이지네이션
            total = len(df_latest)
            page_df = df_latest.iloc[offset:offset + limit]

            items = [
                {"ticker": row["ticker"], "name": str(row["name"])}
                for idx, row in page_df.iterrows()
            ]

            return ok({
                "items": items,
                "total": total,
                "limit": limit,
                "offset": offset,
                "source": "memory",
                "asOf": str(latest_date) if latest_date else iso_now()
            })

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="memory", total=0, limit=limit, offset=offset)


    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_profiles(self, request: HttpRequest):
        """
        GET /api/company-profiles?limit=<int>&offset=<int>&symbol=<str>
        회사 프로필: 메모리에서 읽기
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        symbol = request.GET.get("symbol")

        try:
            # 메모리에서 profile 데이터 가져오기
            if ApiConfig.profile_df is None:
                return degraded("Profile data not loaded in memory", source="memory", total=0, limit=limit, offset=offset)
            
            df = ApiConfig.profile_df

            # 특정 심볼 검색
            if symbol:
                if symbol in df.index:
                    row = df.loc[symbol]
                    items = [{
                        "ticker": symbol,
                        "explanation": str(row["explanation"])
                    }]
                    return ok({
                        "items": items,
                        "total": 1,
                        "limit": limit,
                        "offset": 0,
                        "source": "memory",
                        "asOf": iso_now()
                    })
                else:
                    return ok({
                        "items": [],
                        "total": 0,
                        "limit": limit,
                        "offset": offset,
                        "source": "memory",
                        "asOf": iso_now()
                    })

            # 페이지네이션
            total = len(df)
            page_df = df.iloc[offset:offset + limit]

            # instant에서 name 가져오기
            instant_df = ApiConfig.instant_df
            items = []
            for idx, row in page_df.iterrows():
                name = None
                if instant_df is not None and 'ticker' in instant_df.columns:
                    ticker_data = instant_df[instant_df['ticker'] == idx]
                    if len(ticker_data) > 0:
                        name = str(ticker_data.iloc[0]['name'])
                
                items.append({
                    "ticker": idx,
                    "name": name,
                    "explanation": str(row["explanation"])
                })

            return ok({
                "items": items,
                "total": total,
                "limit": limit,
                "offset": offset,
                "source": "memory",
                "asOf": iso_now()
            })

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="memory", total=0, limit=limit, offset=offset)

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_reports_detail(self, request: HttpRequest, symbol: str):
        """
        GET /api/reports/{symbol}
        상세 리포트: 회사 프로필 + 2020년부터 전체 재무 히스토리 데이터
        - 메모리에서 instant 데이터 읽기 (S3 접근 없음!)
        """
        try:
            # 1) 메모리에서 회사 프로필 가져오기
            explanation = None
            if ApiConfig.profile_df is not None and symbol in ApiConfig.profile_df.index:
                prow = ApiConfig.profile_df.loc[symbol]
                explanation = str(prow.get("explanation", None)) if "explanation" in prow else None

            # 2) 메모리에서 instant 데이터 가져오기
            latest_data = None
            ts_price = None
            history_data = None
            
            if ApiConfig.instant_df is None:
                return degraded("Instant data not loaded in memory", source="memory")
            
            instant_df = ApiConfig.instant_df
            
            if 'ticker' in instant_df.columns and 'date' in instant_df.columns:
                # 해당 종목의 전체 히스토리 필터링 (2020년부터 전부)
                symbol_history = instant_df[instant_df['ticker'] == symbol].sort_values('date')
                
                if len(symbol_history) > 0:
                    # 최신 데이터 (재무 정보 표시용)
                    latest_row = symbol_history.iloc[-1]
                    latest_data = pd.DataFrame([latest_row])
                    ts_price = str(latest_row['date'])
                    
                    # 전체 히스토리 데이터 생성 (2020-01-01부터 전부)
                    history_data = []
                    for idx, row in symbol_history.iterrows():
                        try:
                            # 날짜를 문자열로 변환
                            date_str = str(row['date'].date()) if hasattr(row['date'], 'date') else str(row['date'])
                            
                            history_data.append({
                                "date": date_str,
                                "close": safe_float(row['close']),
                                "change": safe_float(row['change']),
                                "change_rate": safe_float(row['change_rate']),
                                "market_cap": safe_int(row['market_cap']),
                                "PER": safe_float(row['PER']),
                                "PBR": safe_float(row['PBR']),
                                "EPS": safe_float(row['EPS']),
                                "BPS": safe_float(row['BPS']),
                                "DIV": safe_float(row['DIV']),
                                "DPS": safe_float(row['DPS']),
                                "ROE": safe_float(row['ROE']),
                            })
                        except Exception as e:
                            debug_print(f"Error processing row: {e}")
                            continue

            # 최신 재무 데이터 추출 (화면 표시용)
            name = None
            market_type = None
            industry = None
            price = change = change_rate = None
            market_cap = None
            shares_outstanding = None
            
            eps = None
            bps = None
            div = None
            dps = None
            roe = None
            
            valuation = {"pe_annual": None, "pe_ttm": None, "forward_pe": None,
                        "ps_ttm": None, "pb": None, "pcf_ttm": None, "pfcf_ttm": None}
            
            dividend = {"payout_ratio": None, "yield": None, "latest_exdate": None}

            if latest_data is not None and len(latest_data) > 0:
                row = latest_data.iloc[0]

                # 기본 정보
                name = str(row["name"]) if "name" in row and row["name"] is not None else None
                market_type = str(row["market"]) if "market" in row and row["market"] is not None else None
                industry = str(row["industry"]) if "industry" in row and row["industry"] is not None else None
                
                # 가격 정보 (숫자로 변환)
                price = safe_float(row.get("close"))
                change = safe_float(row.get("change"))
                change_rate = safe_float(row.get("change_rate"))

                # 시총 (정수로 변환)
                market_cap = safe_int(row.get("market_cap"))

                # 재무 지표 (숫자로 변환)
                eps = safe_float(row.get("EPS"))
                bps = safe_float(row.get("BPS"))
                div = safe_float(row.get("DIV"))
                dps = safe_float(row.get("DPS"))
                roe = safe_float(row.get("ROE"))
                
                # 발행주식수 계산
                if market_cap and price and price > 0:
                    shares_outstanding = int(market_cap / price)

                # 밸류에이션 (숫자로 변환)
                per_val = safe_float(row.get("PER"))
                pbr_val = safe_float(row.get("PBR"))
                
                if per_val is not None:
                    valuation["pe_annual"] = per_val
                    valuation["pe_ttm"] = per_val
                if pbr_val is not None:
                    valuation["pb"] = pbr_val
                
                # 배당 수익률
                if div is not None:
                    dividend["yield"] = round(div, 2)

            # 응답
            resp = {
                "ticker": symbol,
                "name": name,
                "market": market_type,
                "industry": industry,
                
                # price 필드 (dict 형태)
                "price": {
                    "current": price,
                    "change": change,
                    "change_rate": change_rate
                } if price is not None else None,
                
                # 최신 데이터 (현재 시점)
                "current": {
                    "price": price,
                    "change": change,
                    "change_rate": change_rate,
                    "market_cap": market_cap,
                    "shares_outstanding": shares_outstanding,
                    "date": ts_price
                },
                
                # 밸류에이션
                "valuation": valuation,
                
                # 배당
                "dividend": dividend,
                
                # 재무 지표 (최신)
                "financials": {
                    "eps": eps,
                    "bps": bps,
                    "dps": dps,
                    "roe": roe,
                    "div": div
                },
                
                # 전체 히스토리 (2020-01-01부터 전부)
                "history": history_data,
                
                # 프로필
                "profile": {"symbol": symbol, "explanation": explanation} if explanation else None,
                
                # 기타
                "indicesSnippet": MOCK_INDICES if INDICES_SOURCE != "s3" else None,
                "articles": MOCK_ARTICLES.get("items", [])[:5] if ARTICLES_SOURCE != "s3" else [],
                "asOf": ts_price or iso_now(),
                "source": "memory",
            }

            return ok(resp)

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="memory")