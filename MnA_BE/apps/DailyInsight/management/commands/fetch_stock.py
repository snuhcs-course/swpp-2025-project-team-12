from django.core.management.base import BaseCommand
from apps.DailyInsight.models import StockIndex
from decimal import Decimal
import yfinance as yf

class Command(BaseCommand):
    help = 'Fetch real stock data'
    
    def handle(self, *args, **options):
        # Let's fetch KOSPI data
        self.stdout.write('Fetching KOSPI data...')
        
        try:
            # Download KOSPI data (^KS11 is KOSPI symbol)
            ticker = yf.Ticker("^KS11")
            hist = ticker.history(period="1d")  # Get today's data
            
            if not hist.empty:
                # Get the latest price
                latest_price = hist['Close'].iloc[-1]
                
                # Create a record in our database
                stock = StockIndex.objects.create(
                    index_type='KOSPI',
                    close_price=Decimal(str(latest_price)),
                    change_percent=Decimal('0')  # We'll calculate this later
                )
                
                self.stdout.write(
                    self.style.SUCCESS(f'✓ KOSPI saved: {latest_price:.2f}')
                )
            else:
                self.stdout.write(
                    self.style.WARNING('No data received from yfinance')
                )
                
        except Exception as e:
            self.stdout.write(
                self.style.ERROR(f'Error: {e}')
            )