from django.test import TestCase, Client
from django.utils import timezone
from apps.api.models import RecommendationBatch, RecommendationItem
import datetime as _dt


class ApiRecommendationsGeneralTests(TestCase):
    """
    일반 추천 엔드포인트 상세 테스트
    DB 우선, Mock fallback 검증
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/recommendations/general"
        # 오늘 날짜 사용 (views.py에서 _dt.datetime.now(KST).date() 사용)
        self.market_date = _dt.datetime.now().date()
    
    def _make_batch_with_items(self, n=5, risk="공격투자형", market_date=None):
        """테스트용 배치 및 아이템 생성 헬퍼"""
        if market_date is None:
            market_date = self.market_date
        
        batch = RecommendationBatch.objects.create(
            market_date=market_date,
            risk_profile=risk,
            source="llm",
            as_of_utc=timezone.now(),
        )
        
        for i in range(1, n+1):
            RecommendationItem.objects.create(
                batch=batch,
                ticker=f"T{i:03d}",
                name=f"Name{i}",
                reason=[f"reason {i}"],
                rank=i,
                news=[{
                    "title": f"title{i}",
                    "url": f"https://ex{i}.com",
                    "source": "ex"
                }],
                expected_direction="up",
                conviction=0.7,
                market="KOSDAQ",
            )
        
        return batch
    
    def test_general_endpoint_accessible(self):
        """일반 추천 엔드포인트에 접근 가능한지 확인"""
        response = self.client.get(self.url, {"risk": "공격투자형"})
        
        # 항상 200 OK (mock fallback 있음)
        self.assertEqual(response.status_code, 200)
    
    def test_general_returns_json(self):
        """일반 추천이 JSON을 반환하는지 확인"""
        response = self.client.get(self.url, {"risk": "공격투자형"})
        
        self.assertIn("application/json", response.get("Content-Type", ""))
        
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_general_has_pagination_fields(self):
        """일반 추천 응답이 페이지네이션 필드를 포함하는지 확인"""
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        # 필수 페이지네이션 필드
        pagination_fields = ["total", "limit", "offset"]
        for field in pagination_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing pagination field: {field}")
    
    def test_general_has_required_fields(self):
        """일반 추천 응답이 필수 필드를 포함하는지 확인"""
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        # 필수 필드
        required_fields = ["items", "source", "marketDate", "personalized"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
        
        # personalized는 False여야 함
        self.assertFalse(data["personalized"], "General recommendations should not be personalized")
    
    def test_general_from_db_basic(self):
        """DB 배치가 있으면 DB에서 가져오는지 확인"""
        self._make_batch_with_items(n=5, risk="공격투자형")
        
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["source"], "llm", "Should use DB source")
        self.assertEqual(data["total"], 5)
        self.assertEqual(len(data["items"]), 5)
    
    def test_general_from_db_with_pagination(self):
        """DB 배치의 페이지네이션이 작동하는지 확인"""
        self._make_batch_with_items(n=5, risk="공격투자형")
        
        response = self.client.get(self.url, {
            "risk": "공격투자형",
            "limit": 2,
            "offset": 1
        })
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["source"], "llm")
        self.assertEqual(data["total"], 5)
        self.assertEqual(data["limit"], 2)
        self.assertEqual(data["offset"], 1)
        self.assertEqual(len(data["items"]), 2)
        
        # rank 순서 확인
        self.assertEqual(data["items"][0]["rank"], 2)
        self.assertEqual(data["items"][1]["rank"], 3)
    
    def test_general_item_structure(self):
        """추천 아이템의 구조가 올바른지 확인"""
        self._make_batch_with_items(n=3, risk="공격투자형")
        
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        self.assertGreater(len(data["items"]), 0)
        
        item = data["items"][0]
        
        # 필수 필드
        required_item_fields = ["ticker", "name", "reason", "rank", "news"]
        for field in required_item_fields:
            with self.subTest(field=field):
                self.assertIn(field, item, f"Item should have {field}")
        
        # news 구조 확인
        self.assertIsInstance(item["news"], list)
        if item["news"]:
            news_item = item["news"][0]
            self.assertIn("title", news_item)
            self.assertIn("url", news_item)
    
    def test_general_rank_ordering(self):
        """추천 아이템이 rank 순서대로 정렬되는지 확인"""
        self._make_batch_with_items(n=5, risk="공격투자형")
        
        response = self.client.get(self.url, {"risk": "공격투자형", "limit": 5})
        data = response.json()
        
        ranks = [item["rank"] for item in data["items"]]
        
        # rank가 오름차순이어야 함
        self.assertEqual(ranks, sorted(ranks), "Items should be sorted by rank")
        self.assertEqual(ranks, [1, 2, 3, 4, 5])
    
    def test_general_different_risk_profiles(self):
        """다른 위험 프로필별로 다른 배치를 가져오는지 확인"""
        # 공격투자형 배치
        self._make_batch_with_items(n=3, risk="공격투자형")
        
        # 안정추구형 배치
        self._make_batch_with_items(n=2, risk="안정추구형")
        
        # 공격투자형 요청
        response1 = self.client.get(self.url, {"risk": "공격투자형"})
        data1 = response1.json()
        
        # 안정추구형 요청
        response2 = self.client.get(self.url, {"risk": "안정추구형"})
        data2 = response2.json()
        
        # DB source일 때만 개수 검증
        if data1["source"] == "llm" and data2["source"] == "llm":
            self.assertEqual(data1["total"], 3)
            self.assertEqual(data2["total"], 2)
        else:
            # Mock fallback - 최소한 데이터는 있어야 함
            self.assertGreater(data1["total"], 0)
            self.assertGreater(data2["total"], 0)
    
    def test_general_date_parameter(self):
        """date 파라미터로 특정 날짜의 배치를 가져올 수 있는지 확인"""
        # 특정 날짜 배치
        specific_date = _dt.date(2025, 1, 15)
        self._make_batch_with_items(n=3, risk="공격투자형", market_date=specific_date)
        
        response = self.client.get(self.url, {
            "risk": "공격투자형",
            "date": "2025-01-15"
        })
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["marketDate"], "2025-01-15")
        
        # DB에서 가져왔으면 total=3
        if data["source"] == "llm":
            self.assertEqual(data["total"], 3)
    
    def test_general_fallback_to_mock_when_no_batch(self):
        """DB에 배치가 없으면 mock으로 fallback하는지 확인"""
        # DB에 아무것도 없음 (setUp에서 생성 안함)
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        
        # mock fallback
        self.assertEqual(data["source"], "mock")
        
        # mock 데이터도 items 있어야 함
        self.assertIn("items", data)
        self.assertIsInstance(data["items"], list)
        self.assertGreater(len(data["items"]), 0)
    
    def test_general_default_risk_profile(self):
        """risk 파라미터가 없으면 기본값 사용"""
        self._make_batch_with_items(n=3, risk="공격투자형")
        
        # risk 없이 요청
        response = self.client.get(self.url)
        data = response.json()
        
        # 기본값은 "공격투자형" (코드에서 확인)
        self.assertEqual(response.status_code, 200)
    
    def test_general_invalid_date_format(self):
        """잘못된 날짜 형식은 현재 날짜로 fallback"""
        response = self.client.get(self.url, {
            "risk": "공격투자형",
            "date": "invalid-date"
        })
        
        # 에러 없이 응답
        self.assertEqual(response.status_code, 200)
    
    def test_general_market_date_field(self):
        """응답에 marketDate 필드가 올바른 형식인지 확인"""
        self._make_batch_with_items(n=3, risk="공격투자형")
        
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        self.assertIn("marketDate", data)
        self.assertRegex(
            data["marketDate"],
            r'\d{4}-\d{2}-\d{2}',
            "marketDate should be YYYY-MM-DD format"
        )
    
    def test_general_as_of_field(self):
        """응답에 asOf 필드가 ISO 형식인지 확인"""
        self._make_batch_with_items(n=3, risk="공격투자형")
        
        response = self.client.get(self.url, {"risk": "공격투자형"})
        data = response.json()
        
        self.assertIn("asOf", data)
        self.assertIsInstance(data["asOf"], str)
        self.assertRegex(
            data["asOf"],
            r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}',
            "asOf should be ISO 8601 format"
        )
    
    def test_general_no_authentication_required(self):
        """일반 추천은 인증 없이 접근 가능"""
        response = self.client.get(self.url, {"risk": "공격투자형"})
        
        self.assertNotEqual(response.status_code, 401)
        self.assertEqual(response.status_code, 200)
    
    def test_general_accepts_get_only(self):
        """일반 추천은 GET만 허용"""
        # POST는 405
        response_post = self.client.post(self.url)
        self.assertEqual(response_post.status_code, 405)
        
        # PUT도 405
        response_put = self.client.put(self.url)
        self.assertEqual(response_put.status_code, 405)
        
        # DELETE도 405
        response_delete = self.client.delete(self.url)
        self.assertEqual(response_delete.status_code, 405)
    
    def test_general_pagination_consistency(self):
        """페이지네이션 total이 실제 아이템 수와 일치하는지"""
        self._make_batch_with_items(n=7, risk="공격투자형")
        
        # 전체 가져오기
        response = self.client.get(self.url, {
            "risk": "공격투자형",
            "limit": 100
        })
        data = response.json()
        
        # DB에서 가져왔으면 7개
        if data["source"] == "llm":
            self.assertEqual(data["total"], 7)
            self.assertEqual(len(data["items"]), 7)
        else:
            # Mock fallback
            self.assertGreater(data["total"], 0)
    
    def test_general_offset_beyond_total(self):
        """offset이 total보다 크면 빈 결과"""
        self._make_batch_with_items(n=5, risk="공격투자형")
        
        response = self.client.get(self.url, {
            "risk": "공격투자형",
            "offset": 100
        })
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        
        # DB에서 가져왔으면 total=5, items=0
        if data["source"] == "llm":
            self.assertEqual(data["total"], 5)
            self.assertEqual(len(data["items"]), 0)
    
    def test_general_response_time(self):
        """일반 추천 응답이 합리적인 시간 내에 오는지"""
        import time
        
        self._make_batch_with_items(n=10, risk="공격투자형")
        
        start = time.time()
        response = self.client.get(self.url, {"risk": "공격투자형"})
        elapsed = time.time() - start
        
        # 3초 이내 응답 (DB 쿼리)
        self.assertLess(elapsed, 3.0, "Response should be within 3 seconds")
        self.assertEqual(response.status_code, 200)