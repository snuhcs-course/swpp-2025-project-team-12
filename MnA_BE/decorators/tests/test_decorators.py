# decorators/tests/test_decorators.py
"""
decorators 모듈 종합 테스트
목표: 100% 커버리지
"""

from django.test import TestCase, RequestFactory
from django.http import JsonResponse
from unittest.mock import patch, MagicMock
import jwt
from datetime import datetime, timedelta


class DefaultErrorHandlerTests(TestCase):
    """decorators/default_error_handler.py 테스트"""
    
    def test_default_error_handler_success(self):
        """정상 실행 시 원래 함수 결과 반환"""
        from decorators.default_error_handler import default_error_handler
        
        @default_error_handler
        def test_function(value):
            return value * 2
        
        result = test_function(5)
        self.assertEqual(result, 10)
    
    def test_default_error_handler_catches_exception(self):
        """예외 발생 시 500 에러 반환"""
        from decorators.default_error_handler import default_error_handler
        
        @default_error_handler
        def failing_function():
            raise ValueError("Test error")
        
        response = failing_function()
        
        self.assertIsInstance(response, JsonResponse)
        self.assertEqual(response.status_code, 500)
        
        import json
        content = json.loads(response.content)
        self.assertEqual(content['message'], 'INTERNAL ERROR')
    
    @patch('decorators.default_error_handler.debug_print')
    def test_default_error_handler_calls_debug_print(self, mock_debug_print):
        """예외 발생 시 debug_print 호출"""
        from decorators.default_error_handler import default_error_handler
        
        @default_error_handler
        def failing_function():
            raise RuntimeError("Test error")
        
        response = failing_function()
        
        # debug_print가 호출되었는지 확인
        self.assertTrue(mock_debug_print.called)
        call_arg = mock_debug_print.call_args[0][0]
        self.assertIn('RuntimeError', call_arg)
        self.assertIn('Test error', call_arg)
    
    def test_default_error_handler_with_args_kwargs(self):
        """인자와 키워드 인자를 받는 함수"""
        from decorators.default_error_handler import default_error_handler
        
        @default_error_handler
        def test_function(a, b, c=10):
            return a + b + c
        
        result = test_function(1, 2, c=3)
        self.assertEqual(result, 6)
    
    def test_default_error_handler_returns_json_response(self):
        """JsonResponse를 반환하는 함수"""
        from decorators.default_error_handler import default_error_handler
        
        @default_error_handler
        def test_function():
            return JsonResponse({"status": "ok"})
        
        response = test_function()
        self.assertIsInstance(response, JsonResponse)


class RequireAuthTests(TestCase):
    """decorators/require_auth.py 테스트"""
    
    def setUp(self):
        self.factory = RequestFactory()
        
        # 환경변수 설정
        self.env_patcher = patch.dict('os.environ', {
            'SECRET_KEY': 'test_secret_key',
            'HASH_ALGORITHM': 'HS256',
            'ACCESS_TOKEN_EXPIRE_MINUTES': '30',
            'REFRESH_TOKEN_EXPIRE_DAYS': '7'
        })
        self.env_patcher.start()
    
    def tearDown(self):
        self.env_patcher.stop()
    
    def test_require_auth_no_access_token(self):
        """access_token이 없을 때"""
        from decorators.require_auth import require_auth
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        request = self.factory.get('/')
        request.COOKIES = {}
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 401)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['message'], 'Unauthorized')
    
    @patch('decorators.require_auth.User')
    def test_require_auth_valid_access_token(self, mock_user_model):
        """유효한 access_token"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_access_token
        
        # Mock User
        mock_user = MagicMock()
        mock_user.id = 123
        mock_user_model.objects.get.return_value = mock_user
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"user_id": kwargs.get("user").id})
        
        access_token = make_access_token(123)
        request = self.factory.get('/')
        request.COOKIES = {'access_token': access_token}
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 200)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['user_id'], 123)
    
    @patch('decorators.require_auth.User')
    def test_require_auth_expired_access_token_no_refresh(self, mock_user_model):
        """access_token 만료, refresh_token 없음"""
        from decorators.require_auth import require_auth
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        # 만료된 토큰 생성
        expired_token = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(hours=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        
        request = self.factory.get('/')
        request.COOKIES = {'access_token': expired_token}
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 500)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['message'], 'UNEXPECTED ERROR')
    
    @patch('decorators.require_auth.User')
    def test_require_auth_expired_access_token_with_valid_refresh(self, mock_user_model):
        """access_token 만료, 유효한 refresh_token으로 갱신"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_refresh_token
        
        # Mock User
        mock_user = MagicMock()
        mock_user.id = 123
        refresh_token = make_refresh_token(123)
        mock_user.refresh_token = refresh_token
        mock_user_model.objects.get.return_value = mock_user
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"user_id": kwargs.get("user").id})
        
        # 만료된 access_token
        expired_access = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(hours=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        
        request = self.factory.get('/')
        request.COOKIES = {
            'access_token': expired_access,
            'refresh_token': refresh_token
        }
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 200)
        # 새로운 토큰이 쿠키에 설정되었는지 확인
        self.assertIn('refresh_token', response.cookies)
        self.assertIn('access_token', response.cookies)
    
    @patch('decorators.require_auth.User')
    def test_require_auth_expired_refresh_token(self, mock_user_model):
        """access_token과 refresh_token 모두 만료"""
        from decorators.require_auth import require_auth
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        # 만료된 토큰들
        expired_access = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(hours=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        expired_refresh = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(days=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        
        request = self.factory.get('/')
        request.COOKIES = {
            'access_token': expired_access,
            'refresh_token': expired_refresh
        }
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 401)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['message'], 'TOKEN EXPIRED')
    
    @patch('decorators.require_auth.User')
    def test_require_auth_user_not_found(self, mock_user_model):
        """유효한 토큰이지만 사용자가 존재하지 않음"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_access_token
        
        # User.objects.get이 DoesNotExist 예외 발생
        mock_user_model.objects.get.side_effect = Exception("User not found")
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        access_token = make_access_token(999)
        request = self.factory.get('/')
        request.COOKIES = {'access_token': access_token}
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 500)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['message'], 'UNEXPECTED ERROR (USER NOT FOUND)')
    
    @patch('decorators.require_auth.User')
    def test_require_auth_duplicated_refresh_token(self, mock_user_model):
        """중복된 refresh_token 감지"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_refresh_token
        
        # Mock User
        mock_user = MagicMock()
        mock_user.id = 123
        mock_user.refresh_token = "different_token"  # DB의 토큰과 다름
        mock_user_model.objects.get.return_value = mock_user
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        # 만료된 access_token
        expired_access = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(hours=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        
        # 쿠키의 refresh_token
        cookie_refresh = make_refresh_token(123)
        
        request = self.factory.get('/')
        request.COOKIES = {
            'access_token': expired_access,
            'refresh_token': cookie_refresh
        }
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 401)
        import json
        content = json.loads(response.content)
        self.assertIn('invalid refresh token', content['message'])
        
        # DB의 refresh_token이 빈 문자열로 초기화되었는지 확인
        self.assertEqual(mock_user.refresh_token, "")
        self.assertTrue(mock_user.save.called)
    
    @patch('decorators.require_auth.User')
    def test_require_auth_duplicated_token_save_fails(self, mock_user_model):
        """중복 토큰 감지 후 저장 실패"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_refresh_token
        
        # Mock User
        mock_user = MagicMock()
        mock_user.id = 123
        mock_user.refresh_token = "different_token"
        mock_user.save.side_effect = Exception("Save failed")
        mock_user_model.objects.get.return_value = mock_user
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        expired_access = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(hours=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        cookie_refresh = make_refresh_token(123)
        
        request = self.factory.get('/')
        request.COOKIES = {
            'access_token': expired_access,
            'refresh_token': cookie_refresh
        }
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 500)
        import json
        content = json.loads(response.content)
        self.assertEqual(content['message'], 'UNEXPECTED ERROR')
    
    @patch('decorators.require_auth.User')
    @patch('decorators.require_auth.rotate_refresh_token')
    def test_require_auth_rtr_fails(self, mock_rotate, mock_user_model):
        """RTR(Refresh Token Rotation) 실패"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_refresh_token
        
        # Mock User
        mock_user = MagicMock()
        mock_user.id = 123
        refresh_token = make_refresh_token(123)
        mock_user.refresh_token = refresh_token
        mock_user_model.objects.get.return_value = mock_user
        
        # rotate_refresh_token이 예외 발생
        mock_rotate.side_effect = Exception("Rotation failed")
        
        @require_auth
        def test_view(self, request, **kwargs):
            return JsonResponse({"status": "ok"})
        
        expired_access = jwt.encode(
            {'id': 123, 'exp': datetime.utcnow() - timedelta(hours=1)},
            'test_secret_key',
            algorithm='HS256'
        )
        
        request = self.factory.get('/')
        request.COOKIES = {
            'access_token': expired_access,
            'refresh_token': refresh_token
        }
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertEqual(response.status_code, 500)
        import json
        content = json.loads(response.content)
        self.assertIn('RTR', content['message'])
    
    @patch('decorators.require_auth.User')
    def test_require_auth_injects_user_kwarg(self, mock_user_model):
        """user 인자가 함수에 주입되는지 확인"""
        from decorators.require_auth import require_auth
        from utils.token_handler import make_access_token
        
        mock_user = MagicMock()
        mock_user.id = 456
        mock_user.name = "Test User"
        mock_user_model.objects.get.return_value = mock_user
        
        received_user = None
        
        @require_auth
        def test_view(self, request, **kwargs):
            nonlocal received_user
            received_user = kwargs.get('user')
            return JsonResponse({"status": "ok"})
        
        access_token = make_access_token(456)
        request = self.factory.get('/')
        request.COOKIES = {'access_token': access_token}
        
        # ViewSet pattern: pass None as self

        
        response = test_view(None, request)
        
        self.assertIsNotNone(received_user)
        self.assertEqual(received_user.id, 456)
        self.assertEqual(received_user.name, "Test User")