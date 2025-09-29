# apps/MarketIndex/models.py
from django.db import models
from django.utils import timezone

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
        db_table = 'marketindex_stockindex'  # Explicit table name
    
    def __str__(self):
        return f"{self.index_type}: {self.close_price} - {self.timestamp.strftime('%Y-%m-%d')}"