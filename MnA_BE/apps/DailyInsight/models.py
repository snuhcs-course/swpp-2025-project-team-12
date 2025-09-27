from django.db import models
from MnA_BE.constants import *

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