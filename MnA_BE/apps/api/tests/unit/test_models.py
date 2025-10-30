from django.test import TestCase
from django.db import IntegrityError
from django.utils import timezone
from apps.api.models import (
    RecommendationBatch,
    RecommendationItem,
    BatchLevel,
    Audience,
    Regime,
    Direction,
)
import datetime


class RecommendationBatchModelTests(TestCase):
    """
    RecommendationBatch 모델 유닛 테스트
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.market_date = datetime.date(2025, 1, 15)
    
    def test_create_batch_minimal(self):
        """최소 필드로 배치 생성"""
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date
        )
        
        self.assertIsNotNone(batch.id)
        self.assertEqual(batch.market_date, self.market_date)
        self.assertEqual(batch.source, "llm")  # 기본값
        self.assertEqual(batch.level, BatchLevel.GLOBAL)  # 기본값
        self.assertEqual(batch.audience, Audience.GENERAL)  # 기본값
    
    def test_create_batch_full(self):
        """전체 필드로 배치 생성"""
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date,
            risk_profile="공격투자형",
            source="llm",
            model_id="gpt-4",
            level=BatchLevel.INDUSTRY,
            industry_tag="반도체",
            audience=Audience.PERSONALIZED,
            regime=Regime.RISK_ON,
            inputs_hash="abc123",
            prompt_version="v2.1",
            notes={"kospi": 2500, "kosdaq": 800},
        )
        
        self.assertIsNotNone(batch.id)
        self.assertEqual(batch.risk_profile, "공격투자형")
        self.assertEqual(batch.level, BatchLevel.INDUSTRY)
        self.assertEqual(batch.industry_tag, "반도체")
        self.assertEqual(batch.audience, Audience.PERSONALIZED)
        self.assertEqual(batch.regime, Regime.RISK_ON)
        self.assertIsInstance(batch.notes, dict)
    
    def test_batch_str_representation(self):
        """__str__ 메서드"""
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date,
            risk_profile="공격투자형",
            level=BatchLevel.INDUSTRY,
            industry_tag="IT"
        )
        
        str_repr = str(batch)
        
        self.assertIn("2025-01-15", str_repr)
        self.assertIn("industry", str_repr)
        self.assertIn("공격투자형", str_repr)
        self.assertIn("IT", str_repr)
    
    def test_batch_unique_constraint(self):
        """같은 날짜/프로필/레벨/업종/대상 조합은 중복 불가"""
        RecommendationBatch.objects.create(
            market_date=self.market_date,
            risk_profile="공격투자형",
            level=BatchLevel.GLOBAL,
            industry_tag="IT",  # NULL이 아닌 값
            audience=Audience.GENERAL,
        )
        
        # 같은 조합으로 다시 생성 시도
        with self.assertRaises(IntegrityError):
            RecommendationBatch.objects.create(
                market_date=self.market_date,
                risk_profile="공격투자형",
                level=BatchLevel.GLOBAL,
                industry_tag="IT",  # 같은 값
                audience=Audience.GENERAL,
            )
    
    def test_batch_different_risk_profile_allowed(self):
        """다른 risk_profile이면 중복 허용"""
        RecommendationBatch.objects.create(
            market_date=self.market_date,
            risk_profile="공격투자형",
        )
        
        batch2 = RecommendationBatch.objects.create(
            market_date=self.market_date,
            risk_profile="안정추구형",
        )
        
        self.assertIsNotNone(batch2.id)
    
    def test_batch_different_date_allowed(self):
        """다른 날짜면 중복 허용"""
        RecommendationBatch.objects.create(
            market_date=self.market_date,
            risk_profile="공격투자형",
        )
        
        batch2 = RecommendationBatch.objects.create(
            market_date=self.market_date + datetime.timedelta(days=1),
            risk_profile="공격투자형",
        )
        
        self.assertIsNotNone(batch2.id)
    
    def test_batch_timestamps(self):
        """created_at과 updated_at 자동 생성"""
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date
        )
        
        self.assertIsNotNone(batch.created_at)
        self.assertIsNotNone(batch.updated_at)
        self.assertIsInstance(batch.created_at, datetime.datetime)
        self.assertIsInstance(batch.updated_at, datetime.datetime)
    
    def test_batch_as_of_utc_default(self):
        """as_of_utc 기본값"""
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date
        )
        
        self.assertIsNotNone(batch.as_of_utc)
        self.assertIsInstance(batch.as_of_utc, datetime.datetime)
    
    def test_batch_notes_json_field(self):
        """notes JSON 필드"""
        notes_data = {
            "kospi": 2500,
            "kosdaq": 800,
            "overview": "시장 상승세"
        }
        
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date,
            notes=notes_data
        )
        
        # DB에서 다시 조회
        batch_from_db = RecommendationBatch.objects.get(id=batch.id)
        
        self.assertEqual(batch_from_db.notes, notes_data)
        self.assertEqual(batch_from_db.notes["kospi"], 2500)
    
    def test_batch_level_choices(self):
        """level 필드 선택지"""
        # MARKET
        batch1 = RecommendationBatch.objects.create(
            market_date=self.market_date,
            level=BatchLevel.MARKET,
        )
        self.assertEqual(batch1.level, BatchLevel.MARKET)
        
        # INDUSTRY
        batch2 = RecommendationBatch.objects.create(
            market_date=self.market_date + datetime.timedelta(days=1),
            level=BatchLevel.INDUSTRY,
            industry_tag="반도체",
        )
        self.assertEqual(batch2.level, BatchLevel.INDUSTRY)
    
    def test_batch_audience_choices(self):
        """audience 필드 선택지"""
        # GENERAL
        batch1 = RecommendationBatch.objects.create(
            market_date=self.market_date,
            audience=Audience.GENERAL,
        )
        self.assertEqual(batch1.audience, Audience.GENERAL)
        
        # PERSONALIZED
        batch2 = RecommendationBatch.objects.create(
            market_date=self.market_date + datetime.timedelta(days=1),
            audience=Audience.PERSONALIZED,
        )
        self.assertEqual(batch2.audience, Audience.PERSONALIZED)
    
    def test_batch_regime_choices(self):
        """regime 필드 선택지"""
        batch = RecommendationBatch.objects.create(
            market_date=self.market_date,
            regime=Regime.RISK_ON,
        )
        
        self.assertEqual(batch.regime, Regime.RISK_ON)


class RecommendationItemModelTests(TestCase):
    """
    RecommendationItem 모델 유닛 테스트
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.batch = RecommendationBatch.objects.create(
            market_date=datetime.date(2025, 1, 15),
            risk_profile="공격투자형",
        )
    
    def test_create_item_minimal(self):
        """최소 필드로 아이템 생성"""
        item = RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="005930",
            name="삼성전자",
        )
        
        self.assertIsNotNone(item.id)
        self.assertEqual(item.batch, self.batch)
        self.assertEqual(item.rank, 1)
        self.assertEqual(item.ticker, "005930")
        self.assertEqual(item.name, "삼성전자")
        self.assertEqual(item.market, "KOSPI")  # 기본값
        self.assertEqual(item.expected_direction, Direction.NEUTRAL)  # 기본값
        self.assertEqual(item.conviction, 0.5)  # 기본값
    
    def test_create_item_full(self):
        """전체 필드로 아이템 생성"""
        news_data = [
            {"title": "뉴스1", "url": "http://ex.com/1", "source": "ex"},
            {"title": "뉴스2", "url": "http://ex.com/2", "source": "ex"},
        ]
        reason_data = ["레짐: Risk On", "밸류: 저평가", "모멘텀: 상승"]
        
        item = RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="005930",
            name="삼성전자",
            market="KOSPI",
            news=news_data,
            reason=reason_data,
            expected_direction=Direction.UP,
            conviction=0.85,
            score=0.923,
            score_breakdown={"quality": 0.9, "value": 0.8, "momentum": 0.95},
        )
        
        self.assertEqual(item.news, news_data)
        self.assertEqual(item.reason, reason_data)
        self.assertEqual(item.expected_direction, Direction.UP)
        self.assertEqual(item.conviction, 0.85)
        self.assertEqual(float(item.score), 0.923)
    
    def test_item_str_representation(self):
        """__str__ 메서드"""
        item = RecommendationItem.objects.create(
            batch=self.batch,
            rank=2,
            ticker="000660",
            name="SK하이닉스",
        )
        
        str_repr = str(item)
        
        self.assertIn(str(self.batch.id), str_repr)
        self.assertIn("2", str_repr)  # rank
        self.assertIn("000660", str_repr)  # ticker
    
    def test_item_ordering(self):
        """rank 순서대로 정렬"""
        item3 = RecommendationItem.objects.create(
            batch=self.batch, rank=3, ticker="T3", name="N3"
        )
        item1 = RecommendationItem.objects.create(
            batch=self.batch, rank=1, ticker="T1", name="N1"
        )
        item2 = RecommendationItem.objects.create(
            batch=self.batch, rank=2, ticker="T2", name="N2"
        )
        
        items = list(RecommendationItem.objects.all())
        
        self.assertEqual(items[0].rank, 1)
        self.assertEqual(items[1].rank, 2)
        self.assertEqual(items[2].rank, 3)
    
    def test_item_unique_batch_rank(self):
        """같은 batch에서 rank 중복 불가"""
        RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="005930",
            name="삼성전자",
        )
        
        with self.assertRaises(IntegrityError):
            RecommendationItem.objects.create(
                batch=self.batch,
                rank=1,  # 중복 rank
                ticker="000660",
                name="SK하이닉스",
            )
    
    def test_item_unique_batch_ticker(self):
        """같은 batch에서 ticker 중복 불가"""
        RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="005930",
            name="삼성전자",
        )
        
        with self.assertRaises(IntegrityError):
            RecommendationItem.objects.create(
                batch=self.batch,
                rank=2,
                ticker="005930",  # 중복 ticker
                name="삼성전자",
            )
    
    def test_item_rank_must_be_positive(self):
        """rank는 1 이상이어야 함"""
        with self.assertRaises(IntegrityError):
            RecommendationItem.objects.create(
                batch=self.batch,
                rank=0,  # 0은 불가
                ticker="005930",
                name="삼성전자",
            )
    
    def test_item_conviction_range(self):
        """conviction은 0.0 ~ 1.0 범위"""
        # 유효한 값
        item1 = RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="T1",
            name="N1",
            conviction=0.0,
        )
        self.assertEqual(item1.conviction, 0.0)
        
        item2 = RecommendationItem.objects.create(
            batch=self.batch,
            rank=2,
            ticker="T2",
            name="N2",
            conviction=1.0,
        )
        self.assertEqual(item2.conviction, 1.0)
        
        # 범위 밖 값
        with self.assertRaises(IntegrityError):
            RecommendationItem.objects.create(
                batch=self.batch,
                rank=3,
                ticker="T3",
                name="N3",
                conviction=1.5,  # > 1.0
            )
    
    def test_item_related_name(self):
        """batch.items로 역참조 가능"""
        RecommendationItem.objects.create(
            batch=self.batch, rank=1, ticker="T1", name="N1"
        )
        RecommendationItem.objects.create(
            batch=self.batch, rank=2, ticker="T2", name="N2"
        )
        
        items = self.batch.items.all()
        
        self.assertEqual(items.count(), 2)
        self.assertEqual(items[0].rank, 1)
        self.assertEqual(items[1].rank, 2)
    
    def test_item_cascade_delete(self):
        """batch 삭제 시 items도 삭제"""
        RecommendationItem.objects.create(
            batch=self.batch, rank=1, ticker="T1", name="N1"
        )
        RecommendationItem.objects.create(
            batch=self.batch, rank=2, ticker="T2", name="N2"
        )
        
        batch_id = self.batch.id
        self.batch.delete()
        
        # items도 삭제되었는지 확인
        items = RecommendationItem.objects.filter(batch_id=batch_id)
        self.assertEqual(items.count(), 0)
    
    def test_item_news_json_field(self):
        """news JSON 필드"""
        news_data = [
            {"title": "뉴스1", "url": "http://ex.com/1", "source": "source1"},
            {"title": "뉴스2", "url": "http://ex.com/2", "source": "source2"},
        ]
        
        item = RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="005930",
            name="삼성전자",
            news=news_data,
        )
        
        item_from_db = RecommendationItem.objects.get(id=item.id)
        
        self.assertEqual(item_from_db.news, news_data)
        self.assertEqual(len(item_from_db.news), 2)
        self.assertEqual(item_from_db.news[0]["title"], "뉴스1")
    
    def test_item_reason_json_field(self):
        """reason JSON 필드"""
        reason_data = ["이유1", "이유2", "이유3"]
        
        item = RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="005930",
            name="삼성전자",
            reason=reason_data,
        )
        
        item_from_db = RecommendationItem.objects.get(id=item.id)
        
        self.assertEqual(item_from_db.reason, reason_data)
        self.assertEqual(len(item_from_db.reason), 3)
    
    def test_item_direction_choices(self):
        """expected_direction 필드 선택지"""
        # UP
        item1 = RecommendationItem.objects.create(
            batch=self.batch,
            rank=1,
            ticker="T1",
            name="N1",
            expected_direction=Direction.UP,
        )
        self.assertEqual(item1.expected_direction, Direction.UP)
        
        # DOWN
        item2 = RecommendationItem.objects.create(
            batch=self.batch,
            rank=2,
            ticker="T2",
            name="N2",
            expected_direction=Direction.DOWN,
        )
        self.assertEqual(item2.expected_direction, Direction.DOWN)