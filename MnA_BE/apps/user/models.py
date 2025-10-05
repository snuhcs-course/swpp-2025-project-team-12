from django.db import models

TINY_TEXT = 255

class User(models.Model):
    # dev 쪽 유지: AutoField (이미 배포/마이그레이션과 충돌 최소)
    id = models.AutoField(primary_key=True)

    password = models.CharField(max_length=TINY_TEXT)
    # 이름은 기본값 허용(HEAD에서 default 있었음)
    name = models.CharField(max_length=TINY_TEXT, default="")

    # HEAD에서 가져옴: refresh_token (선택 필드로)
    refresh_token = models.CharField(max_length=TINY_TEXT, default="", blank=True)

    # dev 쪽의 interests를 유지하되, 경고 해결: default=dict() → default=list
    # 리스트 태그 저장 형태 권장: ["AI","2차전지", ...]
    interests = models.JSONField(default=list)
