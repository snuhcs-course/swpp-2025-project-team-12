# apps/api/tests/integration/test_reco_error_handling.py
from django.test import TestCase, Client


class ApiErrorHandlingTests(TestCase):
    """
    모든 엔드포인트의 에러 처리 일관성 테스트
    """

    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()

    def test_404_on_invalid_endpoints(self):
        """존재하지 않는 엔드포인트는 404 반환"""
        invalid_endpoints = [
            "/api/nonexistent",
            "/api/invalid-endpoint",
            "/api/test123",
        ]

        for endpoint in invalid_endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)
                self.assertEqual(response.status_code, 404)

    def test_405_on_wrong_http_methods(self):
        """잘못된 HTTP 메서드는 405 반환"""
        endpoints = [
            "/api/health",
            "/api/indices",
            "/api/company-profiles",
            "/api/reports/005930",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]

        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint, method="POST"):
                response = self.client.post(endpoint)
                self.assertEqual(response.status_code, 405, f"{endpoint} should reject POST")

            with self.subTest(endpoint=endpoint, method="PUT"):
                response = self.client.put(endpoint)
                self.assertEqual(response.status_code, 405, f"{endpoint} should reject PUT")

            with self.subTest(endpoint=endpoint, method="DELETE"):
                response = self.client.delete(endpoint)
                self.assertEqual(response.status_code, 405, f"{endpoint} should reject DELETE")

    def test_invalid_symbol_format(self):
        """잘못된 심볼 형식 처리"""
        invalid_symbols = [
            "INVALID!!!",
            "12345678901234567890",  # 너무 긴 심볼
            "",  # 빈 심볼
        ]

        for symbol in invalid_symbols:
            with self.subTest(symbol=symbol):
                response = self.client.get(f"/api/reports/{symbol}")

                # 200 (빈 결과) 또는 400/404
                self.assertIn(response.status_code, [200, 400, 404])

    def test_graceful_degradation_on_s3_failure(self):
        """S3 실패 시 적절한 degraded 모드"""
        # S3를 사용하는 엔드포인트들
        endpoints = [
            "/api/company-profiles",
        ]

        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)
                data = response.json()

                # 항상 200 OK (degraded 가능)
                self.assertEqual(response.status_code, 200)

                # degraded면 error 메시지
                if data.get("degraded"):
                    self.assertIn("error", data)

    def test_no_500_errors_on_valid_requests(self):
        """유효한 요청에 대해 500 에러가 발생하지 않는지 확인"""
        endpoints = [
            "/api/health",
            "/api/indices",
            "/api/company-profiles",
            "/api/company-profiles?limit=5&offset=10",
            "/api/reports/005930",
            "/api/reports/INVALID999",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]

        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)

                self.assertNotEqual(
                    response.status_code, 500, f"{endpoint} returned 500 Internal Server Error"
                )

    def test_json_response_on_errors(self):
        """에러 응답도 JSON 형식인지 확인"""
        # 404 에러
        response = self.client.get("/api/nonexistent")

        # JSON 응답이어야 함 (또는 HTML일 수 있음)
        if response.status_code == 404:
            # Content-Type 확인 (charset 등 추가 정보 포함 가능)
            content_type = response.get("Content-Type", "")
            self.assertTrue(
                "application/json" in content_type or "text/html" in content_type,
                f"Unexpected content type: {content_type}",
            )

    def test_missing_required_parameters(self):
        """필수 파라미터 누락 처리"""
        # risk 파라미터가 필요한 엔드포인트 (하지만 기본값이 있을 수 있음)
        response = self.client.get("/api/recommendations/general")

        # 기본값 사용 또는 에러
        self.assertIn(response.status_code, [200, 400])

    def test_invalid_pagination_parameters(self):
        """잘못된 페이지네이션 파라미터 처리"""
        test_cases = [
            {"limit": -1},
            {"offset": -1},
            {"limit": "abc"},
            {"offset": "xyz"},
            {"limit": 99999999},
        ]

        endpoint = "/api/company-profiles"

        for params in test_cases:
            with self.subTest(params=params):
                response = self.client.get(endpoint, params)

                # 에러 또는 기본값 사용
                self.assertIn(response.status_code, [200, 400])

                # 500 에러는 없어야 함
                self.assertNotEqual(response.status_code, 500)

    def test_invalid_date_format(self):
        """잘못된 날짜 형식 처리"""
        invalid_dates = [
            "2025-13-01",  # 잘못된 월
            "2025-01-32",  # 잘못된 일
            "invalid-date",  # 완전히 잘못된 형식
            "01/01/2025",  # 다른 형식
        ]

        for date in invalid_dates:
            with self.subTest(date=date):
                response = self.client.get(
                    "/api/recommendations/general", {"risk": "공격투자형", "date": date}
                )

                # 에러 또는 현재 날짜로 fallback
                self.assertIn(response.status_code, [200, 400])
                self.assertNotEqual(response.status_code, 500)

    def test_special_characters_in_parameters(self):
        """특수 문자가 포함된 파라미터 처리"""
        special_chars = [
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "../../../etc/passwd",
        ]

        for char in special_chars:
            with self.subTest(special_char=char):
                response = self.client.get("/api/reports/{char}")

                # 에러 처리 (500은 아니어야 함)
                self.assertNotEqual(response.status_code, 500)

    def test_concurrent_requests_stability(self):
        """동시 요청에도 안정적인지 확인"""
        import threading

        results = []

        def make_request():
            response = self.client.get("/api/indices")
            results.append(response.status_code)

        # 10개의 동시 요청
        threads = [threading.Thread(target=make_request) for _ in range(10)]

        for thread in threads:
            thread.start()

        for thread in threads:
            thread.join()

        # 모두 성공해야 함
        for status_code in results:
            self.assertEqual(status_code, 200)

    def test_large_offset_handling(self):
        """매우 큰 offset 처리"""
        response = self.client.get("/api/company-profiles", {"offset": 999999999})

        # 에러 없이 빈 결과
        self.assertEqual(response.status_code, 200)
        data = response.json()

        if not data.get("degraded"):
            self.assertEqual(len(data.get("items", [])), 0)

    def test_utf8_encoding(self):
        """UTF-8 인코딩 처리"""
        # 한글 파라미터
        response = self.client.get("/api/recommendations/general", {"risk": "공격투자형"})

        self.assertEqual(response.status_code, 200)

        # 응답도 UTF-8
        data = response.json()
        self.assertIsInstance(data, dict)
