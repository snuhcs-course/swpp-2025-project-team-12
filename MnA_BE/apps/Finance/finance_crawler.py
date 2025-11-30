import warnings

warnings.filterwarnings("ignore", category=UserWarning)

import calendar
import pandas_market_calendars as mcal
import time
import requests
import pandas as pd
import numpy as np
import os
import io
import boto3
from datetime import datetime, date
from zoneinfo import ZoneInfo
from pykrx import stock
from bs4 import BeautifulSoup
from io import BytesIO


def get_kst_now() -> datetime:
    return datetime.now(ZoneInfo("Asia/Seoul"))


def is_trading_day_krx(d: date) -> bool:
    krx = mcal.get_calendar("XKRX")
    ds = d.strftime("%Y-%m-%d")
    schedule = krx.schedule(start_date=ds, end_date=ds)
    return not schedule.empty


def first_trading_day_of_month(year: int, month: int) -> date | None:
    krx = mcal.get_calendar("XKRX")
    start = date(year, month, 1)
    last_day = calendar.monthrange(year, month)[1]
    end = date(year, month, last_day)

    schedule = krx.schedule(
        start_date=start.strftime("%Y-%m-%d"), end_date=end.strftime("%Y-%m-%d")
    )
    if schedule.empty:
        return None
    return schedule.index[0].date()


def is_first_trading_day(d: date) -> bool:
    if not is_trading_day_krx(d):
        return False
    ftd = first_trading_day_of_month(d.year, d.month)
    return ftd is not None and d == ftd


def get_stock_info(biz_day, mktId):  # STK, KSQ
    gen_otp_url = "http://data.krx.co.kr/comm/fileDn/GenerateOTP/generate.cmd"
    gen_otp_stk = {
        "mktId": mktId,
        "trdDd": biz_day,
        "money": "1",
        "csvxls_isNo": "false",
        "name": "fileDown",
        "url": "dbms/MDC/STAT/standard/MDCSTAT03901",
    }
    headers = {
        "Referer": "http://data.krx.co.kr/contents/MDC/MDI/mdiLoader",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36",
    }
    otp_stk = requests.post(gen_otp_url, gen_otp_stk, headers=headers).text
    down_url = "http://data.krx.co.kr/comm/fileDn/download_csv/download.cmd"
    down_sector_stk = requests.post(down_url, {"code": otp_stk}, headers=headers)
    sector_stk = pd.read_csv(BytesIO(down_sector_stk.content), encoding="EUC-KR")
    sector_stk.columns = [
        "ticker",
        "name",
        "market",
        "industry",
        "close",
        "change",
        "change_rate",
        "market_cap",
    ]
    sector_stk.set_index("ticker", inplace=True)
    return sector_stk


def get_explanation(code):
    url = f"https://navercomp.wisereport.co.kr/v2/company/c1010001.aspx?cmp_cd={code}"
    response = requests.get(url)
    soup = BeautifulSoup(response.text, "html.parser")
    elements = soup.find_all("li", class_="dot_cmp")
    return " ".join(li.text.strip() for li in elements)


def get_explanation_per_tickers(tickers, sleep):
    explained = []

    for idx, i in enumerate(tickers, 1):
        explanation = get_explanation(i)
        explained.append({"ticker": i, "explanation": explanation})
        time.sleep(sleep)
        if idx % 100 == 0:
            print(f"{idx} tickers processed...")

    explained_df = pd.DataFrame(explained).set_index("ticker")
    return explained_df


def save_df_s3(prefix, today, market, df):
    bucket = os.getenv("S3_BUCKET")
    s3 = boto3.client("s3")

    year, month = today.year, today.month

    key = f"{prefix}/year={year}/month={month}/market={market}/{today.strftime('%Y-%m-%d')}.parquet"

    buffer = io.BytesIO()
    df.to_parquet(buffer, engine="pyarrow", index=True)
    buffer.seek(0)

    s3.upload_fileobj(buffer, bucket, key)
    print(f"[S3] Uploaded to s3://{bucket}/{key}")
    return 0


now = get_kst_now()
today = now.date()
print(today)

trading_day = is_trading_day_krx(today)
first_trading_day = is_first_trading_day(today)
print("오늘은 거래일인가?", trading_day)
print("오늘은 매월 초 거래일 인가?", first_trading_day)
print()


# trading_day==True 일때 실행하게 된다.

biz_day = "20251002"
market = "STK"
# kospi: STK / kosdaq: KSQ

today_snapshot = get_stock_info(biz_day, "STK")
print(today_snapshot)  # 이후 s3에 저장.
print()


# 너무 오래 걸리기 때문에 demo에서는 일부만 가져온다.
tickers = today_snapshot.index[:10]

explained_df = get_explanation_per_tickers(tickers, 1)
print(explained_df)  # 이후 s3에 저장.
