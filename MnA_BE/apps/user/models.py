from django.db import models
import uuid

TINY_TEXT = 255

class User(models.Model):
    id = models.CharField(
        primary_key=True, 
        default=uuid.uuid4,  # UUID 자동 생성
        editable=False, 
        max_length=36
    )
    password = models.CharField(max_length=TINY_TEXT)
    name = models.CharField(max_length=TINY_TEXT, unique=True)  # unique 추가 권장
    refresh_token = models.CharField(max_length=TINY_TEXT, blank=True, default="")

class Style(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    interests = models.JSONField()
    strategy = models.JSONField()
    create_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-create_at"]