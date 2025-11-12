# apps/api/apps.py

from django.apps import AppConfig
import pandas as pd
from utils.debug_print import debug_print


class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'apps.api'
    
    # 클래스 변수로 데이터 저장 (메모리에 상주)
    instant_df = None
    profile_df = None
    last_loaded = None

    def ready(self):
        """
        Django 앱 시작 시 한 번만 실행
        instant 데이터를 메모리에 로드
        """
        # 개발 서버 reload 시 중복 실행 방지
        # import os
        # if os.environ.get('RUN_MAIN') == 'true' or os.environ.get('WERKZEUG_RUN_MAIN') == 'true':
        #     return
            
        try:
            from S3.finance import FinanceS3Client
            from apps.api.constants import FINANCE_BUCKET
            from datetime import datetime
            
            debug_print("=" * 50)
            debug_print("Loading instant data into memory...")
            
            s3 = FinanceS3Client()
            
            # 1) Instant 데이터 로드
            instant_df, ts = s3.get_latest_parquet_df(
                FINANCE_BUCKET,
                'price-financial-info-instant/'
            )
            
            if instant_df is not None:
                self.instant_df = instant_df
                debug_print(f"✓ Instant data loaded: {instant_df.shape}")
                debug_print(f"  - Unique tickers: {instant_df['ticker'].nunique()}")
                debug_print(f"  - Date range: {instant_df['date'].min()} ~ {instant_df['date'].max()}")
            
            # 2) Company Profile 데이터 로드
            profile_df, _ = s3.get_latest_parquet_df(
                FINANCE_BUCKET,
                'company-profile/'
            )
            
            if profile_df is not None:
                self.profile_df = profile_df
                debug_print(f"✓ Profile data loaded: {profile_df.shape}")
            
            self.last_loaded = datetime.now()
            debug_print(f"✓ Data loaded at: {self.last_loaded}")
            debug_print("=" * 50)
            
        except Exception as e:
            debug_print(f"✗ Error loading data: {e}")
            self.instant_df = None
            self.profile_df = None