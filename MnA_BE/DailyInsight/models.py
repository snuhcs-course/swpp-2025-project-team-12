from django.db import models

TINY_TEXT = 255
LONG_TEXT = 65535

# Create your models here.
class User(models.Model):
    id = models.AutoField(primary_key=True)
    password = models.CharField(max_length=TINY_TEXT)
    name = models.CharField(max_length=TINY_TEXT)

    # bitmask string for each tag(On / Off)
    interests = models.CharField(max_length=LONG_TEXT)

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
