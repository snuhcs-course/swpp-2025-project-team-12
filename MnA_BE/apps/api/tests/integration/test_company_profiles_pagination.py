# apps/api/tests/integration/test_company_profiles_pagination.py
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
    
    def test_company_profiles_general_pagination_path(self):
        """심볼 없이 일반 페이지네이션 요청 (실제 메모리 데이터 사용)"""
        # 심볼 없이 일반 페이지네이션 요청
        response = self.client.get(self.url, {
            "limit": 5,
            "offset": 2
        })
        data = response.json()
        
        # 실제 데이터 검증
        self.assertEqual(response.status_code, 200)
        self.assertGreater(data["total"], 0)  # 실제 데이터가 있음
        self.assertEqual(data["limit"], 5)
        self.assertEqual(data["offset"], 2)
        self.assertEqual(len(data["items"]), min(5, data["total"] - 2))  # offset 고려
        self.assertEqual(data["source"], "memory")
        self.assertIn("asOf", data)
    
    def test_company_profiles_general_pagination_no_offset(self):
        """offset=0인 기본 페이지네이션 (실제 메모리 데이터 사용)"""
        # 기본 페이지네이션
        response = self.client.get(self.url, {"limit": 2})
        data = response.json()
        
        # 검증
        self.assertEqual(response.status_code, 200)
        self.assertGreater(data["total"], 0)
        self.assertEqual(len(data["items"]), min(2, data["total"]))
        self.assertIn("ticker", data["items"][0])
    
    @patch('apps.api.views.ApiConfig')
    def test_company_profiles_exception_in_pagination(self, mock_config):
        """Exception 발생 시 degraded"""
        # ApiConfig.profile_df를 None으로 설정
        mock_config.profile_df = None
        mock_config.instant_df = None
        
        response = self.client.get(self.url, {"limit": 10})
        data = response.json()
        
        # degraded 응답
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "memory")
        self.assertEqual(data["total"], 0)
        self.assertIn("error", data)