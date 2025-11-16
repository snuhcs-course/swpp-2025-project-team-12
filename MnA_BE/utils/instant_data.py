from django.core.cache import cache
from utils.debug_print import debug_print
from utils.store import store
from S3.finance import FinanceS3Client
from apps.api.constants import FINANCE_BUCKET
from datetime import datetime
from utils.for_api import ok
import pandas as pd
import time, json

def print_line():
    debug_print("=" * 50)


def init():
    print_line()
    debug_print("Loading instant data into Django cache...")

    total_start = time.time()
    s3 = FinanceS3Client()

    # 1) Instant 데이터 로드
    instant_start = time.time()
    instant_df, ts = s3.get_latest_parquet_df(
        FINANCE_BUCKET,
        'price-financial-info-instant/'
    )
    instant_elapsed = time.time() - instant_start

    if instant_df is not None:
        # 시가총액 기준으로 정렬
        sort_start = time.time()
        instant_df['market_cap_numeric'] = pd.to_numeric(instant_df['market_cap'], errors='coerce')
        instant_df = instant_df.sort_values(
            by=['date', 'market_cap_numeric'],
            ascending=[True, False]
        ).drop(columns=['market_cap_numeric'])
        sort_elapsed = time.time() - sort_start

        # Store in Shared memory
        store.set_data('instant_df', instant_df)

        debug_print(f"✓ Instant data loaded to cache: {instant_df.shape}")
        debug_print(f"  - S3 download time: {instant_elapsed:.2f}s")
        debug_print(f"  - Sort time: {sort_elapsed:.2f}s")
        debug_print(f"  - Unique tickers: {instant_df['ticker'].nunique()}")
        debug_print(f"  - Date range: {instant_df['date'].min()} ~ {instant_df['date'].max()}")
        debug_print(f"  - Sorted by: date (asc), market_cap (desc)")

    # 2) Company Profile 데이터 로드 (KOSPI + KOSDAQ 자동 검색)
    profile_start = time.time()

    response = s3.get_list_v2(FINANCE_BUCKET, 'company-profile/')

    if 'Contents' in response:
        files = sorted(response['Contents'], key=lambda x: x['LastModified'], reverse=True)

        kospi_file = None
        kosdaq_file = None

        for f in files:
            if 'market=kospi' in f['Key'] and kospi_file is None:
                kospi_file = f['Key']
            if 'market=kosdaq' in f['Key'] and kosdaq_file is None:
                kosdaq_file = f['Key']
            if kospi_file and kosdaq_file:
                break

        profile_kospi = None
        profile_kosdaq = None

        if kospi_file:
            profile_kospi = s3.get_dataframe(FINANCE_BUCKET, kospi_file)
            debug_print(f"  - KOSPI profile from: {kospi_file}")

        if kosdaq_file:
            profile_kosdaq = s3.get_dataframe(FINANCE_BUCKET, kosdaq_file)
            debug_print(f"  - KOSDAQ profile from: {kosdaq_file}")

        if profile_kosdaq is not None and profile_kospi is not None:
            profile_df = pd.concat([profile_kosdaq, profile_kospi])

            # Store in Shared Memory
            store.set_data('profile_df', profile_df)

            debug_print(f"✓ Profile data loaded to cache: {profile_df.shape}")
            debug_print(f"  - KOSDAQ: {len(profile_kosdaq)} 종목")
            debug_print(f"  - KOSPI: {len(profile_kospi)} 종목")
            debug_print(f"  - Total: {len(profile_df)} 종목")
        elif profile_kosdaq is not None:
            store.set_data('profile_df', profile_kosdaq)
            debug_print(f"✓ Profile data loaded (KOSDAQ only): {profile_kosdaq.shape}")
        elif profile_kospi is not None:
            store.set_data('profile_df', profile_kospi)
            debug_print(f"✓ Profile data loaded (KOSPI only): {profile_kospi.shape}")

    profile_elapsed = time.time() - profile_start
    debug_print(f"  - Load time: {profile_elapsed:.2f}s")

    # 로드 시각 저장
    total_elapsed = time.time() - total_start
    cache.set('data_last_loaded', datetime.now(), timeout=None)

    debug_print(f"✓ Total loading time: {total_elapsed:.2f}s")
    debug_print(f"✓ Data loaded at: {datetime.now()}")
    print_line()


def reload():
    print_line()
    debug_print("Manual data reload triggered...")

    total_start = time.time()
    s3 = FinanceS3Client()

    # 1) Instant 데이터 로드
    instant_start = time.time()
    instant_df, ts = s3.get_latest_parquet_df(
        FINANCE_BUCKET,
        'price-financial-info-instant/'
    )
    instant_elapsed = time.time() - instant_start

    if instant_df is not None:
        # 시가총액 기준 정렬
        instant_df['market_cap_numeric'] = pd.to_numeric(instant_df['market_cap'], errors='coerce')
        instant_df = instant_df.sort_values(
            by=['date', 'market_cap_numeric'],
            ascending=[True, False]
        ).drop(columns=['market_cap_numeric'])

        # Django 캐시에 저장
        store.set_data('instant_df', instant_df)
        debug_print(f"✓ Instant data reloaded to cache: {instant_df.shape}")

    # 2) Profile 데이터 로드 (market별 자동 검색)
    profile_start = time.time()

    response = s3.get_list_v2(
        FINANCE_BUCKET, 'company-profile/'
    )

    if 'Contents' in response:
        files = sorted(response['Contents'], key=lambda x: x['LastModified'], reverse=True)

        kospi_file = None
        kosdaq_file = None

        for f in files:
            if 'market=kospi' in f['Key'] and kospi_file is None:
                kospi_file = f['Key']
            if 'market=kosdaq' in f['Key'] and kosdaq_file is None:
                kosdaq_file = f['Key']
            if kospi_file and kosdaq_file:
                break

        profile_kospi = None
        profile_kosdaq = None

        if kospi_file: profile_kospi = s3.get_dataframe(FINANCE_BUCKET, kospi_file)
        if kosdaq_file: profile_kosdaq = s3.get_dataframe(FINANCE_BUCKET, kosdaq_file)

        if profile_kosdaq is not None and profile_kospi is not None:
            profile_df = pd.concat([profile_kosdaq, profile_kospi])
            store.set_data('profile_df', profile_df)
            debug_print(f"✓ Profile data reloaded to cache: {profile_df.shape}")

    profile_elapsed = time.time() - profile_start

    total_elapsed = time.time() - total_start
    # 로드 시각 저장
    store.set_data('data_last_loaded', datetime.now())

    debug_print(f"✓ Total reload time: {total_elapsed:.2f}s")
    debug_print(f"✓ Data reloaded at: {datetime.now()}")
    print_line()

    # 캐시에서 확인
    instant_df = store.get_data('instant_df')
    profile_df = store.get_data('profile_df')

    return ok({
        "message": "Data reloaded successfully",
        "instant_shape": list(instant_df.shape) if instant_df is not None else None,
        "profile_shape": list(profile_df.shape) if profile_df is not None else None,
        "instant_time": f"{instant_elapsed:.2f}s",
        "profile_time": f"{profile_elapsed:.2f}s",
        "total_time": f"{total_elapsed:.2f}s",
        "reloaded_at": str(datetime.now())
    })