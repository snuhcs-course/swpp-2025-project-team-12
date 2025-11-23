# apps/api/tests/integration/test_mocks.py
"""
Mocks/mock_data.py 통합 테스트
Mock 데이터 생성 및 구조 검증
"""

from django.test import TestCase
from datetime import datetime


class MockDataTests(TestCase):
    """Mocks/mock_data.py 테스트"""
    
    def test_iso_now(self):
        """iso_now 함수"""
        from Mocks.mock_data import iso_now
        
        result = iso_now()
        
        self.assertIsInstance(result, str)
        self.assertIn('T', result)
        # ISO 8601 형식 확인
        parsed = datetime.fromisoformat(result)
        self.assertIsNotNone(parsed)
    
    def test_mock_indices(self):
        """MOCK_INDICES 상수"""
        from Mocks.mock_data import MOCK_INDICES
        
        self.assertIn('kospi', MOCK_INDICES)
        self.assertIn('kosdaq', MOCK_INDICES)
        self.assertIn('asOf', MOCK_INDICES)
        self.assertIn('source', MOCK_INDICES)
        
        self.assertEqual(MOCK_INDICES['source'], 'mock')
        self.assertIsInstance(MOCK_INDICES['kospi'], dict)
        self.assertIn('value', MOCK_INDICES['kospi'])
        self.assertIn('changePct', MOCK_INDICES['kospi'])
    
    def test_mock_articles(self):
        """MOCK_ARTICLES 상수"""
        from Mocks.mock_data import MOCK_ARTICLES
        
        self.assertIn('items', MOCK_ARTICLES)
        self.assertIn('asOf', MOCK_ARTICLES)
        self.assertIn('source', MOCK_ARTICLES)
        
        self.assertEqual(MOCK_ARTICLES['source'], 'mock')
        self.assertIsInstance(MOCK_ARTICLES['items'], list)
        self.assertGreater(len(MOCK_ARTICLES['items']), 0)
        
        # 첫 번째 아이템 구조 확인
        item = MOCK_ARTICLES['items'][0]
        self.assertIn('title', item)
        self.assertIn('url', item)
        self.assertIn('publisher', item)
        self.assertIn('publishedAt', item)
    
    def test_mock_recommendations(self):
        """mock_recommendations 함수"""
        from Mocks.mock_data import mock_recommendations
        
        result = mock_recommendations()
        
        self.assertIn('items', result)
        self.assertIn('asOf', result)
        self.assertIn('source', result)
        
        self.assertEqual(result['source'], 'mock')
        self.assertIsInstance(result['items'], list)
        self.assertGreater(len(result['items']), 0)
        
        # 첫 번째 추천 아이템 구조 확인
        item = result['items'][0]
        self.assertIn('ticker', item)
        self.assertIn('name', item)
        self.assertIn('market', item)
        self.assertIn('rank', item)
        self.assertIn('news', item)
        self.assertIn('reason', item)
        self.assertIn('expected_direction', item)
        self.assertIn('conviction', item)
        
        # 데이터 타입 확인
        self.assertIsInstance(item['ticker'], str)
        self.assertIsInstance(item['rank'], int)
        self.assertIsInstance(item['news'], list)
        self.assertIsInstance(item['reason'], list)
        self.assertIsInstance(item['conviction'], float)