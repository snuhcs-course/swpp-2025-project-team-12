# apps/api/views.py
from django.http import HttpRequest
from django.core.cache import cache
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceBucket
from Mocks.mock_data import MOCK_INDICES, MOCK_ARTICLES
from decorators import default_error_handler
from utils.debug_print import debug_print
from utils.pagination import get_pagination
from utils.get_llm_overview import get_latest_overview
from utils.for_api import *
from utils.store import store
from utils import instant_data
from apps.api.constants import *
import json
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

        s3 = FinanceBucket()

        company_profile_head = s3.check_source(S3_PREFIX_COMPANY)
        price_financial_head = s3.check_source(S3_PREFIX_PRICE)

        s3_status = {
            "ok": True,
            "latest": {
                "companyProfile": company_profile_head["latest"],
                "priceFinancial": price_financial_head["latest"]
            }
        }

        # DB 체크는 간단히 (실제로는 DB 쿼리 시도)
        db_status = {"ok": True}
        
        # 캐시 상태 확인
        instant_df = store.get_data('instant_df')
        profile_df = store.get_data('profile_df')
        last_loaded = cache.get('data_last_loaded')

        cache_status = {
            "instant_loaded": instant_df is not None,
            "profile_loaded": profile_df is not None,
            "last_loaded": str(last_loaded) if last_loaded else None
        }

        return ok({
            "api": "ok",
            "s3": s3_status,
            "db": db_status,
            "cache": cache_status,
            "asOf": iso_now()
        })

    @action(detail=False, methods=['post'])
    @default_error_handler
    def reload_data(self, request: HttpRequest):
        """
        POST /api/reload-data
        관리자가 수동으로 instant/profile 데이터를 리로드
        서버 재시작 없이 최신 데이터로 업데이트
        """
        try:
            return instant_data.reload()
        except Exception as e:
            debug_print(f"✗ Error reloading data: {e}")
            import traceback
            debug_print(traceback.format_exc())
            return degraded(str(e), source="reload")

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_indices(self, request: HttpRequest):
        """
        GET /api/indices
        지수: S3에서 최신 KOSPI/KOSDAQ 지수 읽기
        """
        if INDICES_SOURCE == "s3":
            try:
                # S3 클라이언트 생성
                s3 = FinanceBucket()

                # 최신 날짜의 지수 파일 찾기
                response = s3.get_list_v2(S3_PREFIX_INDICES)

                if 'Contents' not in response:
                    return degraded(
                        "No indices data in S3",
                        source="s3",
                        kospi=MOCK_INDICES.get("kospi", {"value": 2500, "changePct": 0}),
                        kosdaq=MOCK_INDICES.get("kosdaq", {"value": 750, "changePct": 0})
                    )

                # 최신 파일 찾기
                files = sorted(response['Contents'], key=lambda x: x['LastModified'], reverse=True)

                # KOSPI와 KOSDAQ 최신 파일 찾기
                kospi_file = None
                kosdaq_file = None

                for f in files:
                    if 'KOSPI.json' in f['Key'] and kospi_file is None:
                        kospi_file = f
                    if 'KOSDAQ.json' in f['Key'] and kosdaq_file is None:
                        kosdaq_file = f
                    if kospi_file and kosdaq_file:
                        break

                # 데이터 읽기
                kospi_data = {}
                kosdaq_data = {}
                as_of = None

                if kospi_file:
                    data = s3.get_json(kospi_file['Key'])
                    kospi_data = {
                        "value": round(data.get('close', 0), 2),
                        "changePct": round(data.get('change_percent', 0), 2)
                    }
                    as_of = data.get('fetched_at') or str(kospi_file['LastModified'])

                if kosdaq_file:
                    data = s3.get_json(kosdaq_file['Key'])
                    kosdaq_data = {
                        "value": round(data.get('close', 0), 2),
                        "changePct": round(data.get('change_percent', 0), 2)
                    }
                    if not as_of:
                        as_of = data.get('fetched_at') or str(kosdaq_file['LastModified'])

                return ok({
                    "kospi": kospi_data,
                    "kosdaq": kosdaq_data,
                    "asOf": as_of,
                    "source": "s3"
                })

            except Exception as e:
                debug_print(f"Error fetching indices from S3: {e}")
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
        캐시에서 읽기!
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        market = request.GET.get("market", "kosdaq")

        # 대소문자 처리: 소문자 입력도 허용
        if market:
            market = market.upper()

        try:
            # 캐시에서 instant 데이터 가져오기
            df = store.get_data('instant_df')
            if df is None:
                return degraded("Data not loaded in cache", source="cache", total=0, limit=limit, offset=offset)

            # # 최신 날짜만 필터링
            latest_date = df['date'].max()
            df_latest = df[df['date'] == latest_date]

            # 시장 필터링 (market은 이미 대문자로 변환됨)
            if market and 'market' in df_latest.columns:
                df_latest = df_latest[df_latest['market'] == market]

            # 페이지네이션
            total = len(df_latest)
            page_df = df_latest.iloc[offset:offset + limit]

            company_overview = get_latest_overview("company-overview")

            items = [
                {
                    "ticker": row["ticker"],
                    "name": str(row["name"]),
                    "close": row["close"],
                    "change": row["change"],
                    "change_rate": row["change_rate"],
                    "summary":
                        json.loads(company_overview.get(row["ticker"], "{}")).get("summary", None)
                }
                for idx, row in page_df.iterrows()
            ]

            return ok({
                "items": items,
                "total": total,
                "limit": limit,
                "offset": offset,
                "source": "cache",
                "asOf": str(latest_date) if latest_date else iso_now()
            })

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="cache", total=0, limit=limit, offset=offset)


    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_profiles(self, request: HttpRequest):
        """
        GET /api/company-profiles?limit=<int>&offset=<int>&symbol=<str>
        회사 프로필: 캐시에서 읽기
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        symbol = request.GET.get("symbol")

        try:
            # 캐시에서 profile 데이터 가져오기
            df = store.get_data('profile_df')
            if df is None:
                return degraded("Profile data not loaded in cache", source="cache", total=0, limit=limit, offset=offset)

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
                        "source": "cache",
                        "asOf": iso_now()
                    })
                else:
                    return ok({
                        "items": [],
                        "total": 0,
                        "limit": limit,
                        "offset": offset,
                        "source": "cache",
                        "asOf": iso_now()
                    })

            # 페이지네이션
            total = len(df)
            page_df = df.iloc[offset:offset + limit]

            # instant에서 name 가져오기
            instant_df = store.get_data('instant_df')
            items = []
            for idx, row in page_df.iterrows():
                # comment getting name part (speed issue)
                # name = None
                # if instant_df is not None and 'ticker' in instant_df.columns:
                #     ticker_data = instant_df[instant_df['ticker'] == idx]
                #     if len(ticker_data) > 0:
                #         name = str(ticker_data.iloc[0]['name'])

                items.append({
                    "ticker": idx,
                    "name": "hello",
                    # "explanation": str(row["explanation"])
                })


            return ok({
                "items": items,
                "total": total,
                "limit": limit,
                "offset": offset,
                "source": "cache",
                "asOf": iso_now()
            })

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="cache", total=0, limit=limit, offset=offset)

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_overview(self, request, ticker:str):
        """
        GET /api/overview/{ticker}
        """
        try:
            company_overview = get_latest_overview("company-overview")
        except Exception as e:
            return JsonResponse({ "message": "Unexpected Server Error" }, status=500)

        return JsonResponse(json.loads(company_overview.get(ticker, "{}")), status=200, safe=False)


    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_reports_detail(self, request: HttpRequest, symbol: str):
        """
        GET /api/reports/{symbol}
        상세 리포트: 회사 프로필 + 2020년부터 전체 재무 히스토리 데이터
        - 캐시에서 instant 데이터 읽기
        - 실시간 지수 정보 포함
        """
        try:
            # 1) 캐시에서 회사 프로필 가져오기
            profile_df = store.get_data('profile_df')
            explanation = None
            if profile_df is not None and symbol in profile_df.index:
                prow = profile_df.loc[symbol]
                explanation = str(prow.get("explanation", None)) if "explanation" in prow else None

            # 2) 캐시에서 instant 데이터 가져오기
            instant_df = store.get_data('instant_df')
            if instant_df is None:
                return degraded("Instant data not loaded in cache", source="cache")

            latest_data = None
            ts_price = None
            history_data = None

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

            # 3) 실시간 지수 정보 가져오기 (S3에서)
            indices_snippet = None
            if INDICES_SOURCE == "s3":
                try:
                    s3 = FinanceBucket()
                    response = s3.get_list_v2(S3_PREFIX_INDICES)

                    if 'Contents' in response:
                        files = sorted(response['Contents'], key=lambda x: x['LastModified'], reverse=True)

                        kospi_file = None
                        kosdaq_file = None

                        for f in files:
                            if 'KOSPI.json' in f['Key'] and kospi_file is None:
                                kospi_file = f
                            if 'KOSDAQ.json' in f['Key'] and kosdaq_file is None:
                                kosdaq_file = f
                            if kospi_file and kosdaq_file:
                                break

                        indices_snippet = {}

                        if kospi_file:
                            data = s3.get_json(kospi_file['Key'])
                            indices_snippet['kospi'] = {
                                "value": round(data.get('close', 0), 2),
                                "changePct": round(data.get('change_percent', 0), 2)
                            }

                        if kosdaq_file:
                            data = s3.get_json(kosdaq_file['Key'])
                            indices_snippet['kosdaq'] = {
                                "value": round(data.get('close', 0), 2),
                                "changePct": round(data.get('change_percent', 0), 2)
                            }
                except Exception as e:
                    debug_print(f"Error fetching indices: {e}")
                    indices_snippet = MOCK_INDICES if INDICES_SOURCE != "s3" else None
            else:
                indices_snippet = MOCK_INDICES

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
                
                # 실시간 지수 정보
                "indicesSnippet": indices_snippet,

                # 뉴스는 제외
                "articles": [],

                "asOf": ts_price or iso_now(),
                "source": "cache",
            }

            return ok(resp)

        except Exception as e:
            debug_print(e)
            import traceback
            debug_print(traceback.format_exc())
            return degraded(str(e), source="cache")