# apps/api/tests/integration/test_models.py
"""
Django Model 통합 테스트 (DB 사용)
RecommendationBatch, RecommendationItem
"""

from django.test import TestCase
from datetime import date
from decimal import Decimal


class RecommendationBatchModelTests(TestCase):
    """apps/api/models.py - RecommendationBatch 테스트"""

    def test_create_batch_basic(self):
        """기본 배치 생성"""
        from apps.api.models import RecommendationBatch, BatchLevel, Audience

        batch = RecommendationBatch.objects.create(
            market_date=date(2025, 1, 15), level=BatchLevel.GLOBAL, audience=Audience.GENERAL
        )

        self.assertEqual(batch.market_date, date(2025, 1, 15))
        self.assertEqual(batch.level, BatchLevel.GLOBAL)
        self.assertEqual(batch.audience, Audience.GENERAL)
        self.assertEqual(batch.source, "llm")

    def test_create_batch_with_all_fields(self):
        """모든 필드를 포함한 배치 생성"""
        from apps.api.models import RecommendationBatch, BatchLevel, Audience, Regime

        batch = RecommendationBatch.objects.create(
            market_date=date(2025, 1, 15),
            risk_profile="aggressive",
            source="custom_llm",
            model_id="gpt-4",
            level=BatchLevel.INDUSTRY,
            industry_tag="technology",
            audience=Audience.PERSONALIZED,
            regime=Regime.RISK_ON,
            inputs_hash="abc123",
            prompt_version="v2.0",
            notes={"overview": "Market bullish"},
        )

        self.assertEqual(batch.risk_profile, "aggressive")
        self.assertEqual(batch.model_id, "gpt-4")
        self.assertEqual(batch.industry_tag, "technology")
        self.assertEqual(batch.regime, Regime.RISK_ON)
        self.assertIsNotNone(batch.notes)

    def test_batch_str_representation(self):
        """__str__ 메서드"""
        from apps.api.models import RecommendationBatch, BatchLevel, Audience

        batch = RecommendationBatch.objects.create(
            market_date=date(2025, 1, 15),
            risk_profile="moderate",
            level=BatchLevel.INDUSTRY,
            industry_tag="finance",
            audience=Audience.GENERAL,
        )

        str_repr = str(batch)
        self.assertIn("2025-01-15", str_repr)
        self.assertIn("industry", str_repr)
        self.assertIn("moderate", str_repr)
        self.assertIn("finance", str_repr)

    def test_batch_str_with_none_fields(self):
        """__str__ - risk_profile과 industry_tag가 None일 때"""
        from apps.api.models import RecommendationBatch, BatchLevel, Audience

        batch = RecommendationBatch.objects.create(
            market_date=date(2025, 1, 15), level=BatchLevel.GLOBAL, audience=Audience.GENERAL
        )

        str_repr = str(batch)
        self.assertIn("2025-01-15", str_repr)
        self.assertIn("-", str_repr)  # None은 '-'로 표시

    def test_batch_timestamps(self):
        """created_at, updated_at 자동 설정"""
        from apps.api.models import RecommendationBatch

        batch = RecommendationBatch.objects.create(market_date=date(2025, 1, 15))

        self.assertIsNotNone(batch.created_at)
        self.assertIsNotNone(batch.updated_at)
        self.assertIsNotNone(batch.as_of_utc)


class RecommendationItemModelTests(TestCase):
    """apps/api/models.py - RecommendationItem 테스트"""

    def setUp(self):
        """테스트용 배치 생성"""
        from apps.api.models import RecommendationBatch, BatchLevel, Audience

        self.batch = RecommendationBatch.objects.create(
            market_date=date(2025, 1, 15), level=BatchLevel.GLOBAL, audience=Audience.GENERAL
        )

    def test_create_item_basic(self):
        """기본 아이템 생성"""
        from apps.api.models import RecommendationItem, Direction

        item = RecommendationItem.objects.create(
            batch=self.batch, rank=1, ticker="005930", name="삼성전자", market="KOSPI"
        )

        self.assertEqual(item.rank, 1)
        self.assertEqual(item.ticker, "005930")
        self.assertEqual(item.name, "삼성전자")
        self.assertEqual(item.expected_direction, Direction.NEUTRAL)
        self.assertEqual(item.conviction, 0.5)

    def test_create_item_with_all_fields(self):
        """모든 필드를 포함한 아이템 생성"""
        from apps.api.models import RecommendationItem, Direction

        item = RecommendationItem.objects.create(
            batch=self.batch,
            rank=2,
            ticker="000660",
            name="SK하이닉스",
            market="KOSPI",
            news=[{"title": "News 1", "url": "http://example.com"}],
            reason=["Reason 1", "Reason 2"],
            expected_direction=Direction.UP,
            conviction=0.85,
            score=Decimal("95.500"),
            score_breakdown={"quality": 90, "value": 95},
        )

        self.assertEqual(item.rank, 2)
        self.assertEqual(item.expected_direction, Direction.UP)
        self.assertEqual(item.conviction, 0.85)
        self.assertEqual(item.score, Decimal("95.500"))
        self.assertIsNotNone(item.news)
        self.assertIsNotNone(item.reason)
        self.assertIsNotNone(item.score_breakdown)

    def test_item_str_representation(self):
        """__str__ 메서드"""
        from apps.api.models import RecommendationItem

        item = RecommendationItem.objects.create(
            batch=self.batch, rank=3, ticker="035720", name="카카오"
        )

        str_repr = str(item)
        self.assertIn("035720", str_repr)
        self.assertIn("#3", str_repr)

    def test_item_ordering(self):
        """ordering by rank"""
        from apps.api.models import RecommendationItem

        item3 = RecommendationItem.objects.create(batch=self.batch, rank=3, ticker="C", name="C")
        item1 = RecommendationItem.objects.create(batch=self.batch, rank=1, ticker="A", name="A")
        item2 = RecommendationItem.objects.create(batch=self.batch, rank=2, ticker="B", name="B")

        items = list(RecommendationItem.objects.all())
        self.assertEqual(items[0].rank, 1)
        self.assertEqual(items[1].rank, 2)
        self.assertEqual(items[2].rank, 3)

    def test_item_related_name(self):
        """batch.items로 접근 가능"""
        from apps.api.models import RecommendationItem

        RecommendationItem.objects.create(batch=self.batch, rank=1, ticker="A", name="A")
        RecommendationItem.objects.create(batch=self.batch, rank=2, ticker="B", name="B")

        items = self.batch.items.all()
        self.assertEqual(items.count(), 2)
