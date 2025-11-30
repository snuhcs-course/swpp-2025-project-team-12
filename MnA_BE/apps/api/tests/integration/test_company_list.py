# apps/api/tests/integration/test_company_list.py
from django.test import TestCase, Client
from unittest.mock import patch, Mock


class ApiCompanyListTests(TestCase):
    """
    회사 목록 엔드포인트 테스트
    views.py lines 124-162 커버
    """

    def setUp(self):
        self.client = Client()
        self.url = "/api/company-list"

    def test_company_list_endpoint_accessible(self):
        """회사 목록 엔드포인트 접근 가능"""
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)

    def test_company_list_returns_json(self):
        """JSON 반환"""
        response = self.client.get(self.url)
        self.assertIn("application/json", response.get("Content-Type", ""))
        data = response.json()
        self.assertIsInstance(data, dict)

    def test_company_list_has_required_fields(self):
        """필수 필드 확인"""
        response = self.client.get(self.url)
        data = response.json()

        required_fields = ["items", "total", "limit", "offset"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data)

    def test_company_list_default_market(self):
        """기본 market=kosdaq (line 120)"""
        response = self.client.get(self.url)
        data = response.json()

        self.assertEqual(response.status_code, 200)
        if not data.get("degraded"):
            self.assertIn("items", data)

    def test_company_list_kospi_filter(self):
        """kospi 필터 (line 120)"""
        response = self.client.get(self.url, {"market": "kospi"})
        data = response.json()

        self.assertEqual(response.status_code, 200)

    def test_company_list_pagination(self):
        """페이지네이션 (lines 119, 143-144)"""
        response = self.client.get(self.url, {"limit": 5, "offset": 2})
        data = response.json()

        if not data.get("degraded"):
            self.assertEqual(data["limit"], 5)
            self.assertEqual(data["offset"], 2)
            self.assertLessEqual(len(data["items"]), 5)

    def test_company_list_item_structure(self):
        """아이템 구조 (lines 146-149)"""
        response = self.client.get(self.url)
        data = response.json()

        if not data.get("degraded") and data["items"]:
            item = data["items"][0]
            self.assertIn("ticker", item)
            self.assertIn("name", item)

    def test_company_list_no_authentication_required(self):
        """인증 불필요"""
        response = self.client.get(self.url)
        self.assertNotEqual(response.status_code, 401)

    def test_company_list_accepts_get_only(self):
        """GET만 허용"""
        response_post = self.client.post(self.url)
        self.assertEqual(response_post.status_code, 405)

    @patch("apps.api.views.ApiConfig")
    def test_company_list_memory_not_loaded(self, mock_config):
        """메모리에 데이터 없을 때 degraded (lines 160-162)"""
        mock_config.instant_df = None

        response = self.client.get(self.url)
        data = response.json()

        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "memory")
        self.assertEqual(data["total"], 0)

    @patch("apps.api.views.ApiConfig")
    def test_company_list_exception_in_filtering(self, mock_config):
        """company_list 필터링 중 예외 (lines 160-162, 237-239)"""
        # Exception을 발생시키는 Mock
        mock_df = Mock()
        mock_df.__getitem__ = Mock(side_effect=Exception("Filter error"))

        mock_config.instant_df = mock_df

        response = self.client.get(self.url)
        data = response.json()

        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "memory")
