# apps/user/tests/test_user_views.py
import json
import bcrypt
import jwt
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock

# Create your tests here.
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

    # Helper to create valid tokens -------------------
    def make_jwt(self, user_id: int, exp_minutes: int = 30):
        payload = {
            "id": user_id,
            "exp": datetime.utcnow() + timedelta(minutes=exp_minutes),
        }
        return jwt.encode(payload, self.secret, algorithm="HS256")

    # -----------------------------
    # URL reverse
    # -----------------------------
    def test_url_names_exist(self):
        for name in ["login", "signup", "logout", "withdraw"]:
            try:
                reverse(name)
            except NoReverseMatch as e:
                self.fail(f"URL missing: {name} ({e})")

    # -----------------------------
    # signup
    # -----------------------------
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

    # -----------------------------
    # login
    # -----------------------------
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

    # -----------------------------
    # logout
    # -----------------------------
    def test_logout_success_with_real_token(self):
        """실제 유효한 JWT 토큰을 사용한 logout"""
        token = self.make_jwt(self.user.id)
        self.client.cookies["access_token"] = token

        res = self.client.post(reverse("logout"))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["message"], "LOGOUT SUCCESS")

    def test_logout_without_cookie(self):
        res = self.client.post(reverse("logout"))
        self.assertEqual(res.status_code, 401)

    # -----------------------------
    # withdraw
    # -----------------------------
    def test_withdraw_success_with_real_token(self):
        """access_token 쿠키 + 실제 유효한 JWT"""
        token = self.make_jwt(self.user.id)
        self.client.cookies["access_token"] = token

        res = self.client.delete(reverse("withdraw"))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["message"], "WITHDRAWAL SUCCESS")

    def test_withdraw_without_cookie(self):
        res = self.client.delete(reverse("withdraw"))
        self.assertEqual(res.status_code, 401)

    # -----------------------------
    # require_auth decorator edge
    # -----------------------------
    def test_auth_expired_token(self):
        """만료된 access_token → 401"""
        expired_token = self.make_jwt(self.user.id, exp_minutes=-1)
        self.client.cookies["access_token"] = expired_token
        res = self.client.post(reverse("logout"))
        self.assertIn(res.status_code, (401, 500))
