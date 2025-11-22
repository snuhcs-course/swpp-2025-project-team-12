# apps/api/tests/integration/test_views.py
"""
views.py 100% 커버리지 달성을 위한 추가 테스트
누락된 라인: 23, 26-27, 33, 36-37, 93-99, 117, 159, 168-178, 199, 237-239, 
            256, 315-317, 325-330, 353, 390-392, 494, 553-557
"""
from django.test import TestCase, Client
from unittest.mock import patch, Mock
from apps.api.views import safe_float, safe_int
import pandas as pd
import numpy as np


class SafeFloatIntTests(TestCase):
    """safe_float, safe_int 함수 테스트 (lines 20-37)"""
    
    def test_safe_float_with_none(self):
        """safe_float with None (line 23)"""
        self.assertIsNone(safe_float(None))
    
    def test_safe_float_with_nan(self):
        """safe_float with NaN (line 23)"""
        self.assertIsNone(safe_float(np.nan))
    
    def test_safe_float_with_invalid_string(self):
        """safe_float with invalid string (lines 26-27)"""
        self.assertIsNone(safe_float("invalid"))
        self.assertIsNone(safe_float("abc"))
    
    def test_safe_float_with_valid_value(self):
        """safe_float with valid value"""
        self.assertEqual(safe_float("123.45"), 123.45)
        self.assertEqual(safe_float(123), 123.0)
    
    def test_safe_int_with_none(self):
        """safe_int with None (line 33)"""
        self.assertIsNone(safe_int(None))
    
    def test_safe_int_with_nan(self):
        """safe_int with NaN (line 33)"""
        self.assertIsNone(safe_int(np.nan))
    
    def test_safe_int_with_invalid_string(self):
        """safe_int with invalid string (lines 36-37)"""
        self.assertIsNone(safe_int("invalid"))
        self.assertIsNone(safe_int("abc"))
    
    def test_safe_int_with_valid_value(self):
        """safe_int with valid value"""
        self.assertEqual(safe_int("123"), 123)
        self.assertEqual(safe_int(123.7), 123)


class ReloadDataTests(TestCase):
    """reload_data 엔드포인트 테스트 (lines 93-99)"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/reload-data"
    
    @patch('apps.api.views.instant_data.reload')
    def test_reload_data_success(self, mock_reload):
        """reload_data 성공 (line 94)"""
        from django.http import JsonResponse
        mock_reload.return_value = JsonResponse({"status": "ok"})
        response = self.client.post(self.url)  # POST로 변경
        self.assertEqual(response.status_code, 200)
    
    @patch('apps.api.views.instant_data.reload')
    def test_reload_data_exception(self, mock_reload):
        """reload_data 예외 발생 (lines 95-99)"""
        mock_reload.side_effect = Exception("Reload failed")
        response = self.client.post(self.url)  # POST로 변경
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "reload")


class IndicesEdgeCasesTests(TestCase):
    """indices 엔드포인트 엣지 케이스 (lines 117, 159, 168-178)"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/indices"
    
    @patch('apps.api.views.INDICES_SOURCE', 's3')
    @patch('apps.api.views.FinanceBucket')
    def test_indices_no_contents_in_s3(self, mock_bucket_class):
        """S3에 Contents 없을 때 (line 117)"""
        mock_s3 = Mock()
        mock_bucket_class.return_value = mock_s3
        
        # ✅ get_list_v2 직접 mock
        mock_s3.get_list_v2.return_value = {}
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "s3")
        self.assertIn("kospi", data)
        self.assertIn("kosdaq", data)
    
    @patch('apps.api.views.INDICES_SOURCE', 's3')
    @patch('apps.api.views.FinanceBucket')
    def test_indices_only_kosdaq_file(self, mock_bucket_class):
        """KOSDAQ 파일만 있을 때 (line 159)"""
        mock_s3 = Mock()
        mock_bucket_class.return_value = mock_s3
        
        from datetime import datetime
        
        # ✅ KOSDAQ만 반환
        mock_s3.get_list_v2.return_value = {
            'Contents': [
                {'Key': 'indices/KOSDAQ.json', 'LastModified': datetime.now()}
            ]
        }
        
        mock_s3.get_json.return_value = {
            'close': 750.5,
            'change_percent': -0.5,
            'fetched_at': '2025-11-21T10:00:00'
        }
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["source"], "s3")
        self.assertIsNotNone(data.get("asOf"))
    
    @patch('apps.api.views.INDICES_SOURCE', 's3')
    @patch('apps.api.views.FinanceBucket')
    def test_indices_only_kospi_file(self, mock_bucket_class):
        """KOSPI 파일만 있을 때 (line 159)"""
        mock_s3 = Mock()
        mock_bucket_class.return_value = mock_s3
        
        from datetime import datetime
        
        # ✅ get_list_v2로 KOSPI만 반환
        mock_s3.get_list_v2.return_value = {
            'Contents': [
                {'Key': 'indices/KOSPI.json', 'LastModified': datetime.now()}
            ]
        }
        
        mock_s3.get_json.return_value = {
            'close': 2500.5,
            'change_percent': 1.2,
            'fetched_at': '2025-11-21T10:00:00'
        }
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["source"], "s3")
        self.assertIsNotNone(data.get("asOf"))
    
    @patch('apps.api.views.INDICES_SOURCE', 's3')
    @patch('apps.api.views.FinanceBucket')
    def test_indices_s3_exception(self, mock_bucket_class):
        """S3 예외 발생 (lines 168-178)"""
        mock_s3 = Mock()
        mock_bucket_class.return_value = mock_s3
        
        # ✅ get_list_v2에서 예외 발생
        mock_s3.get_list_v2.side_effect = Exception("S3 error")
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "s3")
    
    @patch('apps.api.views.INDICES_SOURCE', 'mock')
    def test_indices_mock_fallback(self):
        """INDICES_SOURCE가 mock일 때 (line 178)"""
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertIn("kospi", data)
        self.assertIn("kosdaq", data)


class CompanyListEdgeCasesTests(TestCase):
    """company_list 엣지 케이스 (lines 199, 237-239)"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/company-list"
    
    @patch('apps.api.views.store.get_data')
    def test_company_list_cache_none(self, mock_get_data):
        """캐시에 데이터 없을 때 (line 199)"""
        mock_get_data.return_value = None
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "cache")
        self.assertEqual(data["total"], 0)
    
    @patch('apps.api.views.store.get_data')
    def test_company_list_exception(self, mock_get_data):
        """company_list 예외 발생 (lines 237-239)"""
        mock_get_data.side_effect = Exception("Cache error")
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "cache")


class CompanyProfilesEdgeCasesTests(TestCase):
    """company_profiles 엣지 케이스 (lines 256, 315-317)"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/company-profiles"
    
    @patch('apps.api.views.store.get_data')
    def test_company_profiles_cache_none(self, mock_get_data):
        """캐시에 프로필 데이터 없을 때 (line 256)"""
        mock_get_data.return_value = None
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "cache")
    
    @patch('apps.api.views.store.get_data')
    def test_company_profiles_exception(self, mock_get_data):
        """company_profiles 예외 발생 (lines 315-317)"""
        mock_get_data.side_effect = Exception("Profile error")
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))


class CompanyOverviewTests(TestCase):
    """company_overview 엔드포인트 테스트 (lines 325-330)"""
    
    def setUp(self):
        self.client = Client()
    
    @patch('apps.api.views.get_latest_overview')
    def test_company_overview_success(self, mock_get_overview):
        """company_overview 성공"""
        mock_get_overview.return_value = {
            '005930': '{"summary": "Test summary"}'
        }
        
        response = self.client.get("/api/overview/005930")
        
        self.assertEqual(response.status_code, 200)
    
    @patch('apps.api.views.get_latest_overview')
    def test_company_overview_exception(self, mock_get_overview):
        """company_overview 예외 발생 (lines 327-328)"""
        mock_get_overview.side_effect = Exception("Overview error")
        
        response = self.client.get("/api/overview/005930")
        
        self.assertEqual(response.status_code, 500)


class ReportsDetailEdgeCasesTests(TestCase):
    """reports_detail 엣지 케이스 (lines 353, 390-392, 494, 553-557)"""
    
    def setUp(self):
        self.client = Client()
    
    @patch('apps.api.views.store.get_data')
    def test_reports_instant_df_none(self, mock_get_data):
        """instant_df가 None일 때 (line 353)"""
        def side_effect(key):
            if key == 'instant_df':
                return None
            return pd.DataFrame()
        
        mock_get_data.side_effect = side_effect
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "cache")
    
    @patch('apps.api.views.store.get_data')
    def test_reports_history_exception(self, mock_get_data):
        """history 처리 중 예외 (lines 390-392)"""
        # ✅ 컬럼이 빠진 DataFrame으로 KeyError 유도
        bad_df = pd.DataFrame({
            'ticker': ['005930'],
            'date': [pd.Timestamp('2025-01-01')],
            'close': [1000.0],
            # 'change' 컬럼 없음 → KeyError
            # 'change_rate' 컬럼 없음 → KeyError
            'market_cap': [1000000],
            'PER': [10.0],
            'PBR': [1.0],
            'EPS': [100.0],
            'BPS': [1000.0],
            'DIV': [1.0],
            'DPS': [100.0],
            'ROE': [5.0],
        })
        
        def side_effect(key):
            if key == 'instant_df':
                return bad_df
            return pd.DataFrame()
        
        mock_get_data.side_effect = side_effect
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        # 예외가 발생해도 응답은 200
        self.assertEqual(response.status_code, 200)
        self.assertIn("history", data)
    
    @patch('apps.api.views.INDICES_SOURCE', 's3')
    @patch('apps.api.views.FinanceBucket')
    @patch('apps.api.views.store.get_data')
    def test_reports_indices_exception(self, mock_get_data, mock_bucket_class):
        """reports에서 indices 가져올 때 예외 (lines 490-492)"""
        # 기본 데이터 제공
        mock_get_data.return_value = pd.DataFrame({
            'ticker': ['005930'],
            'date': [pd.Timestamp('2025-01-01')],
            'name': ['Samsung'],
            'market': ['KOSPI'],
            'industry': ['Tech'],
            'close': [70000],
            'change': [1000],
            'change_rate': [1.5],
            'market_cap': [400000000],
            'PER': [15.0],
            'PBR': [1.2],
            'EPS': [5000],
            'BPS': [60000],
            'DIV': [2.0],
            'DPS': [1400],
            'ROE': [8.0]
        })
        
        # ✅ FinanceBucket의 get_list_v2에서 예외 발생
        mock_s3 = Mock()
        mock_bucket_class.return_value = mock_s3
        mock_s3.get_list_v2.side_effect = Exception("S3 indices error")
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        # indices 예외 발생해도 나머지 데이터는 정상 반환
    
    @patch('apps.api.views.INDICES_SOURCE', 'mock')
    @patch('apps.api.views.store.get_data')
    def test_reports_mock_indices(self, mock_get_data):
        """INDICES_SOURCE가 mock일 때 (line 494)"""
        mock_get_data.return_value = pd.DataFrame()
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(data.get("indicesSnippet"))
    
    @patch('apps.api.views.store.get_data')
    def test_reports_general_exception(self, mock_get_data):
        """reports 일반 예외 (lines 553-557)"""
        mock_get_data.side_effect = Exception("Unexpected error")
        
        response = self.client.get("/api/reports/005930")
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "cache")