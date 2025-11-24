# apps/api/tests/integration/test_s3_and_db_integration.py
"""
S3 및 DB 통합 테스트
복잡한 Mock 대신 실제 동작 위주로 테스트
"""

from django.test import TestCase, Client


class ArticlesTopBasicTests(TestCase):
    """Articles Top 기본 동작 테스트"""

    def setUp(self):
        self.client = Client()
        self.url = "/api/articles/top"

    def test_articles_top_returns_data(self):
        """Articles Top이 데이터를 반환하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # 200 OK
        self.assertEqual(response.status_code, 200)

        # 필수 필드
        self.assertIn("items", data)
        self.assertIn("source", data)
        self.assertIn("asOf", data)

        # items는 리스트
        self.assertIsInstance(data["items"], list)

        # source는 s3 또는 mock
        self.assertIn(data["source"], ["s3", "mock"])


class IndicesBasicTests(TestCase):
    """Indices 기본 동작 테스트"""

    def setUp(self):
        self.client = Client()
        self.url = "/api/indices"

    def test_indices_returns_data(self):
        """Indices가 데이터를 반환하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # 200 OK
        self.assertEqual(response.status_code, 200)

        # 필수 필드
        self.assertIn("kospi", data)
        self.assertIn("kosdaq", data)
        self.assertIn("asOf", data)

        # source가 있으면 s3 또는 mock
        if "source" in data:
            self.assertIn(data["source"], ["s3", "mock"])


class HealthEndpointBasicTests(TestCase):
    """Health 엔드포인트 기본 테스트"""

    def setUp(self):
        self.client = Client()
        self.url = "/api/health"

    def test_health_returns_status(self):
        """Health가 상태를 반환하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # 200 또는 503
        self.assertIn(response.status_code, [200, 503])

        # 필수 필드
        self.assertIn("api", data)
        self.assertIn("s3", data)
        self.assertIn("db", data)

        # api는 항상 ok
        self.assertEqual(data["api"], "ok")


class ReportsDetailBasicTests(TestCase):
    """Reports Detail 기본 동작 테스트"""

    def setUp(self):
        self.client = Client()

    def test_reports_detail_returns_data(self):
        """Reports Detail이 데이터를 반환하는지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()

        # 200 OK
        self.assertEqual(response.status_code, 200)

        # 필수 필드
        self.assertIn("profile", data)
        self.assertIn("price", data)
        self.assertIn("source", data)
        self.assertIn("asOf", data)

        # source는 s3, empty, memory, cache 중 하나
        self.assertIn(data["source"], ["s3", "empty", "memory", "cache"])

    def test_reports_detail_invalid_symbol(self):
        """존재하지 않는 심볼 처리"""
        response = self.client.get("/api/reports/INVALID999")
        data = response.json()

        # 200 OK (에러가 아닌 빈 데이터)
        self.assertEqual(response.status_code, 200)

        # profile은 None이거나 source가 empty
        if data["source"] == "empty":
            self.assertIsNone(data["profile"])


class CompanyProfilesBasicTests(TestCase):
    """Company Profiles 기본 동작 테스트"""

    def setUp(self):
        self.client = Client()
        self.url = "/api/company-profiles"

    def test_company_profiles_returns_data(self):
        """Company Profiles가 데이터를 반환하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # 200 OK
        self.assertEqual(response.status_code, 200)

        # 페이지네이션 필드
        self.assertIn("total", data)
        self.assertIn("limit", data)
        self.assertIn("offset", data)

        # degraded가 아니면 items
        if not data.get("degraded"):
            self.assertIn("items", data)
            self.assertIsInstance(data["items"], list)
