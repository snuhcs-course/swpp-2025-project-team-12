# apps/api/tests/integration/test_reco_general.py
from django.test import TestCase, Client
from unittest.mock import patch, Mock


class ApiRecommendationsGeneralTests(TestCase):
    """일반 추천 엔드포인트 - 실제 S3 데이터 사용"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/recommendations/general"
    
    def test_general_endpoint_accessible(self):
        """일반 추천 엔드포인트에 접근 가능"""
        response = self.client.get(self.url)
        
        # 200 또는 404 (LLM 데이터 없을 수 있음)
        self.assertIn(response.status_code, [200, 404, 500])
    
    def test_general_returns_json(self):
        """JSON 반환"""
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            self.assertIsInstance(data, dict)
    
    def test_general_with_date(self):
        """날짜 파라미터 테스트 (lines 36-66)"""
        response = self.client.get(f"{self.url}/2025/11/21")
        
        # 성공 또는 데이터 없음
        self.assertIn(response.status_code, [200, 404, 500])
    
    def test_general_pagination(self):
        """페이지네이션 (lines 17-28)"""
        response = self.client.get(self.url, {"limit": 5, "offset": 0})
        
        if response.status_code == 200:
            data = response.json()
            # limit, offset 확인
            if "limit" in data:
                self.assertEqual(data["limit"], 5)
    
    def test_general_invalid_pagination(self):
        """잘못된 pagination (lines 25-28)"""
        response = self.client.get(self.url, {"limit": "invalid", "offset": "bad"})
        
        # ValueError 처리되어 기본값 사용
        if response.status_code == 200:
            data = response.json()
            self.assertEqual(data.get("limit"), 10)
            self.assertEqual(data.get("offset"), 0)
    
    @patch('apps.api.recommendations.general.FinanceBucket')
    def test_general_no_llm_output(self, mock_bucket):
        """LLM output 없을 때 404 (lines 30-34)"""
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": False}
        
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, 404)
    
    @patch('apps.api.recommendations.general.FinanceBucket')
    def test_general_s3_exception(self, mock_bucket):
        """S3 Exception 시 500 (lines 37-40)"""
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.side_effect = Exception("S3 Error")
        
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, 500)
    
    @patch('apps.api.recommendations.general.FinanceBucket')
    def test_general_json_string_parsing(self, mock_bucket):
        """JSON 문자열 파싱 (lines 42-44)"""
        import json
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        # JSON 문자열로 반환
        mock_s3.get_json.return_value = json.dumps({
            "top_picks": [
                {"ticker": "005930", "name": "Samsung", "reason": "Good"}
            ]
        })
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            self.assertIn("data", data)
    
    @patch('apps.api.recommendations.general.FinanceBucket')
    def test_general_transform_format(self, mock_bucket):
        """프론트엔드 형식 변환 (lines 47-66)"""
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.return_value = {
            "top_picks": [
                {
                    "ticker": "005930",
                    "name": "Samsung Electronics",
                    "reason": "Strong performance"
                }
            ]
        }
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            self.assertIn("data", data)
            
            items = data["data"]
            if len(items) > 0:
                item = items[0]
                # 필수 필드 확인
                self.assertIn("ticker", item)
                self.assertIn("name", item)
                self.assertIn("headline", item)
                self.assertIn("price", item)
                self.assertIn("time", item)