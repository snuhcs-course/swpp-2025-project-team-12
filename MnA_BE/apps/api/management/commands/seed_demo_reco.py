# apps/api/management/commands/seed_demo_reco.py
from django.core.management.base import BaseCommand
from django.utils import timezone
from datetime import datetime
import pytz
from apps.api.models import RecommendationBatch, RecommendationItem

KST = pytz.timezone("Asia/Seoul")

class Command(BaseCommand):
    help = "Seed a demo recommendation batch with one item"

    def add_arguments(self, parser):
        parser.add_argument("--risk", default="공격투자형")

    def handle(self, *args, **opts):
        risk = opts["risk"]
        market_date = datetime.now(KST).date()

        batch, created = RecommendationBatch.objects.get_or_create(
            market_date=market_date, risk_profile=risk,
            defaults=dict(source="mock", model_id="demo-seed",
                          as_of_utc=timezone.now(), notes="iter2 demo seed"),
        )
        if not created:
            batch.items.all().delete()

        # 예시: 포스코스틸리온 1개
        RecommendationItem.objects.create(
            batch=batch,
            rank=1,
            ticker="058430",
            name="포스코스틸리온",
            news_titles=["포스코, 中 거점 팔고 인니로…동남아 철강시장 뚫는다"],
            news_urls=["https://example.com/news1"],
            reason=[
                "저평가된 밸류에이션과 높은 배당성향",
                "동남아 생산기지 확대로 글로벌 경쟁력 강화 가능"
            ],
            score=None,
        )
        self.stdout.write(self.style.SUCCESS(
            f"Seeded batch for {market_date} / {risk} with 1 item"))
