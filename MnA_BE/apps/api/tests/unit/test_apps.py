# apps/api/tests/unit/test_apps.py
"""
apps.py 100% 커버리지 달성
Missing lines: 22-23 (reloader skip), 28-31 (exception handling)
"""
import os
from django.test import SimpleTestCase
from django.apps import apps
from unittest.mock import patch


class ApiConfigReadyTests(SimpleTestCase):
    """ApiConfig.ready() 메서드 완전 커버리지"""

    @patch("apps.api.apps.instant_data.init")
    @patch("apps.api.apps.debug_print")
    def test_ready_skips_when_run_main_not_true(self, mock_debug_print, mock_init):
        """RUN_MAIN이 'true'가 아닐 때 스킵 (lines 22-23)"""
        old = os.environ.get("RUN_MAIN")

        try:
            # 분기 조건: != 'true' and is not None
            os.environ["RUN_MAIN"] = "not-true"

            # 이미 등록된 app config 가져오기
            config = apps.get_app_config("api")
            config.ready()

            # init이 호출되지 않아야 함
            mock_init.assert_not_called()

            # 스킵 메시지가 출력되어야 함
            calls = [str(call) for call in mock_debug_print.call_args_list]
            skip_called = any("Skipping data load" in call for call in calls)
            self.assertTrue(skip_called, "스킵 메시지가 출력되지 않았습니다")

        finally:
            # 환경변수 복구
            if old is None:
                os.environ.pop("RUN_MAIN", None)
            else:
                os.environ["RUN_MAIN"] = old

    @patch("apps.api.apps.debug_print")
    @patch("apps.api.apps.instant_data.init", side_effect=Exception("Test error"))
    def test_ready_logs_error_when_init_fails(self, mock_init, mock_debug_print):
        """init() 실패 시 예외 처리 (lines 28-31)"""
        old = os.environ.get("RUN_MAIN")

        try:
            # RUN_MAIN을 'true'로 설정하여 if 통과
            os.environ["RUN_MAIN"] = "true"

            config = apps.get_app_config("api")
            config.ready()

            # init이 호출되었어야 함
            mock_init.assert_called()

            # 에러 로그 확인
            calls = [str(call) for call in mock_debug_print.call_args_list]
            error_logged = any("✗ Error loading data:" in call for call in calls)
            self.assertTrue(error_logged, "에러 로그가 출력되지 않았습니다")

            # traceback 로그 확인
            tb_logged = any("Traceback" in call for call in calls)
            self.assertTrue(tb_logged, "traceback 로그가 출력되지 않았습니다")

        finally:
            # 환경변수 복구
            if old is None:
                os.environ.pop("RUN_MAIN", None)
            else:
                os.environ["RUN_MAIN"] = old
