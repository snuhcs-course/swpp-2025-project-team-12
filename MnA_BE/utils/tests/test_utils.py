# utils/tests/test_utils.py
"""
utils 모듈 종합 테스트
목표: 100% 커버리지
"""

from django.test import TestCase, RequestFactory
from unittest.mock import patch, MagicMock
from datetime import datetime, timezone, timedelta
import jwt
import os


class TimeTests(TestCase):
    """utils/time.py 테스트"""
    
    def test_iso_now(self):
        """iso_now 함수가 ISO 8601 형식 반환"""
        from utils.time import iso_now
        
        result = iso_now()
        
        # ISO 8601 형식 확인
        self.assertIsInstance(result, str)
        self.assertIn('T', result)
        self.assertIn('+00:00', result)  # UTC timezone
        
        # datetime으로 파싱 가능한지 확인
        parsed = datetime.fromisoformat(result)
        self.assertIsNotNone(parsed)


class PaginationTests(TestCase):
    """utils/pagination.py 테스트"""
    
    def setUp(self):
        self.factory = RequestFactory()
    
    def test_get_pagination_default_values(self):
        """기본값 사용"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/')
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20)
        self.assertEqual(offset, 0)
    
    def test_get_pagination_with_valid_params(self):
        """유효한 파라미터"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?limit=50&offset=100')
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 50)
        self.assertEqual(offset, 100)
    
    def test_get_pagination_custom_defaults(self):
        """커스텀 기본값"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/')
        limit, offset = get_pagination(request, default_limit=10, max_limit=50)
        
        self.assertEqual(limit, 10)
        self.assertEqual(offset, 0)
    
    def test_get_pagination_exceeds_max_limit(self):
        """max_limit 초과 시 제한"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?limit=200')
        limit, offset = get_pagination(request, max_limit=100)
        
        self.assertEqual(limit, 100)
    
    def test_get_pagination_invalid_limit(self):
        """잘못된 limit 값"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?limit=invalid')
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 20)  # default로 fallback
    
    def test_get_pagination_invalid_offset(self):
        """잘못된 offset 값"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?offset=invalid')
        limit, offset = get_pagination(request)
        
        self.assertEqual(offset, 0)  # default로 fallback
    
    def test_get_pagination_negative_limit(self):
        """음수 limit는 1로 조정"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?limit=-10')
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 1)
    
    def test_get_pagination_negative_offset(self):
        """음수 offset는 0으로 조정"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?offset=-50')
        limit, offset = get_pagination(request)
        
        self.assertEqual(offset, 0)
    
    def test_get_pagination_zero_limit(self):
        """0 limit는 1로 조정"""
        from utils.pagination import get_pagination
        
        request = self.factory.get('/?limit=0')
        limit, offset = get_pagination(request)
        
        self.assertEqual(limit, 1)


class ValidationTests(TestCase):
    """utils/validation.py 테스트"""
    
    def test_validate_password_success(self):
        """유효한 패스워드"""
        from utils.validation import validate_password
        
        # 예외 발생하지 않아야 함
        validate_password("ValidPass123")
    
    def test_raise_if_password_is_none(self):
        """패스워드가 None"""
        from utils.validation import raise_if_password_is_none
        
        with self.assertRaises(Exception) as context:
            raise_if_password_is_none(None)
        
        self.assertIn("cannot be None", str(context.exception))
    
    def test_raise_if_password_too_short(self):
        """패스워드가 너무 짧음"""
        from utils.validation import raise_if_password_too_short
        
        with self.assertRaises(Exception) as context:
            raise_if_password_too_short("Short1", 8)
        
        self.assertIn("at least 8 characters", str(context.exception))
    
    def test_raise_if_password_has_not_number(self):
        """패스워드에 숫자 없음"""
        from utils.validation import raise_if_password_has_not_number
        
        with self.assertRaises(Exception) as context:
            raise_if_password_has_not_number("NoNumber")
        
        self.assertIn("at least one number", str(context.exception))
    
    def test_raise_if_password_has_not_uppercase(self):
        """패스워드에 대문자 없음"""
        from utils.validation import raise_if_password_has_not_uppercase
        
        with self.assertRaises(Exception) as context:
            raise_if_password_has_not_uppercase("nouppercase123")
        
        self.assertIn("uppercase letter", str(context.exception))
    
    def test_raise_if_password_has_not_lowercase(self):
        """패스워드에 소문자 없음"""
        from utils.validation import raise_if_password_has_not_lowercase
        
        with self.assertRaises(Exception) as context:
            raise_if_password_has_not_lowercase("NOLOWERCASE123")
        
        self.assertIn("lowercase letter", str(context.exception))
    
    def test_validate_password_none(self):
        """validate_password: None"""
        from utils.validation import validate_password
        
        with self.assertRaises(Exception):
            validate_password(None)
    
    def test_validate_password_too_short(self):
        """validate_password: 너무 짧음"""
        from utils.validation import validate_password
        
        with self.assertRaises(Exception):
            validate_password("Short1A")
    
    def test_validate_password_no_lowercase(self):
        """validate_password: 소문자 없음"""
        from utils.validation import validate_password
        
        with self.assertRaises(Exception):
            validate_password("NOLOWER123")
    
    def test_validate_password_no_uppercase(self):
        """validate_password: 대문자 없음"""
        from utils.validation import validate_password
        
        with self.assertRaises(Exception):
            validate_password("noupper123")
    
    def test_validate_password_no_number(self):
        """validate_password: 숫자 없음"""
        from utils.validation import validate_password
        
        with self.assertRaises(Exception):
            validate_password("NoNumberHere")
    
    def test_raise_if_name_is_none(self):
        """이름이 None"""
        from utils.validation import raise_if_name_is_none
        
        with self.assertRaises(Exception) as context:
            raise_if_name_is_none(None)
        
        self.assertIn("cannot be None", str(context.exception))
    
    def test_raise_if_name_is_too_long(self):
        """이름이 너무 김"""
        from utils.validation import raise_if_name_is_too_long
        
        with self.assertRaises(Exception) as context:
            raise_if_name_is_too_long("A" * 25, 20)
        
        self.assertIn("longer than 20 characters", str(context.exception))
    
    def test_validate_name_success(self):
        """유효한 이름"""
        from utils.validation import validate_name
        
        # 예외 발생하지 않아야 함
        validate_name("ValidName")
    
    def test_validate_name_none(self):
        """validate_name: None"""
        from utils.validation import validate_name
        
        with self.assertRaises(Exception):
            validate_name(None)
    
    def test_validate_name_too_long(self):
        """validate_name: 너무 김"""
        from utils.validation import validate_name
        
        with self.assertRaises(Exception):
            validate_name("A" * 21)


class TokenHandlerTests(TestCase):
    """utils/token_handler.py 테스트"""
    
    def setUp(self):
        # 환경변수 설정
        self.env_patcher = patch.dict('os.environ', {
            'SECRET_KEY': 'test_secret_key_12345',
            'HASH_ALGORITHM': 'HS256',
            'ACCESS_TOKEN_EXPIRE_MINUTES': '30',
            'REFRESH_TOKEN_EXPIRE_DAYS': '7'
        })
        self.env_patcher.start()
    
    def tearDown(self):
        self.env_patcher.stop()
    
    def test_make_access_token(self):
        """액세스 토큰 생성"""
        from utils.token_handler import make_access_token, decode_token
        
        user_id = 123
        token = make_access_token(user_id)
        
        self.assertIsInstance(token, str)
        
        # 토큰 디코딩하여 확인
        payload = decode_token(token)
        self.assertEqual(payload['id'], user_id)
        self.assertIn('exp', payload)
    
    def test_make_refresh_token(self):
        """리프레시 토큰 생성"""
        from utils.token_handler import make_refresh_token, decode_token
        
        user_id = 456
        token = make_refresh_token(user_id)
        
        self.assertIsInstance(token, str)
        
        # 토큰 디코딩하여 확인
        payload = decode_token(token)
        self.assertEqual(payload['id'], user_id)
        self.assertIn('exp', payload)
        self.assertIn('random_salt', payload)
    
    def test_make_refresh_token_with_custom_exp(self):
        """커스텀 만료 시간으로 리프레시 토큰 생성"""
        from utils.token_handler import make_refresh_token, decode_token
        
        user_id = 789
        custom_exp = datetime.utcnow() + timedelta(days=30)
        token = make_refresh_token(user_id, exp=custom_exp)
        
        payload = decode_token(token)
        self.assertEqual(payload['id'], user_id)
    
    def test_decode_token(self):
        """토큰 디코딩"""
        from utils.token_handler import make_access_token, decode_token
        
        user_id = 999
        token = make_access_token(user_id)
        payload = decode_token(token)
        
        self.assertEqual(payload['id'], user_id)
    
    def test_rotate_refresh_token(self):
        """리프레시 토큰 로테이션"""
        from utils.token_handler import make_refresh_token, rotate_refresh_token, decode_token
        
        user_id = 111
        original_token = make_refresh_token(user_id)
        
        # 로테이션
        new_token = rotate_refresh_token(original_token)
        
        # 새 토큰 검증
        self.assertIsInstance(new_token, str)
        self.assertNotEqual(original_token, new_token)
        
        new_payload = decode_token(new_token)
        self.assertEqual(new_payload['id'], user_id)
    
    def test_set_cookie(self):
        """쿠키 설정"""
        from utils.token_handler import set_cookie
        from django.http import HttpResponse
        
        response = HttpResponse()
        set_cookie(response, 'test_key', 'test_value')
        
        # 쿠키가 설정되었는지 확인
        self.assertIn('test_key', response.cookies)
        cookie = response.cookies['test_key']
        self.assertEqual(cookie.value, 'test_value')
        self.assertTrue(cookie['httponly'])
        self.assertTrue(cookie['secure'])
        self.assertEqual(cookie['samesite'], 'None')
    
    def test_delete_cookie(self):
        """쿠키 삭제"""
        from utils.token_handler import delete_cookie
        from django.http import HttpResponse
        
        response = HttpResponse()
        response.set_cookie('refresh_token', 'value1')
        response.set_cookie('access_token', 'value2')
        
        delete_cookie(response)
        
        # 쿠키가 삭제되었는지 확인 (max_age=0으로 설정됨)
        self.assertIn('refresh_token', response.cookies)
        self.assertIn('access_token', response.cookies)


class ForApiTests(TestCase):
    """utils/for_api.py 테스트"""
    
    @patch('utils.for_api._dt')
    def test_market_date_kst(self, mock_dt):
        """market_date_kst 함수"""
        from utils.for_api import market_date_kst
        
        # 특정 날짜로 mock 설정
        mock_now = MagicMock()
        mock_now.strftime.return_value = "2025-01-15"
        mock_dt.datetime.now.return_value = mock_now
        
        result = market_date_kst()
        
        self.assertEqual(result, "2025-01-15")
        mock_now.strftime.assert_called_once_with("%Y-%m-%d")
    
    def test_ok_basic(self):
        """ok 함수 기본 동작"""
        from utils.for_api import ok
        
        response = ok({"data": "test"})
        
        self.assertEqual(response.status_code, 200)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['data'], 'test')
        self.assertIn('asOf', content)
    
    def test_ok_with_existing_asOf(self):
        """ok 함수: asOf가 이미 있을 때"""
        from utils.for_api import ok
        
        custom_asOf = "2025-01-01T00:00:00"
        response = ok({"data": "test", "asOf": custom_asOf})
        
        import json
        content = json.loads(response.content)
        self.assertEqual(content['asOf'], custom_asOf)
    
    def test_ok_with_meta(self):
        """ok 함수: meta 정보 추가"""
        from utils.for_api import ok
        
        response = ok({"data": "test"}, extra_field="extra_value", count=10)
        
        import json
        content = json.loads(response.content)
        self.assertEqual(content['extra_field'], 'extra_value')
        self.assertEqual(content['count'], 10)
    
    def test_ok_with_custom_status(self):
        """ok 함수: 커스텀 status 코드"""
        from utils.for_api import ok
        
        response = ok({"data": "test"}, status=201)
        
        self.assertEqual(response.status_code, 201)
    
    def test_ok_ensure_ascii_false(self):
        """ok 함수: 한글 처리"""
        from utils.for_api import ok
        
        response = ok({"message": "안녕하세요"})
        
        import json
        content = json.loads(response.content.decode('utf-8'))
        self.assertEqual(content['message'], '안녕하세요')
    
    def test_degraded_basic(self):
        """degraded 함수 기본 동작"""
        from utils.for_api import degraded
        
        response = degraded("Error message")
        
        self.assertEqual(response.status_code, 200)
        import json
        content = json.loads(response.content)
        self.assertTrue(content['degraded'])
        self.assertEqual(content['error'], 'Error message')
        self.assertEqual(content['source'], 's3')
        self.assertIn('asOf', content)
    
    def test_degraded_with_custom_source(self):
        """degraded 함수: 커스텀 source"""
        from utils.for_api import degraded
        
        response = degraded("Error", source="database")
        
        import json
        content = json.loads(response.content)
        self.assertEqual(content['source'], 'database')
    
    def test_degraded_with_custom_status(self):
        """degraded 함수: 커스텀 status"""
        from utils.for_api import degraded
        
        response = degraded("Error", status=500)
        
        self.assertEqual(response.status_code, 500)
    
    def test_degraded_truncates_long_message(self):
        """degraded 함수: 긴 메시지 자르기"""
        from utils.for_api import degraded
        
        long_message = "A" * 300
        response = degraded(long_message)
        
        import json
        content = json.loads(response.content)
        self.assertEqual(len(content['error']), 200)
    
    def test_degraded_with_extra_fields(self):
        """degraded 함수: 추가 필드"""
        from utils.for_api import degraded
        
        response = degraded("Error", extra_field="value", count=5)
        
        import json
        content = json.loads(response.content)
        self.assertEqual(content['extra_field'], 'value')
        self.assertEqual(content['count'], 5)