from django.db import models

TINY_TEXT = 255

class User(models.Model):
    id = models.CharField(primary_key=True, editable=False, max_length=36)
    password = models.CharField(max_length=TINY_TEXT)
    name = models.CharField(max_length=TINY_TEXT, default="")
    interests = models.JSONField(default=dict())