# apps/api/tests/integration/test_api_urlnames.py
from django.test import TestCase
from django.urls import reverse, NoReverseMatch
import os

class ApiUrlNamesSmokeTests(TestCase):
    """
    API URL 라우팅 및 실제 동작 검증
    각 테스트는 명확한 성공/실패 조건을 가짐
    """
    
    def test_all_api_url_names_are_registered(self):
        """모든 주요 API 엔드포인트의 URL name이 등록되어 있는지 확인"""
        required_urls = [
            "health",
            "indices",
            "company_profiles",
            "reports_detail",
        ]
        
        for url_name in required_urls:
            with self.subTest(url_name=url_name):
                try:
                    if url_name == "reports_detail":
                        url = reverse(url_name, kwargs={"symbol": "TEST123"})
                    else:
                        url = reverse(url_name)
                    
                    self.assertIsNotNone(url)
                    self.assertIsInstance(url, str)
                    
                except NoReverseMatch as e:
                    self.fail(f"URL name '{url_name}' is not registered: {e}")
    
    def test_url_path_consistency(self):
        """URL name과 실제 경로가 일치하는지 확인"""
        url_mappings = {
            "health": "/api/health",
            "indices": "/api/indices",
            "company_profiles": "/api/company-profiles",
        }
        
        for name, expected_path in url_mappings.items():
            with self.subTest(name=name):
                actual_path = reverse(name)
                self.assertEqual(
                    actual_path, 
                    expected_path,
                    f"URL name '{name}' resolves to '{actual_path}', expected '{expected_path}'"
                )
    
    def test_health_endpoint_returns_proper_structure(self):
        """
        헬스 체크 엔드포인트가 올바른 응답 구조를 반환하는지 확인
        """
        # 환경변수 확인 - 없으면 테스트 실패
        if not os.getenv('FINANCE_BUCKET_NAME'):
            self.fail(
                "FINANCE_BUCKET_NAME environment variable is not set. "
                "Check your .env file and ensure dotenv.load_dotenv() is called."
            )
        
        url = reverse("health")
        response = self.client.get(url)
        
        # 200 (성공) 또는 503 (S3 실패) 둘 다 허용
        self.assertIn(
            response.status_code, 
            [200, 503],
            f"Health endpoint should return 200 or 503, got {response.status_code}"
        )
        
        # JSON 응답 검증
        data = response.json()
        self.assertIsInstance(data, dict)
        
        # 필수 필드 검증
        required_fields = ["api", "s3", "db"]
        for field in required_fields:
            self.assertIn(field, data, f"Missing required field: {field}")
        
        # api 상태는 반드시 "ok"
        self.assertEqual(data["api"], "ok")
        
        # s3 연결 상태 확인 (성공 또는 실패 정보가 있어야 함)
        self.assertIsInstance(data["s3"], dict)
        if response.status_code == 503:
            # 503이면 S3 실패 정보가 있어야 함
            self.assertFalse(data["s3"].get("ok"), "503 status but S3 shows ok=True")
        
        # db 연결 확인
        self.assertIsInstance(data["db"], dict)
    
    def test_indices_endpoint_returns_valid_data(self):
        """지수 엔드포인트가 올바른 데이터를 반환하는지 확인"""
        url = reverse("indices")
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, 200)
        
        data = response.json()
        
        # 필수 필드 확인
        required_fields = ["kospi", "kosdaq", "asOf"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
        
        # kospi와 kosdaq은 숫자 또는 딕셔너리여야 함
        self.assertTrue(
            isinstance(data["kospi"], (int, float, dict)),
            f"kospi should be number or dict, got {type(data['kospi'])}"
        )
        self.assertTrue(
            isinstance(data["kosdaq"], (int, float, dict)),
            f"kosdaq should be number or dict, got {type(data['kosdaq'])}"
        )
    
    def test_company_profiles_endpoint_returns_data_or_fails_gracefully(self):
        """기업 프로필 엔드포인트 검증"""
        url = reverse("company_profiles")
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, 200)
        
        data = response.json()
        
        # 페이지네이션 필드 필수
        pagination_fields = ["total", "limit", "offset"]
        for field in pagination_fields:
            self.assertIn(field, data, f"Missing pagination field: {field}")
        
        self.assertIsInstance(data["total"], int)
        self.assertGreaterEqual(data["total"], 0)
        
        # degraded가 아니면 items가 반드시 있어야 함
        if not data.get("degraded"):
            self.assertIn("items", data, "Non-degraded response must have 'items'")
            self.assertIsInstance(data["items"], list)
            
            # total > 0이면 items도 있어야 함
            if data["total"] > 0:
                self.assertGreater(len(data["items"]), 0, "Total > 0 but items is empty")
        else:
            # degraded 상태면 error 메시지가 있어야 함
            self.assertIn("error", data, "Degraded response must have 'error' message")
    
    def test_company_profiles_pagination_works_correctly(self):
        """페이지네이션이 올바르게 동작하는지 확인"""
        url = reverse("company_profiles")
        
        response = self.client.get(url, {"limit": 3, "offset": 0})
        self.assertEqual(response.status_code, 200)
        
        data = response.json()
        self.assertEqual(data["limit"], 3)
        self.assertEqual(data["offset"], 0)
        
        # degraded가 아니면 items 개수 확인
        if not data.get("degraded") and "items" in data:
            self.assertLessEqual(
                len(data["items"]), 
                3,
                f"Requested limit=3 but got {len(data['items'])} items"
            )
    
    def test_reports_detail_endpoint(self):
        """리포트 상세 엔드포인트가 올바르게 동작하는지 확인"""
        test_symbol = "005930"
        url = reverse("reports_detail", kwargs={"symbol": test_symbol})
        
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_recommendations_general_endpoint_requires_risk_parameter(self):
        """일반 추천 엔드포인트가 risk 파라미터를 올바르게 처리하는지 확인"""
        url = "/api/recommendations/general"
        
        # 파라미터 없이 요청
        response_without_param = self.client.get(url)
        
        # 404가 아니어야 함 (라우팅은 되어야 함)
        self.assertNotEqual(response_without_param.status_code, 404)
        
        # 파라미터와 함께 요청
        response_with_param = self.client.get(url, {"risk": "공격투자형"})
        
        # 응답이 있어야 함
        self.assertIn(response_with_param.status_code, [200, 400])
    
    def test_recommendations_personalized_endpoint_exists(self):
        """개인화 추천 엔드포인트가 존재하고 응답하는지 확인"""
        response = self.client.get("/api/recommendations/personalized")
        
        # 404가 아니어야 함
        self.assertNotEqual(response.status_code, 404)
        
        # JSON 응답이어야 함
        self.assertIn("application/json", response.get("Content-Type", ""))
    
    def test_all_endpoints_return_json(self):
        """모든 엔드포인트가 JSON Content-Type을 반환하는지 확인"""
        endpoints = [
            reverse("indices"),
            reverse("company_profiles"),
            reverse("reports_detail", kwargs={"symbol": "TEST"}),
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)
                self.assertIn(
                    "application/json",
                    response.get("Content-Type", ""),
                    f"{endpoint} must return JSON"
                )
    
    def test_no_500_errors_on_valid_requests(self):
        """유효한 요청에 대해 500 에러가 발생하지 않는지 확인"""
        endpoints = [
            reverse("indices"),
            reverse("company_profiles"),
            reverse("reports_detail", kwargs={"symbol": "005930"}),
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)
                self.assertNotEqual(
                    response.status_code,
                    500,
                    f"{endpoint} returned 500 Internal Server Error"
                )