# apps/api/management/commands/reload_data.py
"""
데이터를 수동으로 리로드하는 커맨드
실행: python manage.py reload_data
"""

from django.core.management.base import BaseCommand
from django.core.cache import cache
from S3.finance import FinanceS3Client
from apps.api.constants import FINANCE_BUCKET
from datetime import datetime
import time
import pandas as pd
import boto3
import os
import io


class Command(BaseCommand):
    help = 'Reload instant and profile data from S3 into Django cache'

    def handle(self, *args, **options):
        self.stdout.write(self.style.WARNING('Starting data reload...'))
        
        try:
            total_start = time.time()
            s3 = FinanceS3Client()
            
            # 1) Instant 데이터 로드
            self.stdout.write('Loading instant data...')
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
                
                # Django 캐시에 저장
                cache.set('instant_df', instant_df, timeout=None)
                
                self.stdout.write(self.style.SUCCESS(
                    f'✓ Instant data loaded to cache: {instant_df.shape}'
                ))
                self.stdout.write(f'  - S3 download: {instant_elapsed:.2f}s')
                self.stdout.write(f'  - Sort time: {sort_elapsed:.2f}s')
                self.stdout.write(
                    f'  - Unique tickers: {instant_df["ticker"].nunique()}'
                )
                self.stdout.write(
                    f'  - Date range: {instant_df["date"].min()} ~ {instant_df["date"].max()}'
                )
            
            # 2) Company Profile 데이터 로드 (KOSPI + KOSDAQ 자동 검색)
            self.stdout.write('Loading profile data...')
            profile_start = time.time()
            
            # boto3로 market별 최신 파일 자동 검색
            s3_client = boto3.client(
                's3',
                aws_access_key_id=os.getenv('FINANCE_AWS_ACCESS_KEY_ID'),
                aws_secret_access_key=os.getenv('FINANCE_AWS_SECRET_ACCESS_KEY'),
                region_name=os.getenv('AWS_REGION')
            )
            
            response = s3_client.list_objects_v2(
                Bucket=FINANCE_BUCKET,
                Prefix='company-profile/'
            )
            
            if 'Contents' in response:
                files = sorted(response['Contents'], key=lambda x: x['LastModified'], reverse=True)
                
                # market별 최신 파일 찾기
                kospi_file = None
                kosdaq_file = None
                
                for f in files:
                    if 'market=kospi' in f['Key'] and kospi_file is None:
                        kospi_file = f['Key']
                    if 'market=kosdaq' in f['Key'] and kosdaq_file is None:
                        kosdaq_file = f['Key']
                    if kospi_file and kosdaq_file:
                        break
                
                # 파일 로드
                profile_kospi = None
                profile_kosdaq = None
                
                if kospi_file:
                    obj = s3_client.get_object(Bucket=FINANCE_BUCKET, Key=kospi_file)
                    profile_kospi = pd.read_parquet(io.BytesIO(obj['Body'].read()))
                    self.stdout.write(f'  - KOSPI from: {kospi_file}')
                
                if kosdaq_file:
                    obj = s3_client.get_object(Bucket=FINANCE_BUCKET, Key=kosdaq_file)
                    profile_kosdaq = pd.read_parquet(io.BytesIO(obj['Body'].read()))
                    self.stdout.write(f'  - KOSDAQ from: {kosdaq_file}')
                
                # 두 데이터프레임 합치기
                if profile_kosdaq is not None and profile_kospi is not None:
                    profile_df = pd.concat([profile_kosdaq, profile_kospi])
                    
                    # Django 캐시에 저장
                    cache.set('profile_df', profile_df, timeout=None)
                    
                    self.stdout.write(self.style.SUCCESS(
                        f'✓ Profile data loaded to cache: {profile_df.shape}'
                    ))
                    self.stdout.write(f'  - KOSDAQ: {len(profile_kosdaq)} 종목')
                    self.stdout.write(f'  - KOSPI: {len(profile_kospi)} 종목')
                    self.stdout.write(f'  - Total: {len(profile_df)} 종목')
                elif profile_kosdaq is not None:
                    cache.set('profile_df', profile_kosdaq, timeout=None)
                    self.stdout.write(self.style.SUCCESS(
                        f'✓ Profile data loaded (KOSDAQ only): {profile_kosdaq.shape}'
                    ))
                elif profile_kospi is not None:
                    cache.set('profile_df', profile_kospi, timeout=None)
                    self.stdout.write(self.style.SUCCESS(
                        f'✓ Profile data loaded (KOSPI only): {profile_kospi.shape}'
                    ))
            
            profile_elapsed = time.time() - profile_start
            self.stdout.write(f'  - Load time: {profile_elapsed:.2f}s')
            
            total_elapsed = time.time() - total_start
            
            # 로드 시각 저장
            cache.set('data_last_loaded', datetime.now(), timeout=None)
            
            self.stdout.write(self.style.SUCCESS(
                f'✓ Total reload time: {total_elapsed:.2f}s'
            ))
            self.stdout.write(self.style.SUCCESS(
                f'✓ Data reloaded at: {datetime.now()}'
            ))
            
        except Exception as e:
            self.stdout.write(self.style.ERROR(f'✗ Error: {e}'))
            import traceback
            self.stdout.write(traceback.format_exc())