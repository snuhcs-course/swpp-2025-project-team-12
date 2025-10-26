from django.test import TestCase, Client
from django.urls import reverse
from unittest.mock import patch, MagicMock
from django.http import JsonResponse
import json
import bcrypt

from utils.token_handler import make_access_token
from apps.user.models import User


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
        self.assertIn("NOT ALLOWED METHOD", res.json()["message"])

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
        self.assertIn("INVALID PASSWORD", res.json()["message"])

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
        self.url = reverse("name")  # ✅ urls.py 에 name="name" 으로 등록 필요
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
        self.assertIn("NOT ALLOWED METHOD", res.json()["message"])

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
            json.dumps({"name": "@"}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("INVALID NAME", res.json()["message"])

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
        self.url = reverse("profile")  # ✅ urls.py 에 name="profile" 로 등록 필요
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
        self.assertIn("NOT ALLOWED METHOD", res.json()["message"])

    # --- GET ---
    @patch("apps.user.views.S3Client")
    def test_get_success(self, mock_s3):
        """GET 성공 → 200 + image_url 반환"""
        mock_s3.return_value.get_image_url.return_value = "https://example.com/img.png"
        res = self.client.get(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 200)
        self.assertIn("image_url", res.json())
        self.assertEqual(res.json()["image_url"], "https://example.com/img.png")

    @patch("apps.user.views.S3Client")
    def test_get_failed(self, mock_s3):
        """GET 실패 (예외 발생) → 500"""
        mock_s3.return_value.get_image_url.side_effect = Exception("S3 error")
        res = self.client.get(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 500)
        self.assertIn("S3 GET FAILED", res.json()["message"])

    # --- POST ---
    @patch("apps.user.views.S3Client")
    def test_post_missing_image(self, mock_s3):
        """POST image_url 없음 → 400"""
        res = self.client.post(
            self.url,
            json.dumps({}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("IMAGE REQUIRED", res.json()["message"])

    @patch("apps.user.views.S3Client")
    def test_post_put_failed(self, mock_s3):
        """POST S3 put 실패 → 500"""
        mock_s3.return_value.put_image.side_effect = Exception("S3 error")
        res = self.client.post(
            self.url,
            json.dumps({"image_url": "https://upload.com/test.png"}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 500)
        self.assertIn("S3 PUT FAILED", res.json()["message"])

    @patch("apps.user.views.S3Client")
    def test_post_success(self, mock_s3):
        """POST 성공 → 200"""
        res = self.client.post(
            self.url,
            json.dumps({"image_url": "https://upload.com/test.png"}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}",
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("PROFILE IMAGE UPLOAD SUCCESS", res.json()["message"])

    # --- DELETE ---
    @patch("apps.user.views.S3Client")
    def test_delete_failed(self, mock_s3):
        """DELETE 실패 → 400"""
        mock_s3.return_value.delete.side_effect = Exception("delete failed")
        res = self.client.delete(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 400)
        self.assertIn("PROFILE DELETE FAILED", res.json()["message"])

    @patch("apps.user.views.S3Client")
    def test_delete_success(self, mock_s3):
        """DELETE 성공 → 200"""
        res = self.client.delete(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 200)
        self.assertIn("PROFILE DELETE SUCCESS", res.json()["message"])
