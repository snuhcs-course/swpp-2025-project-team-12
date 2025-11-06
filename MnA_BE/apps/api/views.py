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
import pandas as pd

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
    def get_company_list(self, request):
        """
        api/company-list?limit=<int>&offset=<int>&market=kospi|kosdaq
        provide list of names of companies.
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
        market = request.GET.get("market", "kosdaq")

        try:
            source = FinanceS3Client().check_source(bucket=FINANCE_BUCKET, prefix="llm_output")
            if not source["ok"]: return JsonResponse({"message": "Data Not Found"}, status=404)
            year, month, day = source["latest"].split("-")
            if month[0] == '0': month = month[1]
        except Exception as e:
            return JsonResponse({"message": "Unexpected Server Error"}, status=500)

        try:
            df, ts = FinanceS3Client().get_latest_parquet_df(
                FINANCE_BUCKET,
                f"{S3_PREFIX_PRICE}year={year}/month={month}/market={market}"
            )

            if df is None or len(df) == 0:
                return degraded("no parquet found", source="s3", total=0, limit=limit, offset=offset)

            # 페이지네이션
            total = len(df)
            page_df = df.iloc[offset:offset + limit]

            items = [
                {"ticker": idx, "name": str(row["name"])}
                for idx, row in page_df.iterrows()
            ]

            return ok({
                "items": items,
                "total": total,
                "limit": limit,
                "offset": offset,
                "source": "s3",
                "asOf": ts or iso_now()
            })

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="s3", total=0, limit=limit, offset=offset)



    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_company_profiles(self, request: HttpRequest):
        """
        GET /api/company-profiles?limit=<int>&offset=<int>&market=kospi|kosdaq&date=YYYY-MM-DD
        회사 프로필: parquet 최신 파일에서 페이지네이션 적용
        """
        limit, offset = get_pagination(request, default_limit=10, max_limit=100)
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
                    items = [{
                        "ticker": symbol,
                        "explanation": str(row["explanation"])
                    }]
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
                {"ticker": idx, "name": str(row["name"]),  "explanation": str(row["explanation"])}
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
            debug_print(e)
            return degraded(str(e), source="s3", total=0, limit=limit, offset=offset)

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_reports_detail(self, request: HttpRequest, symbol: str):
        """
        GET /api/reports/{symbol}?days=365
        상세 리포트: 회사 프로필 + (스냅샷) 가격/지표 + 기사 요약 + 차트
        - company-profile parquet: explanation
        - price-financial-info parquet: name, close, change, change_rate, PER/PBR/market_cap 등
        - price-financial-info-instant: 차트용 히스토리 데이터
        - KOSPI + KOSDAQ 모두 조회
        """
        try:
            s3 = FinanceS3Client()
            
            # 차트 일수 파라미터 (기본 365일)
            chart_days = int(request.GET.get('days', 365))

            # 1) 회사 프로필
            profile_df, ts_prof = s3.get_latest_parquet_df(FINANCE_BUCKET, S3_PREFIX_COMPANY)
            explanation = None
            if profile_df is not None and symbol in profile_df.index:
                prow = profile_df.loc[symbol]
                explanation = str(prow.get("explanation", None)) if "explanation" in prow else None

            # 2) 가격 스냅샷 - KOSPI + KOSDAQ 둘 다 읽기
            price_df = None
            ts_price = None
            
            # 최신 날짜 찾기
            source = s3.check_source(bucket=FINANCE_BUCKET, prefix="llm_output")
            if source["ok"]:
                year, month, day = source["latest"].split("-")
                if month[0] == '0': 
                    month = month[1]
                
                df_list = []
                for market in ['kospi', 'kosdaq']:
                    try:
                        df_temp, ts_temp = s3.get_latest_parquet_df(
                            FINANCE_BUCKET,
                            f"{S3_PREFIX_PRICE}year={year}/month={month}/market={market}"
                        )
                        if df_temp is not None and len(df_temp) > 0:
                            df_list.append(df_temp)
                            if ts_price is None:
                                ts_price = ts_temp
                    except:
                        continue
                
                if df_list:
                    price_df = pd.concat(df_list)

            # 3) 차트 데이터 - price-financial-info-instant에서 로드
            chart_data = None
            try:
                instant_df, _ = s3.get_latest_parquet_df(
                    FINANCE_BUCKET,
                    'price-financial-info-instant/'
                )
                
                if instant_df is not None and 'ticker' in instant_df.columns and 'date' in instant_df.columns:
                    # 해당 종목 필터링 및 정렬
                    symbol_history = instant_df[instant_df['ticker'] == symbol].sort_values('date')
                    
                    # 최근 N일만
                    symbol_history = symbol_history.tail(chart_days)
                    
                    if len(symbol_history) > 0:
                        # 차트 형식으로 변환
                        chart_data = []
                        for idx, row in symbol_history.iterrows():
                            try:
                                timestamp = int(pd.to_datetime(row['date']).timestamp() * 1000)
                                price_val = float(row['close'])
                                chart_data.append({
                                    "t": timestamp,
                                    "v": price_val
                                })
                            except:
                                continue
            except Exception as e:
                debug_print(f"Chart data error: {e}")
                chart_data = None

            # 데이터 추출
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

            if price_df is not None and symbol in price_df.index:
                row = price_df.loc[symbol]

                # 기본 정보
                if "name" in row and row["name"] is not None:
                    name = str(row["name"])
                
                if "market" in row and row["market"] is not None:
                    market_type = str(row["market"])
                
                if "industry" in row and row["industry"] is not None:
                    industry = str(row["industry"])
                
                if "close" in row and row["close"] is not None:
                    try: 
                        price = float(row["close"])
                    except: 
                        pass
                if "change" in row and row["change"] is not None:
                    try: 
                        change = float(row["change"])
                    except: 
                        pass
                if "change_rate" in row and row["change_rate"] is not None:
                    try: 
                        change_rate = float(row["change_rate"])
                    except: 
                        pass

                # 시총
                if "market_cap" in row and row["market_cap"] is not None:
                    try:
                        market_cap = str(int(row["market_cap"]))
                    except:
                        market_cap = str(row["market_cap"])

                # EPS
                if "EPS" in row and row["EPS"] is not None:
                    try:
                        eps = float(row["EPS"])
                    except:
                        pass
                
                # BPS
                if "BPS" in row and row["BPS"] is not None:
                    try:
                        bps = float(row["BPS"])
                    except:
                        pass
                
                # DIV
                if "DIV" in row and row["DIV"] is not None:
                    try:
                        div = float(row["DIV"])
                        dividend["yield"] = f"{div:.2f}%"
                    except:
                        pass
                
                # DPS
                if "DPS" in row and row["DPS"] is not None:
                    try:
                        dps = float(row["DPS"])
                    except:
                        pass
                
                # ROE
                if "ROE" in row and row["ROE"] is not None:
                    try:
                        roe = float(row["ROE"]) * 100
                    except:
                        pass
                
                # 발행주식수 계산
                if market_cap and price and price > 0:
                    try:
                        shares_outstanding = str(int(float(market_cap) / price))
                    except:
                        pass

                # 밸류에이션
                if "PER" in row and row["PER"] is not None:
                    try:
                        per_val = str(float(row["PER"]))
                        valuation["pe_annual"] = per_val
                        valuation["pe_ttm"] = per_val
                    except:
                        pass
                if "PBR" in row and row["PBR"] is not None:
                    try:
                        valuation["pb"] = str(float(row["PBR"]))
                    except:
                        pass

            # 4) 응답
            resp = {
                "ticker": symbol,
                "name": name,
                "market": market_type,
                "industry": industry,
                "price": price,
                "change": change,
                "change_rate": change_rate,
                "market_cap": market_cap,
                "shares_outstanding": shares_outstanding,
                "valuation": valuation,
                "dividend": dividend,
                "financials": {
                    "eps": eps,
                    "bps": bps,
                    "dps": dps,
                    "roe": roe,
                    "div": div
                },
                "chart": chart_data,  # ← 차트 데이터 추가!
                "profile": {"symbol": symbol, "explanation": explanation} if explanation else None,
                "indicesSnippet": MOCK_INDICES if INDICES_SOURCE != "s3" else None,
                "articles": MOCK_ARTICLES.get("items", [])[:5] if ARTICLES_SOURCE != "s3" else [],
                "asOf": ts_price or ts_prof or iso_now(),
                "source": "s3" if (price is not None or name or explanation) else "empty",
            }

            return ok(resp)

        except Exception as e:
            debug_print(e)
            return degraded(str(e), source="s3")
        
