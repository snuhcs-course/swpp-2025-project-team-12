# apps/api/models.py
from django.db import models
from django.utils import timezone


class BatchLevel(models.TextChoices):
    MARKET = "market", "Market"     # 1단계: 지수/센티먼트 요약
    INDUSTRY = "industry", "Industry"  # 2단계: 업종별 추천
    GLOBAL = "global", "Global"     # 3단계: 전체 통합 Top-N


class Audience(models.TextChoices):
    GENERAL = "general", "General"
    PERSONALIZED = "personalized", "Personalized"


class Regime(models.TextChoices):
    RISK_ON = "market_risk_on", "Risk On"
    NEUTRAL = "market_neutral", "Neutral"
    RISK_OFF = "market_risk_off", "Risk Off"


class Direction(models.TextChoices):
    UP = "up", "Up"
    NEUTRAL = "neutral", "Neutral"
    DOWN = "down", "Down"


class RecommendationBatch(models.Model):
    market_date   = models.DateField(help_text="KST 기준 추천 일자")
    risk_profile  = models.CharField(max_length=32, null=True, blank=True)  # 안정형/중립형/공격투자형 등(1단계는 없음)
    source        = models.CharField(max_length=16, default="llm")
    model_id      = models.CharField(max_length=64, null=True, blank=True)
    as_of_utc     = models.DateTimeField(default=timezone.now)

    # v2 추가 메타
    level         = models.CharField(max_length=16, choices=BatchLevel.choices, default=BatchLevel.GLOBAL)
    industry_tag  = models.CharField(max_length=64, null=True, blank=True)  # level=industry일 때 필수
    audience      = models.CharField(max_length=16, choices=Audience.choices, default=Audience.GENERAL)
    regime        = models.CharField(max_length=32, choices=Regime.choices, null=True, blank=True)
    inputs_hash   = models.CharField(max_length=64, null=True, blank=True)
    prompt_version= models.CharField(max_length=32, null=True, blank=True)

    # 1단계(지수 요약)용 여지: overview/kospi/kosdaq/news_used 직렬화 저장 가능
    notes         = models.JSONField(null=True, blank=True)

    # 타임스탬프
    created_at    = models.DateTimeField(auto_now_add=True)
    updated_at    = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "recommendation_batches"
        constraints = [
            # 하루/성향/레벨/업종/대상 조합 유니크 (개인화 확장 시 user_id 추가 권장)
            models.UniqueConstraint(
                fields=["market_date", "risk_profile", "level", "industry_tag", "audience"],
                name="uq_batch_day_profile_level_industry_audience",
            ),
        ]
        indexes = [
            models.Index(fields=["market_date", "level", "audience"]),
            models.Index(fields=["market_date", "risk_profile"]),
        ]

    def __str__(self):
        return f"{self.market_date} {self.level} {self.risk_profile or '-'} {self.industry_tag or '-'}"


class RecommendationItem(models.Model):
    batch       = models.ForeignKey(RecommendationBatch, on_delete=models.CASCADE, related_name="items")
    rank        = models.IntegerField()
    ticker      = models.CharField(max_length=16)
    name        = models.CharField(max_length=128)
    market      = models.CharField(max_length=10, default="KOSPI")  # KOSPI/KOSDAQ
    # v2: 평행 배열 제거 → 단일 news(JSON)
    news        = models.JSONField(default=list)  # [{title,url,source}]
    reason      = models.JSONField(default=list)  # ["레짐/성향", "밸류/퀄리티/모멘텀", "리스크/트리거"]
    expected_direction = models.CharField(max_length=10, choices=Direction.choices, default=Direction.NEUTRAL)
    conviction  = models.FloatField(default=0.5)
    score       = models.DecimalField(max_digits=6, decimal_places=3, null=True, blank=True)
    # (선택) 상세분해 점수
    score_breakdown = models.JSONField(null=True, blank=True)  # {"quality":..., "value":..., ...}

    class Meta:
        db_table = "recommendation_items"
        ordering = ["rank"]
        constraints = [
            models.UniqueConstraint(fields=["batch", "rank"], name="uq_item_batch_rank"),
            models.UniqueConstraint(fields=["batch", "ticker"], name="uq_item_batch_ticker"),
            models.CheckConstraint(check=models.Q(rank__gte=1), name="ck_item_rank_ge_1"),
            models.CheckConstraint(check=models.Q(conviction__gte=0.0) & models.Q(conviction__lte=1.0),
                                   name="ck_item_conviction_0_1"),
        ]
        indexes = [
            models.Index(fields=["batch", "rank"]),
            models.Index(fields=["ticker"]),
        ]

    def __str__(self):
        return f"{self.batch_id}#{self.rank} {self.ticker}"
