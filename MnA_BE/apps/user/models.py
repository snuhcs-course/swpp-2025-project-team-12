from django.db import models

TINY_TEXT = 255

class User(models.Model):
    id = models.AutoField(primary_key=True, editable=False) # integer increment
    password = models.CharField(max_length=TINY_TEXT)
    name = models.CharField(max_length=TINY_TEXT, unique=True, editable=False)
    refresh_token = models.CharField(max_length=TINY_TEXT, default="")

class Style(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    interests = models.JSONField()
    strategy = models.JSONField()
    create_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-create_at"]
