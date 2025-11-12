# apps/api/apps.py
from django.apps import AppConfig
from django.core.cache import cache
import pandas as pd
from utils.debug_print import debug_print


class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'apps.api'

    def ready(self):
        """
        Django 앱 시작 시 한 번만 실행
        instant 데이터를 Django 캐시에 로드
        """
        import os
        
        run_main = os.environ.get('RUN_MAIN')
        if run_main != 'true' and run_main is not None:
            debug_print("Skipping data load in main process (reloader)")
            return
            
        try:
            from S3.finance import FinanceS3Client
            from apps.api.constants import FINANCE_BUCKET
            from datetime import datetime
            import time
            
            debug_print("=" * 50)
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
                
                # Django 캐시에 저장 (영구)
                cache.set('instant_df', instant_df, timeout=None)
                
                debug_print(f"✓ Instant data loaded to cache: {instant_df.shape}")
                debug_print(f"  - S3 download time: {instant_elapsed:.2f}s")
                debug_print(f"  - Sort time: {sort_elapsed:.2f}s")
                debug_print(f"  - Unique tickers: {instant_df['ticker'].nunique()}")
                debug_print(f"  - Date range: {instant_df['date'].min()} ~ {instant_df['date'].max()}")
                debug_print(f"  - Sorted by: date (asc), market_cap (desc)")
            
            # 2) Company Profile 데이터 로드 (KOSPI + KOSDAQ 자동 검색)
            profile_start = time.time()
            
            import boto3
            import io
            
            s3_client = boto3.client(
                's3',
                aws_access_key_id=os.getenv('FINANCE_AWS_ACCESS_KEY_ID'),
                aws_secret_access_key=os.getenv('FINANCE_AWS_SECRET_ACCESS_KEY'),
                region_name=os.getenv('FINANCE_AWS_REGION')
            )
            
            response = s3_client.list_objects_v2(
                Bucket=FINANCE_BUCKET,
                Prefix='company-profile/'
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
                
                if kospi_file:
                    obj = s3_client.get_object(Bucket=FINANCE_BUCKET, Key=kospi_file)
                    profile_kospi = pd.read_parquet(io.BytesIO(obj['Body'].read()))
                    debug_print(f"  - KOSPI profile from: {kospi_file}")
                
                if kosdaq_file:
                    obj = s3_client.get_object(Bucket=FINANCE_BUCKET, Key=kosdaq_file)
                    profile_kosdaq = pd.read_parquet(io.BytesIO(obj['Body'].read()))
                    debug_print(f"  - KOSDAQ profile from: {kosdaq_file}")
                
                if profile_kosdaq is not None and profile_kospi is not None:
                    profile_df = pd.concat([profile_kosdaq, profile_kospi])
                    
                    # Django 캐시에 저장 (영구)
                    cache.set('profile_df', profile_df, timeout=None)
                    
                    debug_print(f"✓ Profile data loaded to cache: {profile_df.shape}")
                    debug_print(f"  - KOSDAQ: {len(profile_kosdaq)} 종목")
                    debug_print(f"  - KOSPI: {len(profile_kospi)} 종목")
                    debug_print(f"  - Total: {len(profile_df)} 종목")
                elif profile_kosdaq is not None:
                    cache.set('profile_df', profile_kosdaq, timeout=None)
                    debug_print(f"✓ Profile data loaded (KOSDAQ only): {profile_kosdaq.shape}")
                elif profile_kospi is not None:
                    cache.set('profile_df', profile_kospi, timeout=None)
                    debug_print(f"✓ Profile data loaded (KOSPI only): {profile_kospi.shape}")
            
            profile_elapsed = time.time() - profile_start
            debug_print(f"  - Load time: {profile_elapsed:.2f}s")
            
            # 로드 시각 저장
            total_elapsed = time.time() - total_start
            cache.set('data_last_loaded', datetime.now(), timeout=None)
            
            debug_print(f"✓ Total loading time: {total_elapsed:.2f}s")
            debug_print(f"✓ Data loaded at: {datetime.now()}")
            debug_print("=" * 50)
            
        except Exception as e:
            debug_print(f"✗ Error loading data: {e}")
            import traceback
            debug_print(traceback.format_exc())