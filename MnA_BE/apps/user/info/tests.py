from django.db import IntegrityError
from django.test import TestCase, Client
from django.urls import reverse
from unittest.mock import patch, MagicMock
from django.http import JsonResponse
import json
import bcrypt

from utils.token_handler import make_access_token
from apps.user.models import User

image_url = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAIAAADTED8xAAADMElEQVR4nOzVwQnAIBQFQYXff81RUkQCOyDj1YOPnbXWPmeTRef+/3O/OyBjzh3CD95BfqICMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMK0CMO0TAAD//2Anhf4QtqobAAAAAElFTkSuQmCC"


class PasswordViewTest(TestCase):
    """Tests for password_view (change user password)"""

    def setUp(self):
        self.client = Client()
        self.url = reverse("password")

        self.user = User.objects.create(
            name="testuser",
            password=bcrypt.hashpw("oldpass123!".encode("utf-8"), bcrypt.gensalt()).decode("utf-8"),
            refresh_token="dummy_refresh",
        )
        self.access_token = make_access_token(str(self.user.id))

    def test_method_not_allowed(self):
        """GET 요청 시 → 405"""
        res = self.client.get(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 405)
        data = res.json(); self.assertIn("detail", data)

    def test_missing_password_field(self):
        """password 누락 → 400"""
        body = json.dumps({})
        res = self.client.put(
            self.url, body, content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("PASSWORD IS REQUIRED", res.json()["message"])

    @patch("apps.user.views.validate_password", return_value=False)
    def test_invalid_password_format(self, mock_validate):
        """password 형식 불일치 → 400"""
        body = json.dumps({"password": "short"})
        res = self.client.put(
            self.url, body, content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("Password must be at least 8 characters long", res.json()["message"])

    @patch("apps.user.views.validate_password", return_value=True)
    @patch("apps.user.views.bcrypt.hashpw", side_effect=bcrypt.hashpw)
    def test_successful_password_update(self, mock_hash, mock_validate):
        """정상 비밀번호 변경 → 200"""
        new_pw = "ValidPass123!"
        body = json.dumps({"password": new_pw})
        res = self.client.put(
            self.url, body, content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("PASSWORD UPDATE SUCCESS", res.json()["message"])

        self.user.refresh_from_db()
        self.assertTrue(bcrypt.checkpw(new_pw.encode("utf-8"), self.user.password.encode("utf-8")))

    @patch("apps.user.views.validate_password", return_value=True)
    @patch("apps.user.views.bcrypt.hashpw", side_effect=bcrypt.hashpw)
    def test_password_save_failed(self, mock_hash, mock_validate):
        """user.save() 실패 → 500"""
        with patch.object(User, "save", side_effect=Exception("DB fail")):
            body = json.dumps({"password": "ValidPass123!"})
            res = self.client.put(
                self.url, body, content_type="application/json",
                HTTP_COOKIE=f"access_token={self.access_token}",
            )
            self.assertEqual(res.status_code, 500)
            self.assertIn("PASSWORD SAVE FAILED", res.json()["message"])


class NameViewTest(TestCase):
    """Tests for name_view (GET / POST user name)"""

    def setUp(self):
        self.client = Client()
        self.url = reverse("name")
        self.user = User.objects.create(
            name="original",
            password=bcrypt.hashpw("Oldpass123!".encode(), bcrypt.gensalt()).decode(),
            refresh_token="dummy_refresh",
        )
        self.access_token = make_access_token(str(self.user.id))

    def test_method_not_allowed(self):
        """PUT 요청 → 405"""
        res = self.client.put(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 405)
        data = res.json(); self.assertIn("detail", data)

    def test_get_user_name(self):
        """GET → 200 + user.name 반환"""
        res = self.client.get(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["name"], self.user.name)

    def test_post_missing_name(self):
        """POST 에 name 없음 → 400"""
        res = self.client.post(
            self.url,
            json.dumps({}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("NAME IS REQUIRED", res.json()["message"])

    @patch("apps.user.views.validate_name", return_value=False)
    def test_post_invalid_name(self, mock_validate):
        """POST invalid name → 400"""
        res = self.client.post(
            self.url,
            json.dumps({"name": "123456789012345678901"}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("Name cannot be longer than 20 characters", res.json()["message"])

    @patch("apps.user.views.validate_name", return_value=True)
    def test_post_name_conflict(self, mock_validate):
        """POST IntegrityError 발생 → 409"""
        with patch.object(User, "save", side_effect=IntegrityError):
            res = self.client.post(
                self.url,
                json.dumps({"name": "duplicate"}),
                content_type="application/json",
                HTTP_COOKIE=f"access_token={self.access_token}",
            )
            self.assertEqual(res.status_code, 409)
            self.assertIn("NAME ALREADY EXISTS", res.json()["message"])

    @patch("apps.user.views.validate_name", return_value=True)
    def test_post_save_failed(self, mock_validate):
        """POST 일반 Exception 발생 → 500"""
        with patch.object(User, "save", side_effect=Exception("DB error")):
            res = self.client.post(
                self.url,
                json.dumps({"name": "error_name"}),
                content_type="application/json",
                HTTP_COOKIE=f"access_token={self.access_token}",
            )
            self.assertEqual(res.status_code, 500)
            self.assertIn("NAME SAVE FAILED", res.json()["message"])

    @patch("apps.user.views.validate_name", return_value=True)
    def test_post_success(self, mock_validate):
        """POST 정상 변경 → 200"""
        new_name = "newname"
        res = self.client.post(
            self.url,
            json.dumps({"name": new_name}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("NAME UPDATE SUCCESS", res.json()["message"])
        self.user.refresh_from_db()
        self.assertEqual(self.user.name, new_name)

class ProfileViewTest(TestCase):
    """Tests for profile_view (GET / POST / DELETE profile image)"""

    def setUp(self):
        self.client = Client()
        self.url = reverse("profile")
        self.user = User.objects.create(
            name="tester",
            password=bcrypt.hashpw("Abcd1234!".encode(), bcrypt.gensalt()).decode(),
            refresh_token="dummy_refresh",
        )
        self.access_token = make_access_token(str(self.user.id))

    def test_method_not_allowed(self):
        """PUT → 405"""
        res = self.client.put(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 405)
        data = res.json(); self.assertIn("detail", data)

    # --- GET ---
    @patch('apps.user.info.profile.BaseBucket')
    def test_get_delete_success(self, mock_bucket_class):
        """POST/GET/DELETE 성공 시나리오"""
        # Mock S3 client
        mock_s3 = MagicMock()
        mock_bucket_class.return_value = mock_s3
        
        # POST - 이미지 업로드
        mock_s3.put_image.return_value = None  # 성공
        res = self.client.post(
            self.url,
            json.dumps({"image_url": image_url}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("PROFILE IMAGE UPLOAD SUCCESS", res.json()["message"])

        # GET - 이미지 조회
        mock_s3.get_image_url.return_value = image_url
        res = self.client.get(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 200)
        self.assertIn("image_url", res.json())
        self.assertEqual(res.json()["image_url"], image_url)

        # DELETE - 이미지 삭제
        mock_s3.delete.return_value = None  # 성공
        res = self.client.delete(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 200)
        self.assertIn("PROFILE DELETE SUCCESS", res.json()["message"])

    @patch('apps.user.info.profile.BaseBucket')
    def test_delete_get_failed(self, mock_bucket_class):
        """GET 실패 (예외 발생) → 500"""
        mock_s3 = MagicMock()
        mock_bucket_class.return_value = mock_s3
        
        # DELETE 먼저 수행 (이미지가 없는 상태로 만듦)
        mock_s3.delete.return_value = None
        self.client.delete(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        
        # GET 실패 시뮬레이션
        mock_s3.get_image_url.side_effect = Exception("S3 Error")
        res = self.client.get(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 500)
        self.assertIn("S3 GET FAILED", res.json()["message"])

    # --- POST ---
    def test_post_missing_image(self):
        """POST image_url 없음 → 400"""
        res = self.client.post(
            self.url,
            json.dumps({}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("IMAGE REQUIRED", res.json()["message"])

    @patch('apps.user.info.profile.BaseBucket')
    def test_post_put_failed(self, mock_bucket_class):
        """POST S3 put 실패 → 500"""
        mock_s3 = MagicMock()
        mock_bucket_class.return_value = mock_s3
        
        # S3 put 실패 시뮬레이션
        mock_s3.put_image.side_effect = Exception("S3 Put Error")
        
        res = self.client.post(
            self.url,
            json.dumps({"image_url": "image_url"}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 500)
        self.assertIn("S3 PUT FAILED", res.json()["message"])

class PortfolioViewTests(TestCase):
    """Portfolio GET/POST 전체 테스트"""
    
    def setUp(self):
        self.client = Client()
        self.user = User.objects.create(
            name="testuser",
            password=bcrypt.hashpw("Test1234!".encode(), bcrypt.gensalt()).decode(),
            refresh_token="dummy_refresh",
        )
        # portfolio 필드 직접 설정
        self.user.portfolio = {"stocks": ["AAPL", "GOOGL"]}
        self.user.save()
        
        self.access_token = make_access_token(str(self.user.id))
        self.url = reverse("portfolio")
    
    def test_get_portfolio_success(self):
        """GET: 포트폴리오 조회 성공"""
        response = self.client.get(
            self.url,
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertIn("portfolio", data)
        self.assertEqual(data["portfolio"]["stocks"], ["AAPL", "GOOGL"])
    
    def test_post_portfolio_success(self):
        """POST: 포트폴리오 변경 성공"""
        new_portfolio = {"stocks": ["TSLA", "NVDA"]}
        
        response = self.client.post(
            self.url,
            data=json.dumps({"portfolio": new_portfolio}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertIn("PORTFOLIO UPDATE SUCCESS", data["message"])
        
        # DB 확인
        self.user.refresh_from_db()
        self.assertEqual(self.user.portfolio, new_portfolio)
    
    def test_post_portfolio_missing_field(self):
        """POST: portfolio 필드 없음 -> 400"""
        response = self.client.post(
            self.url,
            data=json.dumps({}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        
        self.assertEqual(response.status_code, 400)
        data = response.json()
        self.assertIn("CANNOT FIND INPUT", data["message"])
    
    @patch.object(User, 'save')
    def test_post_portfolio_save_failed(self, mock_save):
        """POST: 저장 실패 -> 500"""
        mock_save.side_effect = Exception("DB Error")
        
        response = self.client.post(
            self.url,
            data=json.dumps({"portfolio": {"stocks": ["MSFT"]}}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        
        self.assertEqual(response.status_code, 500)
        data = response.json()
        self.assertIn("PORTFOLIO SAVE FAILED", data["message"])