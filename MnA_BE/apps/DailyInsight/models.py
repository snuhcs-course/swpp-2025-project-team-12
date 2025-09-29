from django.db import models

TINY_TEXT = 255

class PriceFluctuation(models.Model):
    recommended_date = models.DateField()

class Recommendation(models.Model):
    stock_info = models.TextField()
    reason = models.TextField()
    links = models.JSONField()
    date = models.DateField()

class StockInfo(models.Model):
    ticker = models.CharField(max_length=TINY_TEXT)
    company_name = models.CharField(max_length=TINY_TEXT)
    market_type = models.CharField(max_length=TINY_TEXT)
    industry_tag = models.CharField(max_length=TINY_TEXT)
    company_profile = models.TextField()