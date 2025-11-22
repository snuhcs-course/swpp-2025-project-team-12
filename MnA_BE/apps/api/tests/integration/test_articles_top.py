# apps/api/tests/integration/test_articles_top_coverage.py
"""
articles/top.py 100% 커버리지 달성을 위한 테스트
Missing lines: 40-47 (Mock fallback)
"""
from django.test import TestCase, Client
from unittest.mock import patch


class TopArticlesEdgeCasesTests(TestCase):
    """top articles 엣지 케이스"""
    
    def setUp(self):
        self.client = Client()
        self.url = "/api/articles/top"
    
    @patch('apps.api.articles.top.ARTICLES_SOURCE', 'mock')
    def test_top_articles_mock_fallback(self):
        """ARTICLES_SOURCE가 mock일 때 (lines 40-47)"""
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["source"], "mock")
        self.assertIn("items", data)
        self.assertIn("total", data)
        self.assertIn("limit", data)
        self.assertIn("offset", data)
        self.assertIn("asOf", data)
    
    @patch('apps.api.articles.top.ARTICLES_SOURCE', 's3')
    @patch('apps.api.articles.top.FinanceBucket')
    def test_top_articles_s3_exception(self, mock_bucket_class):
        """S3에서 예외 발생 시"""
        mock_s3 = mock_bucket_class.return_value
        mock_s3.get_latest_json.side_effect = Exception("S3 error")
        
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data.get("degraded"))
        self.assertEqual(data["source"], "s3")