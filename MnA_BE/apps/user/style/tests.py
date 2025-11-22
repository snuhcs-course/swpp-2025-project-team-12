import json
from django.test import TestCase
from django.urls import reverse
from unittest.mock import patch, MagicMock
from apps.user.models import Style, User
from utils.token_handler import make_access_token

class StyleViewTest(TestCase):
    def setUp(self):
        self.user = User.objects.create(
            password="pw1234!", name="tester"
        )
        self.access_token = make_access_token(str(self.user.id))
        self.url = reverse("style")

    def test_get_success_with_existing_style(self):
        """GET 성공 - 최근 style 반환"""
        Style.objects.create(user=self.user, interests="AI,Tech", strategy="Aggressive")

        res = self.client.get(
            self.url,
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("style", res.json())
        self.assertEqual(res.json()["style"]["interests"], "AI,Tech")

    def test_get_no_style(self):
        """GET 성공 - style이 없을 때 None"""
        res = self.client.get(
            self.url,
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 200)
        self.assertIsNone(res.json()["style"])

    def test_post_success(self):
        """POST 성공"""
        data = {"interests": "Blockchain", "strategy": "Balanced"}
        res = self.client.post(
            self.url,
            data=json.dumps(data),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["message"], "INTERESTS UPDATE SUCCESS")
        self.assertEqual(Style.objects.count(), 1)

    def test_post_missing_interests(self):
        """POST 실패 - interests 없음"""
        data = {"strategy": "Balanced"}
        res = self.client.post(
            self.url,
            data=json.dumps(data),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("INTERESTS REQUIRED", res.json()["message"])

    def test_post_missing_strategy(self):
        """POST 실패 - strategy 없음"""
        data = {"interests": "Finance"}
        res = self.client.post(
            self.url,
            data=json.dumps(data),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("STRATEGY REQUIRED", res.json()["message"])

    @patch("apps.user.models.Style.objects.create")
    def test_post_save_failed(self, mock_create):
        """POST 실패 - DB 저장 실패"""
        mock_create.side_effect = Exception("db error")
        data = {"interests": "Finance", "strategy": "Defensive"}
        res = self.client.post(
            self.url,
            data=json.dumps(data),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 500)
        self.assertIn("SAVE INTERESTS FAILED", res.json()["message"])


class StylePageViewTest(TestCase):
    def setUp(self):
        self.user = User.objects.create(
            password="pw1234!", name="tester2"
        )
        self.access_token = make_access_token(str(self.user.id))
        self.url = lambda page_index: reverse("style_page", args=[page_index])

    def test_get_page_success(self):
        """GET 성공 - 페이지별 style 반환"""
        for i in range(15):
            Style.objects.create(
                user=self.user,
                interests=f"topic{i}",
                strategy="Balanced"
            )

        res = self.client.get(
            self.url(1),
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("style_page", res.json())
        self.assertEqual(len(res.json()["style_page"]), 10)  # PAGE_SIZE=10

    def test_get_second_page_success(self):
        """GET 성공 - 두 번째 페이지"""
        for i in range(15):
            Style.objects.create(
                user=self.user,
                interests=f"topic{i}",
                strategy="Balanced"
            )

        res = self.client.get(
            self.url(2),
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(len(res.json()["style_page"]), 5)

    def test_get_empty_page(self):
        """GET - 스타일 없음"""
        res = self.client.get(
            self.url(1),
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["style_page"], [])

    def test_not_allowed_method(self):
        """POST 등 비허용 메서드"""
        res = self.client.post(
            self.url(1),
            data=json.dumps({}),
            content_type="application/json",
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        self.assertEqual(res.status_code, 405)
        data = res.json(); self.assertIn("detail", data)

    @patch('apps.user.style.page.Paginator')
    def test_get_page_exception_returns_empty(self, mock_paginator):
        """페이지 조회 중 Exception 발생 시 빈 리스트 반환"""
        # Paginator 생성 시 Exception 발생
        mock_paginator.side_effect = Exception("Pagination Error")
        
        # 테스트 데이터 생성
        for i in range(5):
            Style.objects.create(
                user=self.user,
                interests=f"topic{i}",
                strategy="Balanced"
            )
        
        res = self.client.get(
            self.url(1),
            HTTP_COOKIE=f"access_token={self.access_token}"
        )
        
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertEqual(data["style_page"], [])