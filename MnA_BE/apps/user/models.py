from django.db import models

TINY_TEXT = 255
LONG_TEXT = 255

class User(models.Model):
    id = models.AutoField(primary_key=True)
    password = models.CharField(max_length=TINY_TEXT)
    name = models.CharField(max_length=TINY_TEXT)

    # bitmask string for each tag(On / Off)
    interests = models.CharField(max_length=LONG_TEXT)