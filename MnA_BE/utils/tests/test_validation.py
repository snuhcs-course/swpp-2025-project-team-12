# utils/tests/test_validation.py
"""
Validation 유틸리티 테스트 (utils/validation.py)
목표: 32% → 100% 커버리지
"""

from django.test import TestCase
from utils.validation import (
    raise_if_password_is_none,
    raise_if_password_too_short,
    raise_if_password_has_not_number,
    raise_if_password_has_not_uppercase,
    raise_if_password_has_not_lowercase,
    raise_if_name_is_none,
    raise_if_name_is_too_long,
    validate_password,
    validate_name
)


class PasswordValidationUtilTests(TestCase):
    """개별 비밀번호 검증 함수 테스트"""
    
    def test_raise_if_password_is_none(self):
        """None 비밀번호 검증"""
        with self.assertRaises(Exception) as context:
            raise_if_password_is_none(None)
        self.assertEqual(str(context.exception), "Password cannot be None")
    
    def test_raise_if_password_is_none_with_valid_password(self):
        """None이 아닌 비밀번호는 통과"""
        # Should not raise - just call it
        raise_if_password_is_none("ValidPass123")
    
    def test_raise_if_password_too_short(self):
        """너무 짧은 비밀번호"""
        with self.assertRaises(Exception) as context:
            raise_if_password_too_short("Short1", 8)
        self.assertIn("at least 8 characters", str(context.exception))
    
    def test_raise_if_password_too_short_with_valid_length(self):
        """충분한 길이의 비밀번호는 통과"""
        # Should not raise - just call it
        raise_if_password_too_short("LongPass123", 8)
    
    def test_raise_if_password_has_not_number(self):
        """숫자 없는 비밀번호"""
        with self.assertRaises(Exception) as context:
            raise_if_password_has_not_number("NoNumber")
        self.assertIn("at least one number", str(context.exception))
    
    def test_raise_if_password_has_not_number_with_number(self):
        """숫자 있는 비밀번호는 통과"""
        # Should not raise - just call it
        raise_if_password_has_not_number("HasNumber1")
    
    def test_raise_if_password_has_not_uppercase(self):
        """대문자 없는 비밀번호"""
        with self.assertRaises(Exception) as context:
            raise_if_password_has_not_uppercase("nouppercase123")
        self.assertIn("at least one uppercase letter", str(context.exception))
    
    def test_raise_if_password_has_not_uppercase_with_uppercase(self):
        """대문자 있는 비밀번호는 통과"""
        # Should not raise - just call it
        raise_if_password_has_not_uppercase("HasUpperCase123")
    
    def test_raise_if_password_has_not_lowercase(self):
        """소문자 없는 비밀번호"""
        with self.assertRaises(Exception) as context:
            raise_if_password_has_not_lowercase("NOLOWERCASE123")
        self.assertIn("at least one lowercase letter", str(context.exception))
    
    def test_raise_if_password_has_not_lowercase_with_lowercase(self):
        """소문자 있는 비밀번호는 통과"""
        # Should not raise - just call it
        raise_if_password_has_not_lowercase("HasLowerCase123")


class NameValidationUtilTests(TestCase):
    """개별 이름 검증 함수 테스트"""
    
    def test_raise_if_name_is_none(self):
        """None 이름 검증"""
        with self.assertRaises(Exception) as context:
            raise_if_name_is_none(None)
        self.assertEqual(str(context.exception), "User ID cannot be None")
    
    def test_raise_if_name_is_none_with_valid_name(self):
        """None이 아닌 이름은 통과"""
        # Should not raise - just call it
        raise_if_name_is_none("validname")
    
    def test_raise_if_name_is_too_long(self):
        """너무 긴 이름"""
        with self.assertRaises(Exception) as context:
            raise_if_name_is_too_long("a" * 25, 20)
        self.assertIn("cannot be longer than 20 characters", str(context.exception))
    
    def test_raise_if_name_is_too_long_with_valid_length(self):
        """적절한 길이의 이름은 통과"""
        # Should not raise - just call it
        raise_if_name_is_too_long("validname", 20)
    
    def test_raise_if_name_is_too_long_exact_length(self):
        """정확히 최대 길이인 이름은 통과"""
        # Should not raise - just call it
        raise_if_name_is_too_long("a" * 20, 20)


class ValidatePasswordTests(TestCase):
    """통합 비밀번호 검증 함수 테스트"""
    
    def test_validate_password_success(self):
        """올바른 비밀번호"""
        # Should not raise - just call it
        validate_password("ValidPass123")
    
    def test_validate_password_none(self):
        """None 비밀번호"""
        with self.assertRaises(Exception) as context:
            validate_password(None)
        self.assertIn("cannot be None", str(context.exception))
    
    def test_validate_password_too_short(self):
        """너무 짧은 비밀번호"""
        with self.assertRaises(Exception) as context:
            validate_password("Short1")
        self.assertIn("at least 8 characters", str(context.exception))
    
    def test_validate_password_no_lowercase(self):
        """소문자 없음"""
        with self.assertRaises(Exception) as context:
            validate_password("NOLOW123")
        self.assertIn("lowercase letter", str(context.exception))
    
    def test_validate_password_no_uppercase(self):
        """대문자 없음"""
        with self.assertRaises(Exception) as context:
            validate_password("noupper123")
        self.assertIn("uppercase letter", str(context.exception))
    
    def test_validate_password_no_number(self):
        """숫자 없음"""
        with self.assertRaises(Exception) as context:
            validate_password("NoNumber")
        self.assertIn("at least one number", str(context.exception))
    
    def test_validate_password_minimum_valid(self):
        """최소 조건을 만족하는 비밀번호"""
        try:
            validate_password("Abcdef12")  # 8자, 대문자, 소문자, 숫자
        except Exception:
            self.fail("Should not raise exception for minimum valid password")
    
    def test_validate_password_with_special_chars(self):
        """특수문자 포함 (허용됨)"""
        # Should not raise - just call it
        validate_password("Valid!Pass@123")


class ValidateNameTests(TestCase):
    """통합 이름 검증 함수 테스트"""
    
    def test_validate_name_success(self):
        """올바른 이름"""
        # Should not raise - just call it
        validate_name("validusername")
    
    def test_validate_name_none(self):
        """None 이름"""
        with self.assertRaises(Exception) as context:
            validate_name(None)
        self.assertIn("cannot be None", str(context.exception))
    
    def test_validate_name_too_long(self):
        """너무 긴 이름 (20자 초과)"""
        with self.assertRaises(Exception) as context:
            validate_name("a" * 21)
        self.assertIn("cannot be longer than 20 characters", str(context.exception))
    
    def test_validate_name_maximum_length(self):
        """최대 길이 (20자) 이름"""
        # Should not raise - just call it
        validate_name("a" * 20)
    
    def test_validate_name_single_char(self):
        """1자리 이름"""
        # Should not raise - just call it
        validate_name("a")
    
    def test_validate_name_with_numbers(self):
        """숫자 포함 이름"""
        # Should not raise - just call it
        validate_name("user123")
    
    def test_validate_name_with_special_chars(self):
        """특수문자 포함 이름 (현재는 허용됨)"""
        # Should not raise - just call it
        validate_name("user_name!")