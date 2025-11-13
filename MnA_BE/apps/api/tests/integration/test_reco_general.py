# apps/api/tests/integration/test_reco_general.py
from django.test import TestCase, Client
from unittest.mock import patch, Mock


class ApiRecommendationsGeneralTests(TestCase):
    """
    일반 추천 엔드포인트 상세 테스트
    프론트엔드 형식 (data, status) 기반
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/recommendations/general"
    
    def test_general_endpoint_accessible(self):
        """일반 추천 엔드포인트에 접근 가능한지 확인"""
        response = self.client.get(self.url)
        
        # 항상 200 OK
        self.assertEqual(response.status_code, 200)
    
    def test_general_returns_json(self):
        """일반 추천이 JSON을 반환하는지 확인"""
        response = self.client.get(self.url)
        
        self.assertIn("application/json", response.get("Content-Type", ""))
        
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_general_has_required_fields(self):
        """일반 추천 응답이 필수 필드를 포함하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # 프론트엔드 형식: data, status
        required_fields = ["data", "status"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
        
        # status는 "success"여야 함
        self.assertEqual(data["status"], "success")
        
        # data는 리스트여야 함
        self.assertIsInstance(data["data"], list)
    
    def test_general_item_structure(self):
        """추천 아이템의 구조가 올바른지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        self.assertGreater(len(items), 0, "Should have at least one item")
        
        if items:
            item = items[0]
            
            # 프론트엔드 RecommendationDto 필드
            required_item_fields = ["ticker", "name", "price", "change", "change_rate", "time", "headline"]
            for field in required_item_fields:
                with self.subTest(field=field):
                    self.assertIn(field, item, f"Item should have {field}")
    
    def test_general_headline_from_reason(self):
        """headline이 LLM의 reason에서 매핑되는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        if items:
            item = items[0]
            
            # headline이 있어야 함
            self.assertIn("headline", item)
            self.assertIsInstance(item["headline"], (str, type(None)))
    
    def test_general_ticker_and_name(self):
        """ticker와 name이 올바른 형식인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        if items:
            item = items[0]
            
            # ticker는 문자열
            self.assertIsInstance(item["ticker"], str)
            self.assertGreater(len(item["ticker"]), 0)
            
            # name은 문자열
            self.assertIsInstance(item["name"], str)
            self.assertGreater(len(item["name"]), 0)
    
    def test_general_price_fields_structure(self):
        """price 관련 필드 구조 확인 (현재는 None 가능)"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        if items:
            item = items[0]
            
            # price, change, change_rate는 숫자 또는 None
            if item["price"] is not None:
                self.assertIsInstance(item["price"], (int, float))
            
            if item["change"] is not None:
                self.assertIsInstance(item["change"], (int, float))
            
            if item["change_rate"] is not None:
                self.assertIsInstance(item["change_rate"], (int, float))
    
    def test_general_time_field(self):
        """time 필드가 올바른 형식인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        if items:
            item = items[0]
            
            # time은 문자열
            self.assertIsInstance(item["time"], str)
            # 시간 형식 (HH:MM)
            self.assertRegex(item["time"], r'\d{2}:\d{2}')
    
    def test_general_default_risk_profile(self):
        """risk 파라미터가 없어도 응답"""
        # risk 없이 요청
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["status"], "success")
    
    def test_general_date_parameter(self):
        """date 파라미터로 특정 날짜 조회"""
        response = self.client.get(self.url + "/2025/11/05")
        
        # 200 OK여야 함
        self.assertEqual(response.status_code, 200)
    
    def test_general_invalid_date_format(self):
        """잘못된 날짜 형식 처리"""
        # 잘못된 날짜로 요청
        response = self.client.get(self.url + "/2025/13/40")
        
        # 에러 없이 응답 (404 또는 200)
        self.assertIn(response.status_code, [200, 404, 500])
    
    def test_general_no_authentication_required(self):
        """일반 추천은 인증 없이 접근 가능"""
        response = self.client.get(self.url)
        
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
    
    def test_general_multiple_items(self):
        """여러 추천 아이템이 반환되는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        
        # 최소 1개 이상의 아이템
        self.assertGreater(len(items), 0, "Should return at least one recommendation")
        
        # 보통 10개 정도 반환
        self.assertLessEqual(len(items), 20, "Should not return too many items")
    
    def test_general_consistent_response_structure(self):
        """모든 아이템이 동일한 구조를 가지는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        items = data.get("data", [])
        if len(items) > 1:
            # 첫 번째 아이템의 키
            first_keys = set(items[0].keys())
            
            # 모든 아이템이 같은 키를 가져야 함
            for item in items[1:]:
                self.assertEqual(set(item.keys()), first_keys, "All items should have same structure")
    
    def test_general_response_time(self):
        """일반 추천 응답이 합리적인 시간 내에 오는지"""
        import time
        
        start = time.time()
        response = self.client.get(self.url)
        elapsed = time.time() - start
        
        # 10초 이내 응답 (S3 호출 포함)
        self.assertLess(elapsed, 10.0, "Response should be within 10 seconds")
        self.assertEqual(response.status_code, 200)
    
    def test_general_empty_data_handling(self):
        """데이터가 없을 때 빈 배열 반환"""
        # 아주 오래된 날짜로 요청
        response = self.client.get(self.url + "/2020/01/01")
        
        if response.status_code == 200:
            data = response.json()
            # data는 빈 배열일 수 있음
            self.assertIsInstance(data.get("data", []), list)
    
    def test_general_status_field_always_present(self):
        """status 필드가 항상 존재하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertIn("status", data)
        self.assertIn(data["status"], ["success", "error"])
    
    @patch('apps.api.recommendations.general.FinanceS3Client')
    def test_general_no_llm_output_404(self, mock_s3_client_class):
        """LLM output이 없을 때 404 반환"""
        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client
        
        # check_source 실패
        mock_s3_client.check_source.return_value = {"ok": False}
        
        response = self.client.get(self.url)
        data = response.json()
        
        # 404 에러
        self.assertEqual(response.status_code, 404)
        self.assertIn("message", data)
        self.assertEqual(data["message"], "No LLM output found")
    
    @patch('apps.api.recommendations.general.FinanceS3Client')
    def test_general_s3_exception_500(self, mock_s3_client_class):
        """S3에서 Exception 발생 시 500 반환"""
        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client
        
        # check_source는 성공
        mock_s3_client.check_source.return_value = {
            "ok": True,
            "latest": "2025-11-06"
        }
        
        # get_json에서 Exception
        mock_s3_client.get_json.side_effect = Exception("S3 Error")
        
        response = self.client.get(self.url)
        data = response.json()
        
        # 500 에러
        self.assertEqual(response.status_code, 500)
        self.assertIn("message", data)
        self.assertEqual(data["message"], "Unexpected Server Error")
    
    def test_general_with_date_parameters(self):
        """날짜 파라미터로 특정 날짜 조회"""
        # URL 패턴: /api/recommendations/general/2025/11/06
        response = self.client.get(f"{self.url}/2025/11/06")
        
        # 200 OK 또는 404 (해당 날짜 데이터 없을 수 있음)
        self.assertIn(response.status_code, [200, 404, 500])
    
    def test_general_invalid_pagination_parameters(self):
        """잘못된 페이지네이션 파라미터 처리 (lines 25-27)"""
        # 문자열 파라미터
        response = self.client.get(self.url, {"limit": "invalid", "offset": "bad"})
        data = response.json()
        
        # 기본값으로 처리되어야 함
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["limit"], 10)  # 기본값
        self.assertEqual(data["offset"], 0)  # 기본값
        
        # 빈 문자열 파라미터
        response2 = self.client.get(self.url, {"limit": "", "offset": ""})
        data2 = response2.json()
        
        self.assertEqual(response2.status_code, 200)
        self.assertEqual(data2["limit"], 10)
        self.assertEqual(data2["offset"], 0)
        
        # 부동소수점 파라미터
        response3 = self.client.get(self.url, {"limit": "5.5", "offset": "2.7"})
        data3 = response3.json()
        
        self.assertEqual(response3.status_code, 200)
        # ValueError 발생 → 기본값
        self.assertEqual(data3["limit"], 10)
        self.assertEqual(data3["offset"], 0)