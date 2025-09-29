from django.core.management.base import BaseCommand
from apps.MarketIndex.models import StockIndex
from decimal import Decimal
import yfinance as yf

class Command(BaseCommand):
    help = 'Fetch all stock indices (KOSPI, KOSDAQ, SP500)'
    
    def handle(self, *args, **options):
        # Dictionary of stock indices and their Yahoo Finance symbols
        STOCKS = {
            'KOSPI': '^KS11',    # KOSPI index
            'KOSDAQ': '^KQ11',   # KOSDAQ index  
            'SP500': '^GSPC'     # S&P 500 index
        }
        
        success_count = 0
        
        # Fetch each stock index
        for name, symbol in STOCKS.items():
            self.stdout.write(f'\nFetching {name}...')
            
            try:
                # Download data
                ticker = yf.Ticker(symbol)
                hist = ticker.history(period="2d")  # Get 2 days to calculate change
                
                if len(hist) >= 1:
                    # Get latest price
                    latest_price = hist['Close'].iloc[-1]
                    
                    # Calculate change percentage if we have previous day
                    if len(hist) >= 2:
                        previous_price = hist['Close'].iloc[-2]
                        change = ((latest_price - previous_price) / previous_price) * 100
                    else:
                        change = 0
                    
                    # Save to database
                    stock = StockIndex.objects.create(
                        index_type=name,
                        close_price=Decimal(str(latest_price)),
                        change_percent=Decimal(str(change))
                    )
                    
                    self.stdout.write(
                        self.style.SUCCESS(
                            f'✓ {name}: {latest_price:.2f} ({change:+.2f}%)'
                        )
                    )
                    success_count += 1
                else:
                    self.stdout.write(
                        self.style.WARNING(f'No data for {name}')
                    )
                    
            except Exception as e:
                self.stdout.write(
                    self.style.ERROR(f'✗ {name} failed: {e}')
                )
        
        # Summary
        self.stdout.write(
            f'\n' + self.style.SUCCESS(f'Completed: {success_count}/3 indices fetched successfully')
        )