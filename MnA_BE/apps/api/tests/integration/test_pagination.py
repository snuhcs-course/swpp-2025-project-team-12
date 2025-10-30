from django.test import TestCase, Client


class ApiPaginationTests(TestCase):
    """
    모든 엔드포인트의 페이지네이션 일관성 테스트
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
    
    def test_pagination_default_values(self):
        """모든 페이지네이션 엔드포인트가 기본값을 사용하는지 확인"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)
                data = response.json()
                
                # 기본 limit=10, offset=0
                self.assertEqual(data.get("limit"), 10, f"{endpoint} should have default limit=10")
                self.assertEqual(data.get("offset"), 0, f"{endpoint} should have default offset=0")
    
    def test_pagination_custom_limit(self):
        """모든 엔드포인트가 커스텀 limit을 적용하는지 확인"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"limit": 5})
                data = response.json()
                
                self.assertEqual(data.get("limit"), 5, f"{endpoint} should apply custom limit")
    
    def test_pagination_custom_offset(self):
        """모든 엔드포인트가 커스텀 offset을 적용하는지 확인"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"offset": 5})
                data = response.json()
                
                self.assertEqual(data.get("offset"), 5, f"{endpoint} should apply custom offset")
    
    def test_pagination_max_limit_enforcement(self):
        """모든 엔드포인트가 최대 limit(100)을 강제하는지 확인"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"limit": 1000})
                data = response.json()
                
                self.assertLessEqual(
                    data.get("limit"),
                    100,
                    f"{endpoint} should enforce max limit of 100"
                )
    
    def test_pagination_negative_limit(self):
        """음수 limit 처리"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"limit": -5})
                data = response.json()
                
                # 음수는 기본값으로 대체되거나 에러
                self.assertIn(response.status_code, [200, 400])
                
                if response.status_code == 200:
                    # 음수를 기본값으로 대체
                    self.assertGreater(data.get("limit"), 0)
    
    def test_pagination_negative_offset(self):
        """음수 offset 처리"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"offset": -5})
                data = response.json()
                
                # 음수는 0으로 대체되거나 에러
                self.assertIn(response.status_code, [200, 400])
                
                if response.status_code == 200:
                    self.assertGreaterEqual(data.get("offset"), 0)
    
    def test_pagination_zero_limit(self):
        """limit=0 처리"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"limit": 0})
                data = response.json()
                
                # 0은 기본값으로 대체되거나 빈 결과
                self.assertIn(response.status_code, [200, 400])
    
    def test_pagination_string_parameters(self):
        """문자열 파라미터 처리"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"limit": "abc", "offset": "xyz"})
                
                # 에러 또는 기본값 사용
                self.assertIn(response.status_code, [200, 400])
    
    def test_pagination_items_count_respects_limit(self):
        """반환된 items 개수가 limit을 초과하지 않는지 확인"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"limit": 3})
                data = response.json()
                
                # items가 있고 degraded가 아니면
                if "items" in data and not data.get("degraded"):
                    self.assertLessEqual(
                        len(data["items"]),
                        3,
                        f"{endpoint} returned more items than limit"
                    )
    
    def test_pagination_offset_beyond_total_returns_empty(self):
        """offset이 total을 초과하면 빈 결과"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint, {"offset": 99999})
                data = response.json()
                
                self.assertEqual(response.status_code, 200)
                
                # items가 비어있어야 함
                if "items" in data and not data.get("degraded"):
                    self.assertEqual(len(data["items"]), 0)
    
    def test_pagination_consistency_across_pages(self):
        """여러 페이지를 순회해도 데이터가 일관되는지 확인"""
        endpoint = "/api/company-profiles"
        
        # 첫 페이지
        response1 = self.client.get(endpoint, {"limit": 2, "offset": 0})
        data1 = response1.json()
        
        # 두 번째 페이지
        response2 = self.client.get(endpoint, {"limit": 2, "offset": 2})
        data2 = response2.json()
        
        # total이 같아야 함
        if not data1.get("degraded") and not data2.get("degraded"):
            self.assertEqual(
                data1.get("total"),
                data2.get("total"),
                "Total should be consistent across pages"
            )
    
    def test_pagination_total_field_present(self):
        """모든 페이지네이션 응답에 total 필드가 있는지 확인"""
        endpoints = [
            "/api/company-profiles",
            "/api/recommendations/general?risk=공격투자형",
            "/api/recommendations/personalized",
        ]
        
        for endpoint in endpoints:
            with self.subTest(endpoint=endpoint):
                response = self.client.get(endpoint)
                data = response.json()
                
                self.assertIn("total", data, f"{endpoint} should have 'total' field")
                self.assertIsInstance(data["total"], int, "total should be integer")
                self.assertGreaterEqual(data["total"], 0, "total should be non-negative")