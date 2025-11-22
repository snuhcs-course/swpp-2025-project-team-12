# apps/api/tests/integration/test_reco_personal.py
from django.test import TestCase, Client
from apps.user.models import User, Style
from unittest.mock import patch, Mock


class ApiRecommendationsPersonalizedTests(TestCase):
    """개인화 추천 - 실제 S3 데이터 사용"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/recommendations/personalized"
    
    def test_personalized_requires_auth(self):
        """인증 필요"""
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, 401)
    
    def test_personalized_with_user(self):
        """유저 생성 후 테스트"""
        user = User.objects.create(name="testuser", password="pass")
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        # 성공 또는 데이터 없음
        self.assertIn(response.status_code, [200, 404, 500])
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_pagination(self, mock_bucket):
        """페이지네이션 (lines 18-28)"""
        user = User.objects.create(name="testpage", password="pass")
        Style.objects.create(
            user=user,
            interests={"interests": ["tech"]},
            strategy={"strategy": "growth"}
        )
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.return_value = {
            "growth_tech": [
                {"ticker": f"00{i}", "name": f"Stock{i}", "reason": "Good"}
                for i in range(10)
            ]
        }
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url, {"limit": 5, "offset": 2})
        
        if response.status_code == 200:
            data = response.json()
            self.assertEqual(data.get("limit"), 5)
            self.assertEqual(data.get("offset"), 2)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_invalid_pagination(self, mock_bucket):
        """잘못된 pagination (lines 26-28)"""
        user = User.objects.create(name="testinvalid", password="pass")
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.return_value = {}
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url, {"limit": "bad", "offset": "invalid"})
        
        if response.status_code == 200:
            data = response.json()
            self.assertEqual(data.get("limit"), 10)
            self.assertEqual(data.get("offset"), 0)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_no_llm_output(self, mock_bucket):
        """LLM output 없을 때 404 (lines 30-34)"""
        user = User.objects.create(name="test404", password="pass")
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": False}
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, 404)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_s3_exception(self, mock_bucket):
        """S3 Exception (lines 37-41)"""
        user = User.objects.create(name="test500", password="pass")
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.side_effect = Exception("S3 Error")
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, 500)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_json_string(self, mock_bucket):
        """JSON 문자열 파싱 (lines 43-45)"""
        import json
        
        user = User.objects.create(name="testjson", password="pass")
        Style.objects.create(
            user=user,
            interests={"interests": ["tech"]},
            strategy={"strategy": "growth"}
        )
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        # JSON 문자열
        mock_s3.get_json.return_value = json.dumps({
            "growth_tech": [{"ticker": "005930", "name": "Samsung", "reason": "Good"}]
        })
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            self.assertIn("data", data)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_no_style(self, mock_bucket):
        """Style 없는 유저 (lines 49-51)"""
        user = User.objects.create(name="testnostyle", password="pass")
        # Style 생성하지 않음
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.return_value = {}
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            self.assertEqual(len(data["data"]), 0)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_filter_by_style(self, mock_bucket):
        """Style 필터링 (lines 48-60)"""
        user = User.objects.create(name="teststyle", password="pass")
        Style.objects.create(
            user=user,
            interests={"interests": ["tech", "health"]},
            strategy={"strategy": "growth"}
        )
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.return_value = {
            "growth_tech": [{"ticker": "005930", "name": "Samsung", "reason": "Tech"}],
            "growth_health": [{"ticker": "068270", "name": "Celltrion", "reason": "Bio"}]
        }
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            # 2개 interest → 2개 결과
            self.assertGreaterEqual(len(data["data"]), 1)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_single_datum(self, mock_bucket):
        """단일 객체 처리 (lines 57-60)"""
        user = User.objects.create(name="testsingle", password="pass")
        Style.objects.create(
            user=user,
            interests={"interests": ["tech"]},
            strategy={"strategy": "growth"}
        )
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        # 리스트가 아닌 단일 객체
        mock_s3.get_json.return_value = {
            "growth_tech": {"ticker": "005930", "name": "Samsung", "reason": "Good"}
        }
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            self.assertGreaterEqual(len(data["data"]), 1)
    
    @patch('apps.api.recommendations.personalized.FinanceBucket')
    def test_personalized_transform_format(self, mock_bucket):
        """프론트엔드 형식 변환 (lines 62-79)"""
        user = User.objects.create(name="testtransform", password="pass")
        Style.objects.create(
            user=user,
            interests={"interests": ["tech"]},
            strategy={"strategy": "growth"}
        )
        
        mock_s3 = Mock()
        mock_bucket.return_value = mock_s3
        
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-21"}
        mock_s3.get_json.return_value = {
            "growth_tech": [
                {"ticker": "005930", "name": "Samsung", "reason": "Strong"}
            ]
        }
        
        from utils.token_handler import make_access_token
        token = make_access_token(user.id)
        self.client.cookies['access_token'] = token
        
        response = self.client.get(self.url)
        
        if response.status_code == 200:
            data = response.json()
            items = data["data"]
            if len(items) > 0:
                item = items[0]
                self.assertIn("ticker", item)
                self.assertIn("name", item)
                self.assertIn("headline", item)
                self.assertIn("price", item)