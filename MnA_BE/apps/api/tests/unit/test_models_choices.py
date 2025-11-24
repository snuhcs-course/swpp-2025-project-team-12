# apps/api/tests/unit/test_models_choices.py
"""
Model Choices 클래스 단위 테스트
외부 의존성 없음
"""

from django.test import TestCase


class ModelChoicesTests(TestCase):
    """Choices 클래스 테스트"""

    def test_batch_level_choices(self):
        """BatchLevel choices"""
        from apps.api.models import BatchLevel

        self.assertEqual(BatchLevel.MARKET, "market")
        self.assertEqual(BatchLevel.INDUSTRY, "industry")
        self.assertEqual(BatchLevel.GLOBAL, "global")

        # choices 확인
        choices = BatchLevel.choices
        self.assertEqual(len(choices), 3)

    def test_audience_choices(self):
        """Audience choices"""
        from apps.api.models import Audience

        self.assertEqual(Audience.GENERAL, "general")
        self.assertEqual(Audience.PERSONALIZED, "personalized")

        choices = Audience.choices
        self.assertEqual(len(choices), 2)

    def test_regime_choices(self):
        """Regime choices"""
        from apps.api.models import Regime

        self.assertEqual(Regime.RISK_ON, "market_risk_on")
        self.assertEqual(Regime.NEUTRAL, "market_neutral")
        self.assertEqual(Regime.RISK_OFF, "market_risk_off")

        choices = Regime.choices
        self.assertEqual(len(choices), 3)

    def test_direction_choices(self):
        """Direction choices"""
        from apps.api.models import Direction

        self.assertEqual(Direction.UP, "up")
        self.assertEqual(Direction.NEUTRAL, "neutral")
        self.assertEqual(Direction.DOWN, "down")

        choices = Direction.choices
        self.assertEqual(len(choices), 3)
