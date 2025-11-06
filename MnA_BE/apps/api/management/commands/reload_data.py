# apps/api/management/commands/reload_data.py
"""
데이터를 수동으로 리로드하는 커맨드
실행: python manage.py reload_data
"""

from django.core.management.base import BaseCommand
from apps.api.apps import ApiConfig
from S3.finance import FinanceS3Client
from apps.api.constants import FINANCE_BUCKET
from datetime import datetime


class Command(BaseCommand):
    help = 'Reload instant and profile data from S3 into memory'

    def handle(self, *args, **options):
        self.stdout.write(self.style.WARNING('Starting data reload...'))
        
        try:
            s3 = FinanceS3Client()
            
            # 1) Instant 데이터 로드
            self.stdout.write('Loading instant data...')
            instant_df, ts = s3.get_latest_parquet_df(
                FINANCE_BUCKET,
                'price-financial-info-instant/'
            )
            
            if instant_df is not None:
                ApiConfig.instant_df = instant_df
                self.stdout.write(self.style.SUCCESS(
                    f'✓ Instant data loaded: {instant_df.shape}'
                ))
                self.stdout.write(
                    f'  - Unique tickers: {instant_df["ticker"].nunique()}'
                )
                self.stdout.write(
                    f'  - Date range: {instant_df["date"].min()} ~ {instant_df["date"].max()}'
                )
            
            # 2) Company Profile 데이터 로드
            self.stdout.write('Loading profile data...')
            profile_df, _ = s3.get_latest_parquet_df(
                FINANCE_BUCKET,
                'company-profile/'
            )
            
            if profile_df is not None:
                ApiConfig.profile_df = profile_df
                self.stdout.write(self.style.SUCCESS(
                    f'✓ Profile data loaded: {profile_df.shape}'
                ))
            
            ApiConfig.last_loaded = datetime.now()
            self.stdout.write(self.style.SUCCESS(
                f'✓ Data reloaded at: {ApiConfig.last_loaded}'
            ))
            
        except Exception as e:
            self.stdout.write(self.style.ERROR(f'✗ Error: {e}'))