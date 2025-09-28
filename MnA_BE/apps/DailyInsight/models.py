from django.db import models
from django.utils import timezone

TINY_TEXT = 255
LONG_TEXT = 255

class PriceFluctuation(models.Model):
    recommended_date = models.DateField()

class Recommendation(models.Model):
    stock_info = models.CharField(max_length=LONG_TEXT)
    reason = models.CharField(max_length=LONG_TEXT)
    links = models.CharField(max_length=LONG_TEXT)
    date = models.DateField()

class StockInfo(models.Model):
    ticker = models.CharField(max_length=TINY_TEXT)
    company_name = models.CharField(max_length=TINY_TEXT)
    market_type = models.CharField(max_length=TINY_TEXT)
    industry_tag = models.CharField(max_length=TINY_TEXT)
    company_profile = models.CharField(max_length=LONG_TEXT)

class StockIndex(models.Model):
    # Simple choices for stock types
    INDEX_CHOICES = [
        ('KOSPI', 'KOSPI'),
        ('KOSDAQ', 'KOSDAQ'),
        ('SP500', 'S&P 500'),
    ]
    
    # Basic fields
    index_type = models.CharField(max_length=10, choices=INDEX_CHOICES)
    close_price = models.DecimalField(max_digits=10, decimal_places=2)
    change_percent = models.DecimalField(max_digits=5, decimal_places=2, default=0)
    timestamp = models.DateTimeField(default=timezone.now)
    
    class Meta:
        ordering = ['-timestamp']  # Most recent first
    
    def __str__(self):
        return f"{self.index_type}: {self.close_price} {self.change_percent}% - {self.timestamp.strftime('%Y-%m-%d')}"