from django.test import TestCase, RequestFactory
from MnA_BE.utils.pagination import get_pagination


class PaginationUtilTests(TestCase):
    """
    pagination.py의 get_pagination() 함수 유닛 테스트
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.factory = RequestFactory()
    
    def test_default_values(self):
        """파라미터 없으면 기본값 사용"""
        request = self.factory.get('/test/')
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20, "Default limit should be 20")
        self.assertEqual(offset, 0, "Default offset should be 0")
    
    def test_custom_default_limit(self):
        """커스텀 기본 limit"""
        request = self.factory.get('/test/')
        
        limit, offset = get_pagination(request, default_limit=50)
        
        self.assertEqual(limit, 50)
        self.assertEqual(offset, 0)
    
    def test_custom_limit_and_offset(self):
        """커스텀 limit과 offset 적용"""
        request = self.factory.get('/test/', {'limit': '10', 'offset': '5'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 10)
        self.assertEqual(offset, 5)
    
    def test_max_limit_enforcement(self):
        """최대 limit 강제"""
        request = self.factory.get('/test/', {'limit': '1000'})
        
        limit, offset = get_pagination(request, max_limit=100)
        
        self.assertEqual(limit, 100, "Limit should be capped at max_limit")
    
    def test_custom_max_limit(self):
        """커스텀 max_limit"""
        request = self.factory.get('/test/', {'limit': '200'})
        
        limit, offset = get_pagination(request, max_limit=50)
        
        self.assertEqual(limit, 50)
    
    def test_negative_limit_becomes_minimum(self):
        """음수 limit은 1로 변환"""
        request = self.factory.get('/test/', {'limit': '-5'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 1, "Negative limit should become 1")
    
    def test_zero_limit_becomes_minimum(self):
        """0 limit은 1로 변환"""
        request = self.factory.get('/test/', {'limit': '0'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 1, "Zero limit should become 1")
    
    def test_negative_offset_becomes_zero(self):
        """음수 offset은 0으로 변환"""
        request = self.factory.get('/test/', {'offset': '-10'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(offset, 0, "Negative offset should become 0")
    
    def test_invalid_limit_uses_default(self):
        """잘못된 limit 형식은 기본값 사용"""
        request = self.factory.get('/test/', {'limit': 'invalid'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20, "Invalid limit should use default")
    
    def test_invalid_offset_uses_zero(self):
        """잘못된 offset 형식은 0 사용"""
        request = self.factory.get('/test/', {'offset': 'invalid'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(offset, 0, "Invalid offset should use 0")
    
    def test_float_limit_converts_to_int(self):
        """소수점 limit은 ValueError → 기본값 사용"""
        request = self.factory.get('/test/', {'limit': '15.7'})
        
        limit, offset = get_pagination(request)
        
        # int('15.7')은 ValueError 발생 → 기본값 20 사용
        self.assertEqual(limit, 20)
    
    def test_float_offset_converts_to_int(self):
        """소수점 offset은 ValueError → 0 사용"""
        request = self.factory.get('/test/', {'offset': '10.9'})
        
        limit, offset = get_pagination(request)
        
        # int('10.9')은 ValueError 발생 → 기본값 0 사용
        self.assertEqual(offset, 0)
    
    def test_empty_string_limit(self):
        """빈 문자열 limit은 기본값 사용"""
        request = self.factory.get('/test/', {'limit': ''})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20)
    
    def test_empty_string_offset(self):
        """빈 문자열 offset은 0 사용"""
        request = self.factory.get('/test/', {'offset': ''})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(offset, 0)
    
    def test_very_large_limit(self):
        """매우 큰 limit은 max_limit로 제한"""
        request = self.factory.get('/test/', {'limit': '999999999'})
        
        limit, offset = get_pagination(request, max_limit=100)
        
        self.assertEqual(limit, 100)
    
    def test_very_large_offset(self):
        """매우 큰 offset도 허용 (상한 없음)"""
        request = self.factory.get('/test/', {'offset': '999999'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(offset, 999999, "Large offset should be allowed")
    
    def test_limit_exactly_at_max(self):
        """limit이 정확히 max_limit"""
        request = self.factory.get('/test/', {'limit': '100'})
        
        limit, offset = get_pagination(request, max_limit=100)
        
        self.assertEqual(limit, 100)
    
    def test_limit_one_over_max(self):
        """limit이 max_limit보다 1 큼"""
        request = self.factory.get('/test/', {'limit': '101'})
        
        limit, offset = get_pagination(request, max_limit=100)
        
        self.assertEqual(limit, 100)
    
    def test_both_parameters_invalid(self):
        """둘 다 잘못된 값이면 기본값"""
        request = self.factory.get('/test/', {'limit': 'abc', 'offset': 'xyz'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20)
        self.assertEqual(offset, 0)
    
    def test_special_characters_in_parameters(self):
        """특수 문자는 ValueError → 기본값"""
        request = self.factory.get('/test/', {'limit': '10!@#', 'offset': '5$%^'})
        
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20)
        self.assertEqual(offset, 0)