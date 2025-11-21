# apps/api/views.py
from django.http import HttpRequest, JsonResponse
from django.core.cache import cache
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
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


# ============================================================================
# Serializers
# ============================================================================

class HealthS3Serializer(serializers.Serializer):
    ok = serializers.BooleanField()
    latest = serializers.DictField()

class HealthCacheSerializer(serializers.Serializer):
    instant_loaded = serializers.BooleanField()
    profile_loaded = serializers.BooleanField()
    last_loaded = serializers.CharField(allow_null=True)

class HealthResponseSerializer(serializers.Serializer):
    api = serializers.CharField()
    s3 = HealthS3Serializer()
    db = serializers.DictField()
    cache = HealthCacheSerializer()
    asOf = serializers.CharField()


class IndicesDataSerializer(serializers.Serializer):
    value = serializers.FloatField()
    changePct = serializers.FloatField()

class IndicesResponseSerializer(serializers.Serializer):
    kospi = IndicesDataSerializer()
    kosdaq = IndicesDataSerializer()
    asOf = serializers.CharField()
    source = serializers.CharField()


class CompanyOverviewSerializer(serializers.Serializer):
    asof_date = serializers.CharField()
    fundamental_analysis = serializers.CharField()
    technical_analysis = serializers.CharField()
    label = serializers.CharField()
    confidence = serializers.FloatField()
    summary = serializers.CharField()
    news = serializers.ListField(child=serializers.CharField())

class CompanyItemSerializer(serializers.Serializer):
    ticker = serializers.CharField()
    name = serializers.CharField()
    overview = CompanyOverviewSerializer()

class CompanyListResponseSerializer(serializers.Serializer):
    items = CompanyItemSerializer(many=True)
    total = serializers.IntegerField()
    limit = serializers.IntegerField()
    offset = serializers.IntegerField()
    source = serializers.CharField()
    asOf = serializers.CharField()


class CompanyProfileItemSerializer(serializers.Serializer):
    ticker = serializers.CharField()
    name = serializers.CharField()
    explanation = serializers.CharField(required=False)

class CompanyProfileResponseSerializer(serializers.Serializer):
    items = CompanyProfileItemSerializer(many=True)
    total = serializers.IntegerField()
    limit = serializers.IntegerField()
    offset = serializers.IntegerField()
    source = serializers.CharField()
    asOf = serializers.CharField()


class ReportPriceSerializer(serializers.Serializer):
    current = serializers.FloatField(allow_null=True)
    change = serializers.FloatField(allow_null=True)
    change_rate = serializers.FloatField(allow_null=True)

class ReportCurrentSerializer(serializers.Serializer):
    price = serializers.FloatField(allow_null=True)
    change = serializers.FloatField(allow_null=True)
    change_rate = serializers.FloatField(allow_null=True)
    market_cap = serializers.IntegerField(allow_null=True)
    shares_outstanding = serializers.IntegerField(allow_null=True)
    date = serializers.CharField(allow_null=True)

class ReportValuationSerializer(serializers.Serializer):
    pe_annual = serializers.FloatField(allow_null=True)
    pe_ttm = serializers.FloatField(allow_null=True)
    forward_pe = serializers.FloatField(allow_null=True)
    ps_ttm = serializers.FloatField(allow_null=True)
    pb = serializers.FloatField(allow_null=True)
    pcf_ttm = serializers.FloatField(allow_null=True)
    pfcf_ttm = serializers.FloatField(allow_null=True)

class ReportDividendSerializer(serializers.Serializer):
    payout_ratio = serializers.FloatField(allow_null=True)
    yield_field = serializers.FloatField(allow_null=True, source='yield')
    latest_exdate = serializers.CharField(allow_null=True)

class ReportFinancialsSerializer(serializers.Serializer):
    eps = serializers.FloatField(allow_null=True)
    bps = serializers.FloatField(allow_null=True)
    dps = serializers.FloatField(allow_null=True)
    roe = serializers.FloatField(allow_null=True)
    div = serializers.FloatField(allow_null=True)

class ReportHistoryItemSerializer(serializers.Serializer):
    date = serializers.CharField()
    close = serializers.FloatField(allow_null=True)
    change = serializers.FloatField(allow_null=True)
    change_rate = serializers.FloatField(allow_null=True)
    market_cap = serializers.IntegerField(allow_null=True)
    PER = serializers.FloatField(allow_null=True)
    PBR = serializers.FloatField(allow_null=True)
    EPS = serializers.FloatField(allow_null=True)
    BPS = serializers.FloatField(allow_null=True)
    DIV = serializers.FloatField(allow_null=True)
    DPS = serializers.FloatField(allow_null=True)
    ROE = serializers.FloatField(allow_null=True)

class ReportProfileSerializer(serializers.Serializer):
    symbol = serializers.CharField()
    explanation = serializers.CharField(allow_null=True)

class ReportResponseSerializer(serializers.Serializer):
    ticker = serializers.CharField()
    name = serializers.CharField(allow_null=True)
    market = serializers.CharField(allow_null=True)
    industry = serializers.CharField(allow_null=True)
    price = ReportPriceSerializer(allow_null=True)
    current = ReportCurrentSerializer()
    valuation = ReportValuationSerializer()
    dividend = ReportDividendSerializer()
    financials = ReportFinancialsSerializer()
    history = ReportHistoryItemSerializer(many=True)
    profile = ReportProfileSerializer(allow_null=True)
    indicesSnippet = serializers.DictField(allow_null=True)
    articles = serializers.ListField()
    asOf = serializers.CharField()
    source = serializers.CharField()


# ============================================================================
# Views
# ============================================================================

class APIView(viewsets.ViewSet):
    """
    Stock API Views - Company data and system health
    """

    @swagger_auto_schema(
        operation_description="Check API health status, data freshness, and cache status",
        responses={200: HealthResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_health(self, request: HttpRequest):
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

        db_status = {"ok": True}
        
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

    @swagger_auto_schema(
        operation_description="Manually reload data from S3 into memory cache (admin function)",
        responses={
            200: openapi.Response(
                description="Success or degraded mode",
                examples={
                    "application/json": {
                        "status": "success",
                        "message": "Data reloaded successfully",
                        "instant_loaded": True,
                        "profile_loaded": True
                    }
                }
            )
        }
    )
    @action(detail=False, methods=['post'])
    @default_error_handler
    def reload_data(self, request: HttpRequest):
        try:
            return instant_data.reload()
        except Exception as e:
            debug_print(f"✗ Error reloading data: {e}")
            import traceback
            debug_print(traceback.format_exc())
            return degraded(str(e), source="reload")

    @swagger_auto_schema(
        operation_description="Get current KOSPI and KOSDAQ index values from S3",
        responses={200: IndicesResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_indices(self, request: HttpRequest):
        if INDICES_SOURCE == "s3":
            try:
                s3 = FinanceBucket()
                response = s3.get_list_v2(S3_PREFIX_INDICES)

                if 'Contents' not in response:
                    return degraded(
                        "No indices data in S3",
                        source="s3",
                        kospi=MOCK_INDICES.get("kospi", {"value": 2500, "changePct": 0}),
                        kosdaq=MOCK_INDICES.get("kosdaq", {"value": 750, "changePct": 0})
                    )

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

        return ok(MOCK_INDICES)

    @swagger_auto_schema(
        operation_description="Get list of companies with AI-generated investment analysis",
        manual_parameters=[
            openapi.Parameter('limit', openapi.IN_QUERY, description="Number of items (default: 10, max: 100)", type=openapi.TYPE_INTEGER),
            openapi.Parameter('offset', openapi.IN_QUERY, description="Pagination offset (default: 0)", type=openapi.TYPE_INTEGER),
            openapi.Parameter('market', openapi.IN_QUERY, description='Market filter: "kospi" or "kosdaq"', type=openapi.TYPE_STRING),
        ],
        responses={200: CompanyListResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_list(self, request):
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        market = request.GET.get("market", None)

        if market:
            market = market.upper()

        try:
            df = store.get_data('instant_df')
            if df is None:
                return degraded("Data not loaded in cache", source="cache", total=0, limit=limit, offset=offset)

            latest_date = df['date'].max()
            df_latest = df[df['date'] == latest_date]

            # if market is set, filter by market
            if market and 'market' in df_latest.columns:
                df_latest = df_latest[df_latest['market'] == market]

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

    @swagger_auto_schema(
        operation_description="Get company profile descriptions from cache",
        manual_parameters=[
            openapi.Parameter('limit', openapi.IN_QUERY, description="Number of items (default: 10, max: 100)", type=openapi.TYPE_INTEGER),
            openapi.Parameter('offset', openapi.IN_QUERY, description="Pagination offset (default: 0)", type=openapi.TYPE_INTEGER),
            openapi.Parameter('symbol', openapi.IN_QUERY, description="Specific ticker to retrieve", type=openapi.TYPE_STRING),
        ],
        responses={200: CompanyProfileResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_profiles(self, request: HttpRequest):
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        symbol = request.GET.get("symbol")

        try:
            df = store.get_data('profile_df')
            if df is None:
                return degraded("Profile data not loaded in cache", source="cache", total=0, limit=limit, offset=offset)

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

            total = len(df)
            page_df = df.iloc[offset:offset + limit]

            items = []
            for idx, row in page_df.iterrows():
                items.append({
                    "ticker": idx,
                    "name": "hello",
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

    @swagger_auto_schema(
        operation_description="Get AI-generated investment analysis for a specific stock",
        manual_parameters=[
            openapi.Parameter('ticker', openapi.IN_PATH, description="Stock ticker code (e.g., 005930)", type=openapi.TYPE_STRING, required=True),
        ],
        responses={
            200: CompanyOverviewSerializer(),
            500: openapi.Response(description="Server error")
        }
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_overview(self, request, ticker:str):
        try:
            company_overview = get_latest_overview("company-overview")
        except Exception as e:
            return JsonResponse({ "message": "Unexpected Server Error" }, status=500)

        return JsonResponse(json.loads(company_overview.get(ticker, "{}")), status=200, safe=False)

    @swagger_auto_schema(
        operation_description="Get comprehensive stock report with complete historical data since 2020",
        manual_parameters=[
            openapi.Parameter('symbol', openapi.IN_PATH, description="Stock ticker code (e.g., 005930)", type=openapi.TYPE_STRING, required=True),
        ],
        responses={200: ReportResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_reports_detail(self, request: HttpRequest, symbol: str):
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
                symbol_history = instant_df[instant_df['ticker'] == symbol].sort_values('date')
                
                if len(symbol_history) > 0:
                    latest_row = symbol_history.iloc[-1]
                    latest_data = pd.DataFrame([latest_row])
                    ts_price = str(latest_row['date'])
                    
                    history_data = []
                    for idx, row in symbol_history.iterrows():
                        try:
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

            # 최신 재무 데이터 추출
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

                name = str(row["name"]) if "name" in row and row["name"] is not None else None
                market_type = str(row["market"]) if "market" in row and row["market"] is not None else None
                industry = str(row["industry"]) if "industry" in row and row["industry"] is not None else None
                
                price = safe_float(row.get("close"))
                change = safe_float(row.get("change"))
                change_rate = safe_float(row.get("change_rate"))

                market_cap = safe_int(row.get("market_cap"))

                eps = safe_float(row.get("EPS"))
                bps = safe_float(row.get("BPS"))
                div = safe_float(row.get("DIV"))
                dps = safe_float(row.get("DPS"))
                roe = safe_float(row.get("ROE"))
                
                if market_cap and price and price > 0:
                    shares_outstanding = int(market_cap / price)

                per_val = safe_float(row.get("PER"))
                pbr_val = safe_float(row.get("PBR"))
                
                if per_val is not None:
                    valuation["pe_annual"] = per_val
                    valuation["pe_ttm"] = per_val
                if pbr_val is not None:
                    valuation["pb"] = pbr_val
                
                if div is not None:
                    dividend["yield"] = round(div, 2)

            # 3) 실시간 지수 정보 가져오기
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

            resp = {
                "ticker": symbol,
                "name": name,
                "market": market_type,
                "industry": industry,
                
                "price": {
                    "current": price,
                    "change": change,
                    "change_rate": change_rate
                } if price is not None else None,

                "current": {
                    "price": price,
                    "change": change,
                    "change_rate": change_rate,
                    "market_cap": market_cap,
                    "shares_outstanding": shares_outstanding,
                    "date": ts_price
                },
                
                "valuation": valuation,
                "dividend": dividend,
                
                "financials": {
                    "eps": eps,
                    "bps": bps,
                    "dps": dps,
                    "roe": roe,
                    "div": div
                },
                
                "history": history_data,
                "profile": {"symbol": symbol, "explanation": explanation} if explanation else None,
                "indicesSnippet": indices_snippet,
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