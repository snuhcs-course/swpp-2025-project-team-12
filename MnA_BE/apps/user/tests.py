# apps/user/tests.py
import json
import bcrypt
import jwt
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock
import os
from dotenv import load_dotenv

load_dotenv()

from django.test import TestCase, Client
from django.urls import reverse, NoReverseMatch
from apps.user.models import User


class UserViewsTest(TestCase):
    """Integration tests for user views & require_auth decorator."""

    def setUp(self):
        self.client = Client()
        self.secret = os.environ.get("SECRET_KEY")
        self.password = bcrypt.hashpw(b"1234abcd!", bcrypt.gensalt()).decode("utf-8")
        self.user = User.objects.create(
            id=1, name="tester", password=self.password, refresh_token="refresh_token_abc"
        )

    def make_jwt(self, user_id: int, exp_minutes: int = 30):
        payload = {
            "id": user_id,
            "exp": datetime.utcnow() + timedelta(minutes=exp_minutes),
        }
        return jwt.encode(payload, self.secret, algorithm="HS256")

    def test_url_names_exist(self):
        for name in ["login", "signup", "logout", "withdraw"]:
            try:
                reverse(name)
            except NoReverseMatch as e:
                self.fail(f"URL missing: {name} ({e})")

    @patch("apps.user.views.validate_password", return_value=True)
    @patch("apps.user.views.make_access_token", return_value="access123")
    @patch("apps.user.views.make_refresh_token", return_value="refresh123")
    def test_signup_success(self, m1, m2, m3):
        body = {"id": "newuser", "password": "Passw0rd!"}
        res = self.client.post(
            reverse("signup"), data=json.dumps(body), content_type="application/json"
        )
        self.assertEqual(res.status_code, 201)
        self.assertIn("refresh_token", res.cookies)
        self.assertIn("access_token", res.cookies)

    def test_signup_invalid_json(self):
        res = self.client.post(reverse("signup"), data="{invalid", content_type="application/json")
        self.assertEqual(res.status_code, 400)

    def test_signup_missing_fields(self):
        for body in [{}, {"id": "abc"}, {"password": "pw"}]:
            res = self.client.post(
                reverse("signup"), data=json.dumps(body), content_type="application/json"
            )
            self.assertEqual(res.status_code, 400)

    def test_signup_user_exists(self):
        body = {"id": self.user.name, "password": "1234abcd!"}
        res = self.client.post(
            reverse("signup"), data=json.dumps(body), content_type="application/json"
        )
        self.assertEqual(res.status_code, 409)

    def test_signup_wrong_password_format(self):
        body = {"id": "userx", "password": "short"}
        res = self.client.post(
            reverse("signup"), data=json.dumps(body), content_type="application/json"
        )
        self.assertEqual(res.status_code, 400)

    @patch("apps.user.views.make_access_token", return_value="access123")
    @patch("apps.user.views.make_refresh_token", return_value="refresh123")
    def test_login_success(self, m1, m2):
        body = {"id": "tester", "password": "1234abcd!"}
        res = self.client.post(
            reverse("login"), data=json.dumps(body), content_type="application/json"
        )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["message"], "LOGIN SUCCESS")
        self.assertIn("access_token", res.cookies)
        self.assertIn("refresh_token", res.cookies)

    def test_login_wrong_password(self):
        body = {"id": "tester", "password": "wrongpw"}
        res = self.client.post(
            reverse("login"), data=json.dumps(body), content_type="application/json"
        )
        self.assertEqual(res.status_code, 401)

    def test_login_user_not_found(self):
        body = {"id": "nouser", "password": "pw"}
        res = self.client.post(
            reverse("login"), data=json.dumps(body), content_type="application/json"
        )
        self.assertEqual(res.status_code, 401)

    def test_login_invalid_json(self):
        res = self.client.post(reverse("login"), data="{", content_type="application/json")
        self.assertEqual(res.status_code, 400)

    def test_login_method_not_allowed(self):
        res = self.client.get(reverse("login"))
        self.assertEqual(res.status_code, 405)

    def test_logout_success_with_real_token(self):
        token = self.make_jwt(self.user.id)
        self.client.cookies["access_token"] = token
        res = self.client.post(reverse("logout"))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["message"], "LOGOUT SUCCESS")

    def test_logout_without_cookie(self):
        res = self.client.post(reverse("logout"))
        self.assertEqual(res.status_code, 401)

    def test_withdraw_success_with_real_token(self):
        token = self.make_jwt(self.user.id)
        self.client.cookies["access_token"] = token
        res = self.client.delete(reverse("withdraw"))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["message"], "WITHDRAWAL SUCCESS")

    def test_withdraw_without_cookie(self):
        res = self.client.delete(reverse("withdraw"))
        self.assertEqual(res.status_code, 401)

    def test_auth_expired_token(self):
        expired_token = self.make_jwt(self.user.id, exp_minutes=-1)
        self.client.cookies["access_token"] = expired_token
        res = self.client.post(reverse("logout"))
        self.assertIn(res.status_code, (401, 500))


# ===== 추가: views.py 누락 커버리지 =====
class LoginMissingTests(TestCase):
    """login 누락 라인 (40, 42)"""

    def test_login_missing_user_id(self):
        """user_id 없음 (line 40)"""
        client = Client()
        url = reverse("login")
        res = client.post(
            url, json.dumps({"password": "Test1234!"}), content_type="application/json"
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("ID REQUIRED", res.json()["message"])

    def test_login_missing_password(self):
        """password 없음 (line 42) - id는 제공"""
        client = Client()
        url = reverse("login")
        # 'user_id'가 아니라 'id' 키 사용
        res = client.post(url, json.dumps({"id": "testuser"}), content_type="application/json")
        self.assertEqual(res.status_code, 400)
        self.assertIn("PASSWORD REQUIRED", res.json()["message"])


class SignupRefreshTokenFailTests(TestCase):
    """signup refresh token 저장 실패 (lines 59-60)"""

    @patch("apps.user.views.validate_password")
    @patch("apps.user.views.validate_name")
    @patch("apps.user.views.User.objects.create")
    def test_signup_refresh_token_save_failed(self, mock_create, mock_val_name, mock_val_pw):
        """refresh token user.save() 실패 (lines 59-60)"""
        mock_val_name.return_value = True
        mock_val_pw.return_value = True

        # User 생성은 성공
        mock_user = MagicMock()
        mock_user.id = 999
        mock_create.return_value = mock_user

        # save() 실패
        mock_user.save.side_effect = Exception("Token save failed")

        client = Client()
        url = reverse("signup")
        res = client.post(
            url,
            json.dumps({"id": "newuser", "password": "Valid123!"}),
            content_type="application/json",
        )

        self.assertEqual(res.status_code, 500)
        self.assertIn("USER CREATE FAILED", res.json()["message"])


class WithdrawMissingTests(TestCase):
    """withdraw 누락 라인 (123-125, 152-153)"""

    def setUp(self):
        from utils.token_handler import make_access_token

        self.client = Client()
        self.url = reverse("withdraw")
        self.user = User.objects.create(
            name="testuser",
            password=bcrypt.hashpw("Pass123!".encode(), bcrypt.gensalt()).decode(),
            refresh_token="dummy",
        )
        self.access_token = make_access_token(str(self.user.id))

    @patch("apps.user.views.BaseBucket")
    def test_withdraw_profile_exists_delete_failed(self, mock_bucket_class):
        """프로필 있지만 삭제 실패 (lines 123-125)"""
        mock_s3 = MagicMock()
        mock_bucket_class.return_value = mock_s3
        mock_s3.get.return_value = "some_image_data"
        mock_s3.delete.side_effect = Exception("S3 delete failed")

        res = self.client.delete(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
        self.assertEqual(res.status_code, 500)
        self.assertIn("PROFILE DELETE FAILED", res.json()["message"])

    @patch("apps.user.views.BaseBucket")
    def test_withdraw_user_delete_failed(self, mock_bucket_class):
        """user.delete() 실패 (lines 152-153)"""
        mock_s3 = MagicMock()
        mock_bucket_class.return_value = mock_s3
        mock_s3.get.side_effect = Exception("Not found")

        with patch.object(User, "delete", side_effect=Exception("DB delete failed")):
            res = self.client.delete(self.url, HTTP_COOKIE=f"access_token={self.access_token}")
            self.assertEqual(res.status_code, 500)
            self.assertIn("USER DELETE FAILED", res.json()["message"])
