import warnings
warnings.filterwarnings("ignore", category=UserWarning)
from __future__ import annotations
from datetime import datetime
from zoneinfo import ZoneInfo
import pandas_market_calendars as mcal
import pandas as pd
import numpy as np
import pytz
import io
from typing import Optional
from s3_factory import S3ClientFactory

def is_trading_day_krx():
    d = datetime.now(ZoneInfo("Asia/Seoul"))
    krx = mcal.get_calendar("XKRX")
    ds = d.strftime("%Y-%m-%d")
    schedule = krx.schedule(start_date=ds, end_date=ds)
    return not schedule.empty

def get_recent_closed_trading_day(num):
    d = datetime.now(ZoneInfo("Asia/Seoul"))
    krx = mcal.get_calendar("XKRX")
    
    start_date = "2020-01-01"
    end_date = d.strftime("%Y-%m-%d")
    
    schedule = krx.schedule(start_date=start_date, end_date=end_date)
    past_trading_days = schedule[schedule["market_close"] < d].index
    
    if not past_trading_days.empty:
        return past_trading_days[-num].strftime("%Y-%m-%d")
    else:
        return None

def load_s3_parquet(key: str, s3_client: Optional[BaseClient] = None):
    bucket = "swpp-12-bucket"

    if s3_client is None:
        s3_client = S3ClientFactory.get_client()

    obj = s3_client.get_object(Bucket=bucket, Key=key)
    buffer = io.BytesIO(obj["Body"].read())
    df = pd.read_parquet(buffer)

    print(f"[S3] Loaded Parquet from s3://{bucket}/{key}")
    return df

def save_s3_df(now: datetime, df_concat: pd.DataFrame, s3_client: Optional[BaseClient] = None):
    bucket = "swpp-12-bucket"

    if s3_client is None:
        s3_client = S3ClientFactory.get_client()

    buffer = io.BytesIO()
    df_concat.to_parquet(buffer, compression="zstd", index=True)
    buffer.seek(0)

    key = (
        f"price-financial-info-instant/year={now.year}/month={now.month}/"
        f"{now.strftime('%Y-%m-%d')}.parquet"
    )

    s3_client.put_object(
        Bucket=bucket,
        Key=key,
        Body=buffer.getvalue(),
        ContentType="application/octet-stream"
    )

    print(f"[S3] Uploaded Parquet to s3://{bucket}/{key}")
    return key

# =========================

if is_trading_day_krx():
    print("거래일 입니다.")

    date1 = get_recent_closed_trading_day(1)  # 가장 최근 종료된 거래일
    date2 = get_recent_closed_trading_day(2)  # 그 이전 거래일
    print(date1, date2)

    date1_dt = pd.to_datetime(date1)
    date2_dt = pd.to_datetime(date2)

    # 기존 데이터 Load
    key1 = f"price-financial-info/year={date1_dt.year}/month={date1_dt.month}/market=kospi/{date1}.parquet"
    key2 = f"price-financial-info/year={date1_dt.year}/month={date1_dt.month}/market=kosdaq/{date1}.parquet"
    key3 = f"price-financial-info-instant/year={date2_dt.year}/month={date2_dt.month}/{date2}.parquet"

    recent_kospi = load_s3_parquet(key1)
    recent_kosdaq = load_s3_parquet(key2)
    before_stack = load_s3_parquet(key3)

    # 새로운 데이터 머지
    all_recent_df = pd.concat([recent_kospi, recent_kosdaq]) \
                        .replace(["nan", "NaN", "None", ""], np.nan) \
                        .dropna()

    ticker = all_recent_df.index
    all_recent_df = all_recent_df.reset_index(drop=True)
    all_recent_df["ticker"] = ticker
    all_recent_df["date"] = date1_dt

    all_recent_df = pd.concat({date1_dt: all_recent_df})
    new_df = pd.concat([before_stack, all_recent_df])

    # 저장
    save_s3_df(date1_dt, new_df)

else:
    print("거래일이 아닙니다.")
