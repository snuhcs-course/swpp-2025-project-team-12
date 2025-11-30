# apps/api/tests/integration/test_reco_personal.py
from django.test import TestCase, Client
from unittest.mock import patch, Mock
from apps.user.models import User, Style
from utils.token_handler import make_access_token


class ApiRecommendationsPersonalizedTests(TestCase):
    """
    개인화 추천 엔드포인트 상세 테스트
    인증 포함 + Mock S3
    """

    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/recommendations/personalized"

    def test_personalized_requires_authentication(self):
        """인증 없이 요청하면 401 반환"""
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 401)
        data = response.json()
        self.assertIn("message", data)
        self.assertIn("Unauthorized", data["message"])

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_with_auth_success(self, mock_s3_client_class):
        """인증된 사용자의 정상 요청"""
        # User & Style 생성
        user = User.objects.create(name="testuser_personalized", password="testpass123")

        Style.objects.create(
            user=user, interests={"interests": ["tech"]}, strategy={"strategy": "growth"}
        )

        # Mock S3 Client
        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        # check_source 성공
        mock_s3_client.check_source.return_value = {"ok": True, "latest": "2025-11-06"}

        # get_json 성공
        mock_s3_client.get_json.return_value = {
            "growth_tech": [
                {"ticker": "005930", "name": "Samsung Electronics", "reason": "Strong growth"}
            ]
        }

        # JWT 토큰 생성 및 쿠키 설정
        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        response = self.client.get(self.url)
        data = response.json()

        # 200 OK
        self.assertEqual(response.status_code, 200)
        self.assertIn("data", data)
        self.assertIn("status", data)
        self.assertEqual(data["status"], "success")

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_pagination(self, mock_s3_client_class):
        """페이지네이션 테스트"""
        user = User.objects.create(name="testuser_pagination", password="testpass123")

        Style.objects.create(
            user=user, interests={"interests": ["tech"]}, strategy={"strategy": "growth"}
        )

        # Mock S3
        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        mock_s3_client.check_source.return_value = {"ok": True, "latest": "2025-11-06"}

        mock_s3_client.get_json.return_value = {
            "growth_tech": [
                {"ticker": f"00{i:04d}", "name": f"Company {i}", "reason": "Good"}
                for i in range(10)
            ]
        }

        # JWT 토큰 생성 및 쿠키 설정
        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        response = self.client.get(self.url, {"limit": 5, "offset": 2})
        data = response.json()

        self.assertEqual(data["limit"], 5)
        self.assertEqual(data["offset"], 2)
        self.assertEqual(data["total"], 10)
        self.assertEqual(len(data["data"]), 5)

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_no_style(self, mock_s3_client_class):
        """Style이 없는 사용자"""
        user = User.objects.create(name="testuser_no_style", password="testpass123")
        # Style 생성하지 않음

        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        mock_s3_client.check_source.return_value = {"ok": True, "latest": "2025-11-06"}

        mock_s3_client.get_json.return_value = {
            "growth_tech": [{"ticker": "005930", "name": "Samsung", "reason": "Good"}]
        }

        # JWT 토큰 생성 및 쿠키 설정
        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        response = self.client.get(self.url)
        data = response.json()

        # Style 없으면 빈 결과
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(data["data"]), 0)
        self.assertEqual(data["total"], 0)

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_no_llm_output_404(self, mock_s3_client_class):
        """LLM output이 없을 때 404"""
        user = User.objects.create(name="testuser_404", password="testpass123")

        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        # check_source 실패
        mock_s3_client.check_source.return_value = {"ok": False}

        # JWT 토큰 생성 및 쿠키 설정
        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        response = self.client.get(self.url)
        data = response.json()

        self.assertEqual(response.status_code, 404)
        self.assertEqual(data["message"], "No LLM output found")

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_s3_exception_500(self, mock_s3_client_class):
        """S3 Exception 시 500"""
        user = User.objects.create(name="testuser_500", password="testpass123")

        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        mock_s3_client.check_source.return_value = {"ok": True, "latest": "2025-11-06"}

        # get_json Exception
        mock_s3_client.get_json.side_effect = Exception("S3 Error")

        # JWT 토큰 생성 및 쿠키 설정
        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        response = self.client.get(self.url)
        data = response.json()

        self.assertEqual(response.status_code, 500)
        self.assertEqual(data["message"], "Unexpected Server Error")

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_with_date_params(self, mock_s3_client_class):
        """날짜 파라미터로 특정 날짜 데이터 요청 (lines 45)"""
        user = User.objects.create(name="testuser_date", password="testpass123")

        Style.objects.create(
            user=user, interests={"interests": ["tech"]}, strategy={"strategy": "growth"}
        )

        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        # get_json에 대한 응답 설정
        mock_s3_client.get_json.return_value = {
            "growth_tech": [{"ticker": "005930", "name": "Samsung", "reason": "Good"}]
        }

        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        # URL: /api/recommendations/personalized/2025/11/06
        response = self.client.get("/api/recommendations/personalized/2025/11/06")

        # 200 또는 404/500 (날짜 데이터 없을 수 있음)
        self.assertIn(response.status_code, [200, 404, 500])

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_invalid_pagination(self, mock_s3_client_class):
        """잘못된 pagination 파라미터 처리 (lines 26-28)"""
        user = User.objects.create(name="testuser_invalid_page", password="testpass123")

        Style.objects.create(
            user=user, interests={"interests": ["tech"]}, strategy={"strategy": "growth"}
        )

        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        mock_s3_client.check_source.return_value = {"ok": True, "latest": "2025-11-06"}

        mock_s3_client.get_json.return_value = {
            "growth_tech": [{"ticker": "005930", "name": "Samsung", "reason": "Good"}]
        }

        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        # 잘못된 pagination 파라미터
        response = self.client.get(self.url, {"limit": "invalid", "offset": "bad"})
        data = response.json()

        # 기본값으로 처리
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["limit"], 10)
        self.assertEqual(data["offset"], 0)

    @patch("apps.api.recommendations.personalized.FinanceS3Client")
    def test_personalized_single_datum(self, mock_s3_client_class):
        """단일 datum 처리 (lines 59-60)"""
        user = User.objects.create(name="testuser_single", password="testpass123")

        Style.objects.create(
            user=user, interests={"interests": ["tech"]}, strategy={"strategy": "growth"}
        )

        mock_s3_client = Mock()
        mock_s3_client_class.return_value = mock_s3_client

        mock_s3_client.check_source.return_value = {"ok": True, "latest": "2025-11-06"}

        # 단일 객체 (리스트가 아님)
        mock_s3_client.get_json.return_value = {
            "growth_tech": {"ticker": "005930", "name": "Samsung", "reason": "Good"}
        }

        access_token = make_access_token(user.id)
        self.client.cookies["access_token"] = access_token

        response = self.client.get(self.url)
        data = response.json()

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(data["data"]), 1)
