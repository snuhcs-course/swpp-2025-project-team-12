# apps/api/tests/integration/test_views_final_coverage.py
"""
views.py 100% 커버리지 달성을 위한 최종 테스트
Line 116-134: company_profiles 일반 페이지네이션 경로
"""

from django.test import TestCase, Client
from unittest.mock import patch, Mock
import pandas as pd


class CompanyProfilesGeneralPaginationTest(TestCase):
    """company_profiles() 일반 페이지네이션 경로 (심볼 없이)"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/company-profiles"
    
    @patch('apps.api.views.FinanceS3Client')
    def test_company_profiles_general_pagination_path(self, mock_client_class):
        """
        심볼 없이 일반 페이지네이션 요청 (Line 116-131)
        이것이 성공하면 Line 116-131이 커버됨
        """
        mock_client = Mock()
        mock_client_class.return_value = mock_client
        
        # DataFrame 생성 (10개 항목)
        mock_df = pd.DataFrame({
            'explanation': [f'Company {i}' for i in range(1, 11)]
        }, index=[f'T{i:03d}' for i in range(1, 11)])
        
        mock_client.get_latest_json.return_value = (mock_df, "2025-11-01T10:00:00")
        
        # 심볼 없이 일반 페이지네이션 요청
        response = self.client.get(self.url, {
            "limit": 5,
            "offset": 2
        })
        data = response.json()
        
        # Line 116-131 커버 검증
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["total"], 10)  # Line 116: total = len(df)
        self.assertEqual(data["limit"], 5)   # Line 127
        self.assertEqual(data["offset"], 2)  # Line 128
        self.assertEqual(len(data["items"]), 5)  # Line 117-122: 페이지네이션
        self.assertEqual(data["source"], "s3")  # Line 129
        self.assertIn("asOf", data)  # Line 130
        
        # offset=2이므로 T003부터 시작
        self.assertEqual(data["items"][0]["ticker"], "T003")
        self.assertEqual(data["items"][4]["ticker"], "T007")
    
    @patch('apps.api.views.FinanceS3Client')
    def test_company_profiles_general_pagination_no_offset(self, mock_client_class):
        """
        offset=0인 기본 페이지네이션 (Line 116-131)
        """
        mock_client = Mock()
        mock_client_class.return_value = mock_client
        
        # DataFrame 생성
        mock_df = pd.DataFrame({
            'explanation': ['Company A', 'Company B', 'Company C']
        }, index=['A001', 'B002', 'C003'])
        
        mock_client.get_latest_json.return_value = (mock_df, "2025-11-01")
        
        # 기본 페이지네이션
        response = self.client.get(self.url, {"limit": 2})
        data = response.json()
        
        # 검증
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["total"], 3)
        self.assertEqual(len(data["items"]), 2)
        self.assertEqual(data["items"][0]["ticker"], "A001")
    
    @patch('apps.api.views.FinanceS3Client')
    def test_company_profiles_exception_in_pagination(self, mock_client_class):
        """
        Exception 발생 시 degraded (Line 133-134)
        """
        mock_client = Mock()
        mock_client_class.return_value = mock_client
        
        # Exception 발생
        mock_client.get_latest_json.side_effect = Exception("S3 Connection Error")
        
        response = self.client.get(self.url, {"limit": 10})
        data = response.json()
        
        # Line 133-134 커버
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "s3")
        self.assertEqual(data["total"], 0)
        self.assertIn("error", data)