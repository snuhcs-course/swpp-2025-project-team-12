# apps/api/models.py
from django.db import models

class RecommendationBatch(models.Model):
    market_date   = models.DateField(help_text="KST 기준 추천 일자")
    risk_profile  = models.CharField(max_length=32)  # 안정형/중립형/공격투자형 등
    source        = models.CharField(max_length=16, default="db")
    model_id      = models.CharField(max_length=64, null=True, blank=True)
    as_of_utc     = models.DateTimeField()
    notes         = models.TextField(null=True, blank=True)

    class Meta:
        db_table = "recommendation_batches"
        unique_together = (("market_date", "risk_profile"),)


class RecommendationItem(models.Model):
    batch       = models.ForeignKey(RecommendationBatch, on_delete=models.CASCADE, related_name="items")
    rank        = models.IntegerField()
    ticker      = models.CharField(max_length=16)
    name        = models.CharField(max_length=128)
    market      = models.CharField(max_length=10, default="KOSPI") # KOSPI/KOSDAQ/
    news_titles = models.JSONField(default=list)
    news_urls   = models.JSONField(default=list)
    reason      = models.JSONField(default=list)
    expected_direction = models.CharField(max_length=10, default="neutral")  # up/down/neutral
    conviction  = models.FloatField(default=0.5)
    score       = models.DecimalField(max_digits=6, decimal_places=3, null=True, blank=True)

    class Meta:
        db_table = "recommendation_items"
        ordering = ["rank"]
        indexes = [
            models.Index(fields=["batch", "rank"]),
            models.Index(fields=["ticker"])
        ]

'''class Stock(models.Model):
    ticker   = models.CharField(primary_key=True, max_length=16)
    name     = models.CharField(max_length=128)
    exchange = models.CharField(max_length=16)         # KOSPI/KOSDAQ
    sector   = models.CharField(max_length=64, null=True, blank=True)
    industry = models.CharField(max_length=64, null=True, blank=True)
    profile  = models.TextField(null=True, blank=True)

    class Meta:
        db_table = "stocks"

class PriceFinancial(models.Model):
    ticker      = models.ForeignKey(Stock, on_delete=models.CASCADE)
    date        = models.DateField()
    price       = models.DecimalField(max_digits=16, decimal_places=4, null=True, blank=True)
    market_cap  = models.BigIntegerField(null=True, blank=True)
    eps         = models.DecimalField(max_digits=16, decimal_places=4, null=True, blank=True)
    bps         = models.DecimalField(max_digits=16, decimal_places=4, null=True, blank=True)
    per         = models.DecimalField(max_digits=16, decimal_places=4, null=True, blank=True)
    pbr         = models.DecimalField(max_digits=16, decimal_places=4, null=True, blank=True)
    roe         = models.DecimalField(max_digits=16, decimal_places=4, null=True, blank=True)

    class Meta:
        db_table = "prices_financials"
        unique_together = (("ticker", "date"),)

class MacroIndex(models.Model):
    series = models.CharField(max_length=32)  # KOSPI/KOSDAQ/SP500/...
    ts     = models.DateTimeField()
    value  = models.DecimalField(max_digits=16, decimal_places=4)

    class Meta:
        db_table = "macro_indices"
        unique_together = (("series", "ts"),)
        indexes = [models.Index(fields=["series", "ts"])]
'''