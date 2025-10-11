from django.db import models

TINY_TEXT = 255

class User(models.Model):
    id = models.CharField(primary_key=True, editable=False, max_length=36)
    password = models.CharField(max_length=TINY_TEXT)
    name = models.CharField(max_length=TINY_TEXT, default="")
    refresh_token = models.CharField(max_length=TINY_TEXT, default="")

class Interests(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    interests = models.JSONField()
    create_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-create_at"]
