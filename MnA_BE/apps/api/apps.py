# apps/api/apps.py
from django.apps import AppConfig
from django.core.cache import cache
import pandas as pd
from utils.debug_print import debug_print
from utils import instant_data


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
            instant_data.init()
            
        except Exception as e:
            debug_print(f"✗ Error loading data: {e}")
            import traceback
            debug_print(traceback.format_exc())