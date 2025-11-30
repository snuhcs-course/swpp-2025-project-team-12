import warnings
warnings.filterwarnings("ignore", category=UserWarning)

from datetime import datetime
from zoneinfo import ZoneInfo
from concurrent.futures import ThreadPoolExecutor, as_completed
from itertools import product
from typing import List
import json
import sys
import time

import boto3
import numpy as np
import pandas as pd
import pandas_market_calendars as mcal
import pytz
import yfinance as yf
from dotenv import load_dotenv
from langchain_chroma import Chroma
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from pydantic import BaseModel, Field

load_dotenv()

# functions for get trading day

def is_trading_day_krx():
    d = datetime.now(ZoneInfo("Asia/Seoul"))
    krx = mcal.get_calendar("XKRX")
    ds = d.strftime("%Y-%m-%d")
    schedule = krx.schedule(start_date=ds, end_date=ds)
    return not schedule.empty

def get_recent_closed_trading_day():
    d = datetime.now(ZoneInfo("Asia/Seoul"))
    krx = mcal.get_calendar("XKRX")
    
    start_date = '2020-01-01'
    end_date = d.strftime("%Y-%m-%d")
    schedule = krx.schedule(start_date=start_date, end_date=end_date)
    
    past_trading_days = schedule[schedule["market_close"] < d].index
    if not past_trading_days.empty:
        return past_trading_days[-1].strftime("%Y-%m-%d")
    else:
        return None

def get_today_date():
    KST = pytz.timezone('Asia/Seoul')
    now = datetime.now(KST)
    return now.strftime("%Y-%m-%d")

def first_trading_day_krx(date_str):
    d = datetime.strptime(date_str, "%Y-%m-%d")
    krx = mcal.get_calendar("XKRX")
    start_date = d.replace(day=1).strftime("%Y-%m-%d")
    if d.month == 12:
        end_date = d.replace(year=d.year + 1, month=1, day=1)
    else:
        end_date = d.replace(month=d.month + 1, day=1)
    end_date = (end_date).strftime("%Y-%m-%d")
    schedule = krx.schedule(start_date=start_date, end_date=end_date)
    if schedule.empty:
        return None
    first_day = schedule.index[0].strftime("%Y-%m-%d")
    return first_day

# functions for fetch data

def get_index_feature(df, market, one_m: int = 21, three_m: int = 63):
    close = df["Close"].astype(float)
    high, low = df["High"], df["Low"]
    ret_d = close.pct_change()

    def _rsi(close: pd.Series, period: int = 14) -> pd.Series:
        delta = close.diff()
        gain = delta.clip(lower=0)
        loss = -delta.clip(upper=0)
        avg_gain = gain.ewm(alpha=1/period, adjust=False).mean()
        avg_loss = loss.ewm(alpha=1/period, adjust=False).mean()
        rs = avg_gain / (avg_loss.replace(0, np.nan))
        return 100 - (100 / (1 + rs))

    def _annualized_vol(ret: pd.Series, window: int = 20, trading_days: int = 252):
        return ret.rolling(window).std() * np.sqrt(trading_days) * 100

    def _max_drawdown(close: pd.Series, window: int = 252):
        roll_max = close.rolling(window, min_periods=1).max()
        dd = close / roll_max - 1.0
        mdd = dd.rolling(window, min_periods=1).min() * 100
        return mdd

    out = {
        f"date": close.index.strftime('%Y-%m-%d'),
        f"market": market,

        f"rsi_{one_m}d": _rsi(close, one_m),
        f"rsi_{three_m}d": _rsi(close, three_m),

        f"momentum_{one_m}d_pct": close.pct_change(one_m) * 100,
        f"momentum_{three_m}d_pct": close.pct_change(three_m) * 100,

        f"vol_ann_{one_m}d_pct": _annualized_vol(ret_d, one_m),
        f"vol_ann_{three_m}d_pct": _annualized_vol(ret_d, three_m),

        f"mdd_{one_m}d_pct": _max_drawdown(close, one_m),
        f"mdd_{three_m}d_pct": _max_drawdown(close, three_m),

        "recent_close_ret_pct": ret_d * 100,
        "recent_high_low_diff_pct_of_close": (high - low) / close * 100,
    }
    return pd.DataFrame(out).round(4).dropna().reset_index(drop=True)

def get_kospi_json(date):
    kospi = yf.Ticker("^KS11")
    kospi_index = kospi.history(period="2y")[:date]
    kospi_features = get_index_feature(kospi_index, 'KOSPI').iloc[-5:]
    print('fetch kospi until', kospi_features['date'].iloc[-1])
    return json.loads(kospi_features.to_json(orient='records', force_ascii=False))

def get_kosdaq_json(date):
    kosdaq = yf.Ticker("^KQ11")
    kosdaq_index = kosdaq.history(period="2y")[:date]
    kosdaq_features = get_index_feature(kosdaq_index, 'KOSDAQ').iloc[-5:]
    print('fetch kosdaq until', kosdaq_features['date'].iloc[-1])
    return json.loads(kosdaq_features.to_json(orient='records', force_ascii=False))

def get_news_json(date):
    year = int(date.split('-')[0])
    month = int(date.split('-')[1])
    day = int(date.split('-')[2])

    s3 = boto3.client('s3')
    path = f"news-articles/year={year}/month={month}/day={day}/multi_section_top100.json"

    for attempt in range(6):  
        print(f'fetch news on {date} (try {attempt+1}/6)')
        try:
            response = s3.get_object(Bucket='swpp-12-bucket', Key=path)
            data = json.load(response['Body'])['articles']
            if len(data) == 0:
                raise ValueError("no articles in files")
            print(f'found {len(data)} articles')
            return json.loads(pd.DataFrame(data)['title'].to_json(orient='records', force_ascii=False))
        except Exception as e:
            print(f'no article ({e})')
            if attempt < 5:
                print("waiting 5 minutes before retry...")
                time.sleep(300)
    print("no article after 6 retries, returning 0")
    sys.exit(0)

def get_stock_info_df(date):
    year = date.split('-')[0]
    month = date.split('-')[1]
    first_month_td = first_trading_day_krx(date)
    print(f'fetch stock info on {date}')
    print(f'fetch company profile on {first_month_td}')

    path = f"s3://swpp-12-bucket/price-financial-info-instant/year={year}/month={month}/{date}.parquet"

    kospi_profile_path = f"s3://swpp-12-bucket/company-profile/year={year}/month={month}/market=kospi/{first_month_td}.parquet"
    kosdaq_profile_path = f"s3://swpp-12-bucket/company-profile/year={year}/month={month}/market=kosdaq/{first_month_td}.parquet"

    all_info = pd.read_parquet(path, engine="pyarrow")
    all_info['ROE'] = round(all_info['ROE'].astype(float), 4).astype(str)
    all_info['date'] = all_info['date'].astype(str)
    kospi_profile = pd.read_parquet(kospi_profile_path, engine="pyarrow")
    kosdaq_profile = pd.read_parquet(kosdaq_profile_path, engine="pyarrow")
    all_profile = pd.concat([kospi_profile, kosdaq_profile])

    return all_info, all_profile

def get_company_json(tmp_info, tmp_profile):
    tmp_info = tmp_info.copy()
    tmp_info.index = tmp_info.index.get_level_values(0)
    tmp_info = tmp_info.sort_index()
    cols = ['date', 'close','market_cap','BPS','PER','PBR','EPS','DIV','DPS','ROE']
    info1 = tmp_info[['ticker','name','market','industry']].iloc[-1].to_dict()
    info2 = tmp_info[cols].resample('QS').first().to_dict(orient='records')
    info3 = tmp_info[cols].tail(5).to_dict(orient='records')
    return {"기본정보": info1, "회사설명": tmp_profile, "주가_및_재무": {"장기": info2, "단기": info3}}

# llm call 1

def hyde_generate_for_market(kospi_json: str, kosdaq_json: str, news_json: str) -> str:
    prompt = f"""
    너는 한국 주식시장 애널리스트이자 투자 서적 요약가입니다.

    아래 세 가지 정보를 참고하세요:
    1) kospi_json: 현재 KOSPI 관련 기술적 지표 및 요약
    2) kosdaq_json: 현재 KOSDAQ 관련 기술적 지표 및 요약
    3) news_json: 오늘 시장 관련 주요 뉴스 헤드라인 및 요약

    이 정보를 바탕으로,
    - 전체 시장이 리스크 온/리스크 오프 중 어느 쪽에 가까운지,
    - 변동성 수준,
    - 인덱스 투자자(John Bogle 스타일)가 이 국면에서 어떻게 행동해야 할지,
    - 단기 트레이더(Mark Minervini 스타일)가 어떤 점을 주로 볼지,
    - 장기 성장주 투자자(Peter Lynch 스타일)가 어떤 심리/행동을 보일지,
    - 팩터/퀀트 관점(What Works on Wall Street)이 시사하는 점

    을 2~4단락 정도의 해설 텍스트로 한국어로 작성하세요.
    실제 책 내용을 그대로 인용하지 말고, 책들의 철학을 요약해서 설명하듯이 쓰세요.

    [kospi_json]
    {kospi_json}

    [kosdaq_json]
    {kosdaq_json}

    [news_json]
    {news_json}
    """
    res = hyde_llm.invoke(prompt.strip())
    return res.content

def build_books_context_for_market(kospi_json: str, kosdaq_json: str, news_json: str) -> str:
    hypothetical_doc = hyde_generate_for_market(
        kospi_json=kospi_json,
        kosdaq_json=kosdaq_json,
        news_json=news_json,
    )

    query_embedding = embeddings.embed_query(hypothetical_doc)

    docs = book_vectordb.similarity_search_by_vector(query_embedding, k=5)

    ctx = ""
    for i, d in enumerate(docs, start=1):
        source = d.metadata.get("source", "unknown")
        page = d.metadata.get("page", "N/A")
        ctx += f"\n[Book {i} | {source} | page {page}]\n{d.page_content}\n"
    return ctx.strip()

def llm_call_1_rag(kospi_json: str, kosdaq_json: str, news_json: str) -> str:
    books_context = build_books_context_for_market(
        kospi_json=kospi_json,
        kosdaq_json=kosdaq_json,
        news_json=news_json,
    )
    class IndexSentiment(BaseModel):
        market: str = Field(description="KOSPI 또는 KOSDAQ")
        label: str = Field(description="상승 | 하락 | 중립")
        confidence: float = Field(description="0~1")
        summary: str = Field(
            description=(
                "해당 지수의 상태를 문단 형태로 기술. "
                "2~4문장 이내. RSI, 모멘텀, 변동성, 관련뉴스 등 주요 포인트를 자연스럽게 포함. 입니다 체"
            )
        )

    class MarketSentimentReport(BaseModel):
        asof_date: str = Field(description="해당 평가 기준 일자 (예: 2025-10-16)")
        kospi: IndexSentiment
        kosdaq: IndexSentiment
        basic_overview: str = Field(
            description=(
                "코스피와 코스닥 지수의 상태를 포함한 전반적인 시장 상태를 문단 평태로 기술. "
                "2~4문장 이내. 입니다 체"
            )
        )
        news_overview: str = Field(
            description=(
                "핵심적인 뉴스 정보를 포함한 전반적인 시장 상태를 문단 형태로 기술. "
                "2~4문장 이내. 입니다 체"
            )
        )

    template = """
    당신은 한국 주식시장(KOSPI/KOSDAQ) 지수 분석가입니다.
    입력(kospi_json, kosdaq_json, news_json)과 투자 관련 서적 발췌(books_context)를 바탕으로
    줄글 중심의 시장 요약을 생성하세요.
    반드시 스키마(MarketSentimentReport)에 맞는 JSON만 반환합니다. (설명·코드·마크다운 금지)

    [참고 서적 발췌(books_context) 활용 지침]
    - 아래 books_context에는 John C. Bogle, Mark Minervini, Peter Lynch, What Works on Wall Street 등의
      투자 서적에서 발췌한 내용이 포함되어 있습니다.
    - 이 텍스트는 시장 심리, 리스크 온/오프, 인덱스 투자 vs 단기 트레이딩 관점, 변동성 국면에서
      투자자가 유의해야 할 철학/원칙을 이해하는 데 활용합니다.
    - books_context의 문장을 그대로 인용할 필요는 없으며,
      요약된 철학과 원칙을 바탕으로 basic_overview 및 각 지수 summary의 톤과 해석에 반영합니다.
    - 단, 현 시점의 지수 수치나 뉴스는 반드시 kospi_json, kosdaq_json, news_json을 우선 사용합니다.

    [스키마]
    - MarketSentimentReport
    - kospi: IndexSentiment
    - kosdaq: IndexSentiment
    - basic_overview: 문자열(2~4문장) — 코스피·코스닥 각각의 상태를 종합한 전반 시장 상황을 자연스럽게 서술
    - news_overview: 문자열(2~4문장) — 제공된 뉴스 흐름을 반영해 시장 톤을 설명(과도한 확언 금지)

    - IndexSentiment
    - market: "KOSPI" 또는 "KOSDAQ"
    - label: "상승" | "중립" | "하락"  (세 글자 그대로)
    - confidence: 0~1  (소수)
    - summary: 문자열(2~4문장) — RSI, 모멘텀, 변동성 등의 핵심 포인트를 자연스럽게 녹여 문단으로 기술

    [라벨링 가이드]
    - RSI(21) 기준:
      - > 70 → "상승"(과열 뉘앙스는 summary에 자연스럽게 포함)
      - 55 ~ 70 → "상승"
      - 45 ~ 55 → "중립"
      - < 45 → "하락"
    - Momentum(%) 해석:
      - > 0 → 상승 추세, ≥ 10%면 “강한 상승”으로 요약 내부에 반영
      - < 0 → 하락/조정 압력
    - 변동성(21d/63d):
      - 21d 변동성이 63d 대비 높아지면 “변동성 확대”로 서술(리스크 톤 상향)
      - 안정되면 “변동성 축소/안정”으로 서술
    - confidence(확신도):
      - 지표 일관성이 높고 방향성이 명확할수록 0.7~0.9 범위,
      - 신호가 상충하거나 뚜렷하지 않으면 0.4~0.6 범위로 자연스럽게 배정

    [시장 레짐 산출(내부 논리, 출력 필드 아님)]
    - 두 지수 모두 "상승"이면 market_risk_on
    - 두 지수 모두 "하락"이면 market_risk_off
    - 그 외 조합은 market_neutral
    ※ 레짐 값 자체는 출력하지 않음. 대신 basic_overview와 news_overview 서술에 반영.

    [작성 규칙]
    - 모든 텍스트는 한국어 / 입니다 체로 작성.
    - 이모티콘/마크다운/불릿 대신 자연스러운 문장으로 작성.
    - summary와 overview는 수치 나열을 피하고 맥락 중심으로 기술(필요 시 핵심 수치 1~2개를 문장에 녹여 표현).
    - kospi.market은 "KOSPI", kosdaq.market은 "KOSDAQ"으로 정확히 표기.
    - 제공되지 않은 정보(추가 필드, 임의 뉴스, 링크 등) 생성 금지.

    [뉴스 활용 지침]
    - news_overview는 news_json의 제목 목록을 단서로 시장 톤을 서술.
    - 개별 기사명을 나열하기보다, 공통 주제(수급, 대형주/성장주, 환율·대외 변수, 낙관/경계 심리)를 요약적으로 반영.
    - 직접적 연계가 불분명하면 과도한 인과 주장 금지.

    [입력 원본]
    [kospi_json]
    {kospi_json}

    [kosdaq_json]
    {kosdaq_json}

    [news_json]
    {news_json}

    [books_context]
    {books_context}
    """

    prompt = PromptTemplate.from_template(template)
    llm = ChatOpenAI(model="gpt-5", temperature=0).with_structured_output(
        MarketSentimentReport
    )
    chain = prompt | llm
    result = chain.invoke(
        {
            "kospi_json": kospi_json,
            "kosdaq_json": kosdaq_json,
            "news_json": news_json,
            "books_context": books_context,
        }
    )
    return result.model_dump_json()

# llm call 2

def hyde_generate_for_stock(index_info_json: str, news_json: str, stock_info_json: str) -> str:
    prompt = f"""
    너는 한국 주식시장 전문 애널리스트이자 투자 서적 요약가입니다.

    아래 정보를 참고하여, 하나의 개별 종목에 대한
    '투자 서적 관점에서 본 종합 해설 텍스트'를 작성하세요.

    1) index_info_json: KOSPI/KOSDAQ 전반 지수 심리 및 레짐 정보
    2) news_json: 시장 및 종목 관련 주요 뉴스 정보
    3) stock_info_json: 해당 종목의 재무, 밸류에이션, 기술적 지표 등 정보

    이 정보를 바탕으로,
    - 현재 시장 국면(리스크 온/오프, 변동성 수준, 지수 방향성),
    - 해당 종목의 펀더멘털(밸류에이션, 수익성, 성장성),
    - 기술적 흐름(추세, 모멘텀, 거래량, 변동성),
    - 리스크 관리 및 포지션 사이징 관점에서의 시사점

    을 2~4단락 정도의 한국어 해설 텍스트로 작성하세요.
    John C. Bogle, Mark Minervini, Peter Lynch, What Works on Wall Street의
    철학을 참고하되, 실제 문장을 그대로 인용하지 말고 요약된 원칙을 적용해 설명하세요.

    [index_info_json]
    {index_info_json}

    [news_json]
    {news_json}

    [stock_info_json]
    {stock_info_json}
    """
    res = hyde_llm.invoke(prompt.strip())
    return res.content

def build_books_context_for_stock(index_info_json: str, news_json: str, stock_info_json: str) -> str:
    hypothetical_doc = hyde_generate_for_stock(
        index_info_json=index_info_json,
        news_json=news_json,
        stock_info_json=stock_info_json,
    )
    query_embedding = embeddings.embed_query(hypothetical_doc)
    docs = book_vectordb.similarity_search_by_vector(query_embedding, k=5)
    ctx = ""
    for i, d in enumerate(docs, start=1):
        source = d.metadata.get("source", "unknown")
        page = d.metadata.get("page", "N/A")
        ctx += f"\n[Book {i} | {source} | page {page}]\n{d.page_content}\n"
    return ctx.strip()

def llm_call_2_rag(index_info_json: str, news_json: str, stock_info_json: str) -> str:
    class StockAnalysisReport(BaseModel):
        asof_date: str = Field(description="해당 평가 기준 일자 (예: 2025-10-16)")
        fundamental_analysis: str = Field(description="기본적 분석을 수행해서 문단 형태로 기술. 2~4문장 이내. **입니다 체**")
        technical_analysis: str = Field(description="기술적 분석을 수행해서 문단 형태로 기술. 2~4문장 이내. **입니다 체**")
        label: str = Field(description="상승 | 하락 | 중립")
        confidence: float = Field(description="0~1")
        summary: str = Field(description="위 기본적/기술적분석, 현재 지수 상태, 뉴스를 이용한 종합적인 종목 진단을 문단 형태로 기술. 3~5문장 이내. **입니다 체**")
        news: List[str] = Field(description="종목 분석에 참고한 뉴스 제목 0-10개. 단순 시장에 영향을 주는 뉴스는 약간만, 관련업종에 직접적인 것들 위주로 채우기. 없으면 빈 리스트([])")

    books_context = build_books_context_for_stock(
        index_info_json=index_info_json,
        news_json=news_json,
        stock_info_json=stock_info_json,
    )

    template = """
    당신은 한국 주식시장 전문 애널리스트입니다.
    입력(index_info_json, news_json, stock_info_json)과 투자 관련 서적 발췌(books_context)를 기반으로
    하나의 종목 분석 결과를 생성하고,
    아래 JSON 스키마(StockAnalysisReport)를 정확히 채워 JSON만 반환하세요.
    코드, 설명, 마크다운, 여분 텍스트는 절대 포함하지 마세요. 반드시 한국어로 간결히 작성하세요.

    [서적 발췌(books_context) 활용 지침]
    - books_context에는 John C. Bogle, Mark Minervini, Peter Lynch, What Works on Wall Street 등의
      투자 서적에서 발췌한 내용이 포함되어 있습니다.
    - 이 텍스트는 밸류에이션 판단, 모멘텀/트렌드 해석, 리스크 관리, 포지션 사이징, 시장 국면별 전략 등에 대한
      투자 철학과 원칙을 이해하는 데 활용합니다.
    - books_context의 문장을 그대로 인용할 필요는 없으며,
      요약된 철학과 원칙을 바탕으로 fundamental_analysis, technical_analysis, summary의 톤과 해석에 반영합니다.
    - 단, 개별 종목의 수치와 지표는 반드시 stock_info_json과 index_info_json, news_json을 우선 사용합니다.

    [작성 지침]
    - fundamental_analysis:
      주요 재무지표(PER, PBR, ROE, EPS, 배당 등)를 2~4문장으로 요약합니다.
      수치 단순 나열보다는 밸류에이션 수준과 수익성 중심으로 기술합니다.
    - technical_analysis:
      여러 지표보다는 주가 추세(상승/하락/횡보)를 바탕으로 2~4문장 요약합니다.
      분기별로 추출된 장기 주가와 일별로 추출된 단기 주가를 모두 고려합니다.
    - label:
      상승 | 중립 | 하락 중 하나를 선택합니다. (기본적+기술적 판단 종합)
    - confidence:
      0~1 사이 실수로 판단 신뢰도 또는 시그널 일관성을 반영합니다.
    - summary:
      뉴스(news_json)와 기본적 분석 및 기술적 분석을 위주로 시장(index_info_json)의 전반 흐름을 고려해서
      3~5문장으로 요약 진단을 작성합니다.
    - news:
      뉴스는 제공된 news_json 내 관련 기사만 언급하며, 제공 외 뉴스 언급은 금지합니다.
      데이터 부족 시 간단히 명시합니다.

    [출력 형식]
    - 반드시 StockAnalysisReport 스키마를 그대로 JSON 형태로 출력합니다.
    - 키 이름 수정/삭제/추가를 하지 않습니다.
    - JSON 외의 어떤 텍스트도 출력하지 않습니다.

    [작성 규칙]
    - 모든 텍스트는 한국어 / **입니다 체**로 작성합니다.
    - 이모티콘/마크다운/불릿 대신 자연스러운 문장으로 작성합니다.

    [index_info_json]
    {index_info_json}

    [news_json]
    {news_json}

    [stock_info_json]
    {stock_info_json}

    [books_context]
    {books_context}
    """

    prompt = PromptTemplate.from_template(template)
    llm = ChatOpenAI(model="gpt-5-nano", temperature=0).with_structured_output(StockAnalysisReport)
    chain = prompt | llm

    result = chain.invoke(
        {
            "index_info_json": index_info_json,
            "news_json": news_json,
            "stock_info_json": stock_info_json,
            "books_context": books_context,
        }
    )

    return result.model_dump_json()

# parallel threadpool

def run_parallel_threadpool(index_info_json, news_json, tmp_dict, max_workers: int = 24):
    results = {}
    max_workers = max(1, int(max_workers))

    def job(ticker: str, stock_info_json):
        try:
            out = llm_call_2_rag(index_info_json, news_json, stock_info_json)
            return ticker, out
        except Exception as e:
            return ticker, f'{{"error":"{str(e)}"}}'

    with ThreadPoolExecutor(max_workers=max_workers) as ex:
        futures = {ex.submit(job, ticker, stock_json): ticker
                   for ticker, stock_json in tmp_dict.items()}
        for fut in as_completed(futures):
            ticker, out = fut.result()
            results[ticker] = out

    return results

# functions for save data

def save_s3(date, content, data):
    bucket = 'swpp-12-bucket'
    s3 = boto3.client("s3")
    year = date.split('-')[0]
    month = date.split('-')[1]
    key = f"llm_output/{content}/year={year}/month={month}/{date}.json"
    s3.put_object(
        Bucket=bucket,
        Key=key,
        Body=json.dumps(data, ensure_ascii=False),
        ContentType='application/json'
    )
    print(f"[S3] Uploaded to s3://{bucket}/{key}")
    return 0

# =================================================

SAVE_S3 = True
BOOK_DB = "../book_chroma_db"

today = get_today_date()
recent_td = get_recent_closed_trading_day()
first_month_td = first_trading_day_krx(today)

recent_td = '2025-11-24'

print('today', today)
print('recent_td', recent_td)
print('fist_month_td', first_month_td)

if is_trading_day_krx():
    print('거래일 입니다.')
    kospi_json = get_kospi_json(recent_td) 
    kosdaq_json = get_kosdaq_json(recent_td)
    news_json = get_news_json(today)

    all_info, all_profile = get_stock_info_df(recent_td)

    target_num = 500
    top_tickers = np.array(all_info.loc[recent_td]['ticker'][all_info.loc[recent_td]['market_cap'].astype(int).rank(ascending=False)<=target_num])

    recent_date = all_info.index.get_level_values(0).max()
    recent_tickers = all_info.loc[recent_date]['ticker'].unique()

    tmp_dict = {}
    grouped = all_info.groupby('ticker', group_keys=False)
    for ticker, df in grouped:
        if ticker not in recent_tickers or ticker not in all_profile.index:
            continue
        if ticker in top_tickers:
            profile = all_profile.loc[ticker]['explanation']
            company_json = get_company_json(df, profile)
            tmp_dict[ticker] = company_json

    embeddings = OpenAIEmbeddings()
    book_vectordb = Chroma(
        embedding_function=embeddings,
        persist_directory=BOOK_DB
    )
    hyde_llm = ChatOpenAI(model="gpt-5-nano", temperature=0)

    index_info_json = llm_call_1_rag(kospi_json, kosdaq_json, news_json)
    print('index_info_json complete')
    if SAVE_S3: save_s3(today, 'market-index-overview', index_info_json)

    all_analysis = run_parallel_threadpool(index_info_json, news_json, tmp_dict, max_workers=100)
    print('all_analsis complete')
    if SAVE_S3: save_s3(today, 'company-overview', all_analysis)
else:
    print('거래일이 아닙니다.')
