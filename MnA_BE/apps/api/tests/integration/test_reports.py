from django.test import TestCase, Client


class ApiReportsDetailTests(TestCase):
    """
    리포트 상세 엔드포인트 테스트
    실제 S3 데이터 또는 degraded 모드 검증
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
    
    def test_reports_detail_endpoint_accessible(self):
        """리포트 상세 엔드포인트에 접근 가능한지 확인"""
        # 유명한 삼성전자로 테스트
        response = self.client.get("/api/reports/005930")
        
        self.assertEqual(response.status_code, 200)
    
    def test_reports_detail_returns_json(self):
        """리포트 상세가 JSON을 반환하는지 확인"""
        response = self.client.get("/api/reports/005930")
        
        self.assertIn("application/json", response.get("Content-Type", ""))
        
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_reports_detail_has_required_fields(self):
        """리포트 상세 응답이 필수 필드를 포함하는지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # 필수 필드
        required_fields = ["profile", "price", "asOf", "source"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
    
    def test_reports_detail_profile_structure_when_exists(self):
        """프로필이 있을 때 올바른 구조인지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # profile이 있으면
        if data["profile"] is not None:
            profile = data["profile"]
            
            # 필수 필드
            self.assertIn("symbol", profile)
            self.assertIn("explanation", profile)
            
            # symbol이 일치
            self.assertEqual(profile["symbol"], "005930")
    
    def test_reports_detail_profile_none_when_not_exists(self):
        """존재하지 않는 심볼은 profile이 None"""
        response = self.client.get("/api/reports/INVALID999")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        
        # profile이 None이거나 source가 empty
        if data["source"] == "empty":
            self.assertIsNone(data["profile"])
    
    def test_reports_detail_has_additional_data_fields(self):
        """리포트에 추가 데이터 필드들이 있는지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # 선택적 필드들
        optional_fields = ["indicesSnippet", "articles"]
        for field in optional_fields:
            with self.subTest(field=field):
                # 있어도 되고 없어도 됨
                if field in data:
                    # articles는 리스트
                    if field == "articles":
                        self.assertIsInstance(data[field], list)
    
    def test_reports_detail_asof_format(self):
        """asOf 필드가 타임스탬프 형식인지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        asof = data["asOf"]
        self.assertIsNotNone(asof)
        self.assertIsInstance(asof, str)
        
        # ISO 8601 형식
        self.assertRegex(
            asof,
            r'\d{4}-\d{2}-\d{2}',
            "asOf should contain date"
        )
    
    def test_reports_detail_source_field(self):
        """source 필드가 올바른 값인지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # source는 "s3" 또는 "empty"
        self.assertIn(data["source"], ["s3", "empty"])
    
    def test_reports_detail_different_symbols(self):
        """다른 심볼들에 대해 응답이 오는지 확인"""
        symbols = ["005930", "000660", "035720"]  # 삼성전자, SK하이닉스, 카카오
        
        for symbol in symbols:
            with self.subTest(symbol=symbol):
                response = self.client.get(f"/api/reports/{symbol}")
                
                self.assertEqual(response.status_code, 200)
                data = response.json()
                
                # profile이 있으면 symbol 일치
                if data["profile"]:
                    self.assertEqual(data["profile"]["symbol"], symbol)
    
    def test_reports_detail_url_parameter(self):
        """URL 파라미터로 심볼을 받는지 확인"""
        # 심볼이 URL path에 있음
        response = self.client.get("/api/reports/TEST123")
        
        self.assertEqual(response.status_code, 200)
    
    def test_reports_detail_no_authentication_required(self):
        """리포트 상세는 인증 없이 접근 가능"""
        response = self.client.get("/api/reports/005930")
        
        self.assertNotEqual(response.status_code, 401)
        self.assertEqual(response.status_code, 200)
    
    def test_reports_detail_accepts_get_only(self):
        """리포트 상세는 GET만 허용"""
        symbol = "005930"
        
        # POST는 405
        response_post = self.client.post(f"/api/reports/{symbol}")
        self.assertEqual(response_post.status_code, 405)
        
        # PUT도 405
        response_put = self.client.put(f"/api/reports/{symbol}")
        self.assertEqual(response_put.status_code, 405)
        
        # DELETE도 405
        response_delete = self.client.delete(f"/api/reports/{symbol}")
        self.assertEqual(response_delete.status_code, 405)
    
    def test_reports_detail_price_field_structure(self):
        """price 필드가 있는지 확인 (향후 구현)"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # price 필드 존재
        self.assertIn("price", data)
        
        # 현재는 None일 수 있음 (향후 구현)
        # 있으면 dict 또는 None
        if data["price"] is not None:
            self.assertIsInstance(data["price"], dict)
    
    def test_reports_detail_degraded_handling(self):
        """S3 실패 시 적절히 처리하는지 확인"""
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # degraded 모드면 적절한 필드
        if "degraded" in data:
            self.assertIn("error", data)
    
    def test_reports_detail_response_time(self):
        """리포트 상세 응답이 합리적인 시간 내에 오는지"""
        import time
        
        start = time.time()
        response = self.client.get("/api/reports/005930")
        elapsed = time.time() - start
        
        # 10초 이내 응답 (S3 호출 가능)
        self.assertLess(elapsed, 10.0, "Response should be within 10 seconds")
        self.assertEqual(response.status_code, 200)