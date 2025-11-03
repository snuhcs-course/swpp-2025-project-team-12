from django.test import TestCase, Client


class ApiRecommendationsPersonalizedTests(TestCase):
    """
    개인화 추천 엔드포인트 상세 테스트
    Mock 데이터 기반 (DB 연동은 향후 구현)
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/recommendations/personalized"
    
    def test_personalized_endpoint_accessible(self):
        """개인화 추천 엔드포인트에 접근 가능한지 확인"""
        response = self.client.get(self.url)
        
        # 200 OK (mock 데이터)
        self.assertEqual(response.status_code, 200)
    
    def test_personalized_returns_json(self):
        """개인화 추천이 JSON을 반환하는지 확인"""
        response = self.client.get(self.url)
        
        self.assertIn("application/json", response.get("Content-Type", ""))
        
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_personalized_has_pagination_fields(self):
        """개인화 추천 응답이 페이지네이션 필드를 포함하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # 필수 페이지네이션 필드
        pagination_fields = ["total", "limit", "offset"]
        for field in pagination_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing pagination field: {field}")
    
    def test_personalized_has_required_fields(self):
        """개인화 추천 응답이 필수 필드를 포함하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # 필수 필드
        required_fields = ["items", "source", "asOf"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
    
    def test_personalized_default_pagination(self):
        """기본 페이지네이션 값이 적용되는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # 기본값: limit=10, offset=0
        self.assertEqual(data["limit"], 10)
        self.assertEqual(data["offset"], 0)
    
    def test_personalized_custom_limit(self):
        """사용자 지정 limit가 적용되는지 확인"""
        response = self.client.get(self.url, {"limit": 5})
        data = response.json()
        
        self.assertEqual(data["limit"], 5)
        
        # items가 limit 이하
        if "items" in data:
            self.assertLessEqual(len(data["items"]), 5)
    
    def test_personalized_custom_offset(self):
        """사용자 지정 offset이 적용되는지 확인"""
        response = self.client.get(self.url, {"offset": 3})
        data = response.json()
        
        self.assertEqual(data["offset"], 3)
    
    def test_personalized_pagination_works(self):
        """페이지네이션이 실제로 작동하는지 확인"""
        # 첫 페이지
        response1 = self.client.get(self.url, {"limit": 2, "offset": 0})
        data1 = response1.json()
        
        # 두 번째 페이지
        response2 = self.client.get(self.url, {"limit": 2, "offset": 2})
        data2 = response2.json()
        
        # 데이터가 충분하면 다른 아이템
        if data1["total"] > 2:
            items1 = data1.get("items", [])
            items2 = data2.get("items", [])
            
            if items1 and items2:
                # ticker 필드 사용 (DB 구조와 일치)
                tickers1 = [item["ticker"] for item in items1]
                tickers2 = [item["ticker"] for item in items2]
                
                # 겹치지 않아야 함
                self.assertEqual(len(set(tickers1) & set(tickers2)), 0)
    
    def test_personalized_item_structure(self):
        """추천 아이템의 구조가 올바른지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # items가 있어야 함
        self.assertIn("items", data)
        self.assertIsInstance(data["items"], list)
        
        if data["items"]:
            item = data["items"][0]
            
            # 필수 필드 (DB 구조: ticker, name)
            required_item_fields = ["ticker", "name"]
            for field in required_item_fields:
                with self.subTest(field=field):
                    self.assertIn(field, item, f"Item should have {field}")
    
    def test_personalized_source_is_mock(self):
        """현재는 mock 소스를 사용하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # source 필드 확인
        self.assertIn("source", data)
        
        # mock 또는 mock-personalized
        self.assertIn("mock", data["source"].lower())
    
    def test_personalized_has_items(self):
        """개인화 추천이 항상 아이템을 반환하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertIn("items", data)
        self.assertIsInstance(data["items"], list)
        
        # mock이므로 항상 데이터 있어야 함
        self.assertGreater(len(data["items"]), 0, "Mock should return items")
    
    def test_personalized_total_consistency(self):
        """total 값이 실제 아이템 수와 일치하는지 확인"""
        response = self.client.get(self.url, {"limit": 100})
        data = response.json()
        
        # offset=0이고 limit >= total이면
        if data["offset"] == 0 and data["limit"] >= data["total"]:
            self.assertEqual(len(data["items"]), data["total"])
    
    def test_personalized_no_authentication_required(self):
        """개인화 추천은 현재 인증 없이 접근 가능 (TODO: 향후 인증 추가)"""
        response = self.client.get(self.url)
        
        # 현재는 인증 없이도 OK
        self.assertEqual(response.status_code, 200)
    
    def test_personalized_accepts_get_only(self):
        """개인화 추천은 GET만 허용"""
        # POST는 405
        response_post = self.client.post(self.url)
        self.assertEqual(response_post.status_code, 405)
        
        # PUT도 405
        response_put = self.client.put(self.url)
        self.assertEqual(response_put.status_code, 405)
        
        # DELETE도 405
        response_delete = self.client.delete(self.url)
        self.assertEqual(response_delete.status_code, 405)
    
    def test_personalized_offset_beyond_total(self):
        """offset이 total보다 크면 빈 결과"""
        response = self.client.get(self.url, {"offset": 1000})
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(data["items"]), 0)
    
    def test_personalized_max_limit(self):
        """최대 limit이 적용되는지 확인"""
        response = self.client.get(self.url, {"limit": 1000})
        data = response.json()
        
        # limit은 100을 초과하지 않아야 함
        self.assertLessEqual(data["limit"], 100)
    
    def test_personalized_as_of_field(self):
        """응답에 asOf 필드가 있는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertIn("asOf", data)
        self.assertIsInstance(data["asOf"], str)
    
    def test_personalized_response_time(self):
        """개인화 추천 응답이 합리적인 시간 내에 오는지"""
        import time
        
        start = time.time()
        response = self.client.get(self.url)
        elapsed = time.time() - start
        
        # 2초 이내 응답 (mock이므로 빠름)
        self.assertLess(elapsed, 2.0, "Response should be within 2 seconds")
        self.assertEqual(response.status_code, 200)