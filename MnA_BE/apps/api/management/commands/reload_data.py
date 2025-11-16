# apps/api/management/commands/reload_data.py
"""
데이터를 수동으로 리로드하는 커맨드
실행: python manage.py reload_data
"""

from django.core.management.base import BaseCommand
from utils import instant_data


class Command(BaseCommand):
    help = 'Reload instant and profile data from S3 into Django cache'

    def handle(self, *args, **options):
        self.stdout.write(self.style.WARNING('Starting data reload...'))
        
        try:
            instant_data.reload()
            
        except Exception as e:
            self.stdout.write(self.style.ERROR(f'✗ Error: {e}'))
            import traceback
            self.stdout.write(traceback.format_exc())