from django.test import TestCase, Client


class ApiIndicesTests(TestCase):
    """
    지수(Indices) 엔드포인트 상세 테스트
    KOSPI, KOSDAQ 데이터 검증
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/indices"
    
    def test_indices_endpoint_accessible(self):
        """지수 엔드포인트에 접근 가능한지 확인"""
        response = self.client.get(self.url)
        
        # 항상 200 OK (mock fallback 있음)
        self.assertEqual(response.status_code, 200)
    
    def test_indices_returns_json(self):
        """지수 엔드포인트가 JSON을 반환하는지 확인"""
        response = self.client.get(self.url)
        
        self.assertIn("application/json", response.get("Content-Type", ""))
        
        # JSON 파싱 가능
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_indices_has_required_fields(self):
        """지수 응답이 필수 필드를 포함하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # 필수 필드: kospi, kosdaq, asOf
        required_fields = ["kospi", "kosdaq", "asOf"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
    
    def test_indices_kospi_structure(self):
        """KOSPI 데이터 구조가 올바른지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        kospi = data["kospi"]
        
        # 숫자 또는 딕셔너리
        if isinstance(kospi, dict):
            # 딕셔너리면 value, changePct 필드
            self.assertIn("value", kospi, "kospi dict should have 'value'")
            self.assertIn("changePct", kospi, "kospi dict should have 'changePct'")
            
            # value는 숫자
            self.assertIsInstance(
                kospi["value"],
                (int, float),
                "kospi.value should be number"
            )
            
            # changePct도 숫자
            self.assertIsInstance(
                kospi["changePct"],
                (int, float),
                "kospi.changePct should be number"
            )
        else:
            # 숫자면 그대로 OK
            self.assertIsInstance(
                kospi,
                (int, float),
                "kospi should be number or dict"
            )
    
    def test_indices_kosdaq_structure(self):
        """KOSDAQ 데이터 구조가 올바른지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        kosdaq = data["kosdaq"]
        
        # 숫자 또는 딕셔너리
        if isinstance(kosdaq, dict):
            # 딕셔너리면 value, changePct 필드
            self.assertIn("value", kosdaq, "kosdaq dict should have 'value'")
            self.assertIn("changePct", kosdaq, "kosdaq dict should have 'changePct'")
            
            # value는 숫자
            self.assertIsInstance(
                kosdaq["value"],
                (int, float),
                "kosdaq.value should be number"
            )
            
            # changePct도 숫자
            self.assertIsInstance(
                kosdaq["changePct"],
                (int, float),
                "kosdaq.changePct should be number"
            )
        else:
            # 숫자면 그대로 OK
            self.assertIsInstance(
                kosdaq,
                (int, float),
                "kosdaq should be number or dict"
            )
    
    def test_indices_asof_is_timestamp(self):
        """asOf 필드가 타임스탬프 형식인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        asof = data["asOf"]
        self.assertIsNotNone(asof)
        self.assertIsInstance(asof, str)
        
        # ISO 8601 형식 체크 (간단한 패턴)
        self.assertRegex(
            asof,
            r'\d{4}-\d{2}-\d{2}',
            "asOf should contain date (YYYY-MM-DD)"
        )
    
    def test_indices_kospi_value_range(self):
        """KOSPI 값이 합리적인 범위인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        kospi = data["kospi"]
        
        # 값 추출
        if isinstance(kospi, dict):
            value = kospi["value"]
        else:
            value = kospi
        
        # KOSPI는 대략 1000 ~ 5000 범위 (역사적으로)
        self.assertGreater(value, 0, "KOSPI should be positive")
        self.assertLess(value, 10000, "KOSPI should be reasonable (< 10000)")
    
    def test_indices_kosdaq_value_range(self):
        """KOSDAQ 값이 합리적인 범위인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        kosdaq = data["kosdaq"]
        
        # 값 추출
        if isinstance(kosdaq, dict):
            value = kosdaq["value"]
        else:
            value = kosdaq
        
        # KOSDAQ은 대략 300 ~ 2000 범위 (역사적으로)
        self.assertGreater(value, 0, "KOSDAQ should be positive")
        self.assertLess(value, 5000, "KOSDAQ should be reasonable (< 5000)")
    
    def test_indices_has_source_field(self):
        """응답에 source 필드가 있는지 확인 (선택적)"""
        response = self.client.get(self.url)
        data = response.json()
        
        # source 필드가 있으면
        if "source" in data:
            source = data["source"]
            
            # "s3" 또는 "mock"
            self.assertIn(
                source,
                ["s3", "mock"],
                "source should be 's3' or 'mock'"
            )
    
    def test_indices_idempotent(self):
        """
        지수 엔드포인트를 여러 번 호출해도 일관된 결과
        (같은 타임스탬프면 같은 데이터)
        """
        response1 = self.client.get(self.url)
        response2 = self.client.get(self.url)
        
        self.assertEqual(response1.status_code, response2.status_code)
        
        data1 = response1.json()
        data2 = response2.json()
        
        # asOf가 같으면 데이터도 같아야 함
        if data1["asOf"] == data2["asOf"]:
            self.assertEqual(data1["kospi"], data2["kospi"])
            self.assertEqual(data1["kosdaq"], data2["kosdaq"])
    
    def test_indices_no_authentication_required(self):
        """지수 엔드포인트는 인증 없이 접근 가능"""
        response = self.client.get(self.url)
        
        # 401 Unauthorized가 아니어야 함
        self.assertNotEqual(response.status_code, 401)
        self.assertEqual(response.status_code, 200)
    
    def test_indices_accepts_get_only(self):
        """지수 엔드포인트는 GET만 허용"""
        # POST는 405 Method Not Allowed
        response_post = self.client.post(self.url)
        self.assertEqual(response_post.status_code, 405)
        
        # PUT도 405
        response_put = self.client.put(self.url)
        self.assertEqual(response_put.status_code, 405)
        
        # DELETE도 405
        response_delete = self.client.delete(self.url)
        self.assertEqual(response_delete.status_code, 405)
    
    def test_indices_changepct_calculation(self):
        """changePct가 있으면 합리적인 범위인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        for market in ["kospi", "kosdaq"]:
            market_data = data[market]
            
            if isinstance(market_data, dict) and "changePct" in market_data:
                change_pct = market_data["changePct"]
                
                with self.subTest(market=market):
                    # 변화율은 대략 -30% ~ +30% 범위
                    self.assertGreaterEqual(
                        change_pct,
                        -30,
                        f"{market} changePct too negative"
                    )
                    self.assertLessEqual(
                        change_pct,
                        30,
                        f"{market} changePct too positive"
                    )
    
    def test_indices_response_time(self):
        """지수 엔드포인트 응답이 합리적인 시간 내에 오는지"""
        import time
        
        start = time.time()
        response = self.client.get(self.url)
        elapsed = time.time() - start
        
        # 5초 이내 응답
        self.assertLess(elapsed, 5.0, "Response should be within 5 seconds")
        self.assertEqual(response.status_code, 200)
    
    def test_indices_mock_fallback_exists(self):
        """
        S3 실패 시 mock 데이터로 fallback하는지 확인
        (degraded 모드가 아니라 항상 데이터 제공)
        """
        response = self.client.get(self.url)
        data = response.json()
        
        # 항상 200 OK
        self.assertEqual(response.status_code, 200)
        
        # 데이터가 항상 있어야 함
        self.assertIn("kospi", data)
        self.assertIn("kosdaq", data)
        self.assertIsNotNone(data["kospi"])
        self.assertIsNotNone(data["kosdaq"])