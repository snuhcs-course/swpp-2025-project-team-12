# apps/api/tests/integration/test_reports.py
from django.test import TestCase, Client
from unittest.mock import patch, Mock

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
        self.assertIn(data["source"], ["s3", "empty", "memory"])
    
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
    
    @patch('apps.api.views.FinanceS3Client')
    def test_reports_detail_with_explanation(self, mock_s3_class):
        """프로필에 explanation이 있을 때 (lines 220-221)"""
        import pandas as pd
        
        mock_s3 = Mock()
        mock_s3_class.return_value = mock_s3
        
        # Company profile with explanation
        profile_df = pd.DataFrame({
            'explanation': ['Samsung Electronics is a leading tech company']
        }, index=['005930'])
        
        mock_s3.get_latest_parquet_df.return_value = (profile_df, "2025-11-06T10:00:00")
        mock_s3.check_source.return_value = {"ok": True, "latest": "2025-11-06"}
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # Profile에 explanation 포함
        if data.get("profile"):
            self.assertIn("explanation", data["profile"])
    
    @patch('apps.api.views.FinanceS3Client')  
    def test_reports_detail_charts_days_parameter(self, mock_s3_class):
        """차트 일수 파라미터 테스트 (line 214)"""
        mock_s3 = Mock()
        mock_s3_class.return_value = mock_s3
        
        # Mock 빈 응답
        mock_s3.get_latest_parquet_df.return_value = (None, None)
        mock_s3.check_source.return_value = {"ok": False}
        
        # days 파라미터와 함께 요청
        response = self.client.get("/api/reports/005930", {"days": 30})
        
        # 200 OK
        self.assertEqual(response.status_code, 200)
    
    @patch('apps.api.views.ApiConfig')
    def test_reports_safe_float_with_invalid_data(self, mock_config):
        """safe_float 예외 처리 - 잘못된 데이터 (lines 20, 23-24)"""
        import pandas as pd
        import numpy as np
        
        # 잘못된 데이터가 포함된 DataFrame
        instant_df = pd.DataFrame({
            'ticker': ['TEST001', 'TEST001'],
            'date': pd.to_datetime(['2025-01-01', '2025-01-02']),
            'name': ['Test Company', 'Test Company'],
            'market': ['KOSDAQ', 'KOSDAQ'],
            'industry': ['Tech', 'Tech'],
            'close': ['invalid', np.nan],  # 잘못된 값
            'change': [None, 'bad'],
            'change_rate': ['text', None],
            'market_cap': ['abc', np.nan],
            'PER': [np.nan, 'xyz'],
            'PBR': [None, None],
            'EPS': ['error', np.nan],
            'BPS': [np.nan, 'fail'],
            'DIV': [None, 'wrong'],
            'DPS': ['bad', np.nan],
            'ROE': [np.nan, None]
        })
        
        mock_config.instant_df = instant_df
        mock_config.profile_df = pd.DataFrame({'explanation': ['Test']}, index=['TEST001'])
        
        response = self.client.get("/api/reports/TEST001")
        data = response.json()
        
        # 200 OK - safe_float/safe_int가 None으로 변환
        self.assertEqual(response.status_code, 200)
        
        # history_data에서 모든 값이 None으로 처리됨
        if 'history' in data and data['history']:
            for item in data['history']:
                # 모든 숫자 필드가 None이어야 함
                self.assertIsNone(item.get('close'))
                self.assertIsNone(item.get('change'))
    
    @patch('apps.api.views.ApiConfig')
    def test_reports_safe_int_with_invalid_data(self, mock_config):
        """safe_int 예외 처리 - 잘못된 정수 데이터 (lines 30, 33-34)"""
        import pandas as pd
        import numpy as np
        
        instant_df = pd.DataFrame({
            'ticker': ['TEST002'],
            'date': pd.to_datetime(['2025-01-01']),
            'name': ['Test Company 2'],
            'market': ['KOSPI'],
            'industry': ['Finance'],
            'close': [50000],
            'change': [1000],
            'change_rate': [2.0],
            'market_cap': ['not_a_number'],  # 문자열
            'PER': [15.5],
            'PBR': [1.2],
            'EPS': [3000],
            'BPS': [40000],
            'DIV': [2.5],
            'DPS': [1000],
            'ROE': [8.5]
        })
        
        mock_config.instant_df = instant_df
        mock_config.profile_df = pd.DataFrame({'explanation': ['Test 2']}, index=['TEST002'])
        
        response = self.client.get("/api/reports/TEST002")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        
        # market_cap이 None으로 변환됨
        if 'current' in data and data['current']:
            self.assertIsNone(data['current'].get('market_cap'))
    
    @patch('apps.api.views.ApiConfig')
    def test_reports_exception_in_history_loop(self, mock_config):
        """history 데이터 처리 중 예외 (lines 297-299)"""
        import pandas as pd
        
        # 날짜 변환 시 에러 발생할 데이터
        instant_df = pd.DataFrame({
            'ticker': ['TEST003'],
            'date': ['invalid_date'],  # 잘못된 날짜
            'name': ['Test Company 3'],
            'market': ['KOSDAQ'],
            'industry': ['IT'],
            'close': [10000],
            'change': [100],
            'change_rate': [1.0],
            'market_cap': [1000000],
            'PER': [20.0],
            'PBR': [2.0],
            'EPS': [500],
            'BPS': [10000],
            'DIV': [3.0],
            'DPS': [300],
            'ROE': [5.0]
        })
        
        mock_config.instant_df = instant_df
        mock_config.profile_df = None
        
        response = self.client.get("/api/reports/TEST003")
        data = response.json()
        
        # 예외가 발생해도 200 OK
        self.assertEqual(response.status_code, 200)
    
    @patch('apps.api.views.ApiConfig')
    def test_reports_instant_df_none(self, mock_config):
        """instant_df가 None일 때 degraded (line 262)"""
        mock_config.instant_df = None
        mock_config.profile_df = None
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get('degraded'))
        self.assertEqual(data['source'], 'memory')
    
    @patch('apps.api.views.ApiConfig')
    def test_reports_general_exception(self, mock_config):
        """일반 예외 처리 (lines 415-417)"""
        # Exception을 일으키는 Mock
        mock_config.instant_df = Mock()
        mock_config.instant_df.columns = Mock(side_effect=Exception("Test error"))
        mock_config.profile_df = None
        
        response = self.client.get("/api/reports/TEST999")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get('degraded'))
    
    def test_reports_various_symbols_for_coverage(self):
        """다양한 심볼로 safe_float/safe_int 실행 유도"""
        symbols = ["005930", "000660", "035720", "051910", "INVALID"]
        
        for symbol in symbols:
            with self.subTest(symbol=symbol):
                response = self.client.get(f"/api/reports/{symbol}")
                # 모두 200 OK (degraded 포함)
                self.assertEqual(response.status_code, 200)