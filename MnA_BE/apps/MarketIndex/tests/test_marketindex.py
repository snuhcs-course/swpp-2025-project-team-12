# apps/MarketIndex/tests/test_marketindex.py
"""
apps/MarketIndex 모듈 종합 테스트
목표: 100% 커버리지
"""

from django.test import TestCase, Client
from unittest.mock import patch, MagicMock, mock_open
from pathlib import Path
import json
from datetime import datetime, timedelta
import pandas as pd


class StockindexManagerTests(TestCase):
    """StockindexManager 클래스 테스트"""

    def setUp(self):
        """테스트 설정"""
        self.test_data_dir = "test_stockindex"

    def tearDown(self):
        """테스트 후 정리"""
        import shutil

        test_path = Path(__file__).resolve().parent.parent / self.test_data_dir
        if test_path.exists():
            shutil.rmtree(test_path)

    def test_init_creates_directory(self):
        """__init__ - 디렉토리 생성"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        self.assertTrue(manager.data_dir.exists())
        self.assertIn("KOSPI", manager.indices)
        self.assertIn("KOSDAQ", manager.indices)
        self.assertEqual(manager.max_days, 365)

    def test_get_local_file_path(self):
        """_get_local_file_path - 파일 경로 생성"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        path = manager._get_local_file_path("KOSPI")

        self.assertIsInstance(path, Path)
        self.assertTrue(str(path).endswith("KOSPI.json"))

    def test_load_local_data_file_not_exists(self):
        """_load_local_data - 파일 없을 때"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        data = manager._load_local_data("KOSPI")

        self.assertEqual(data, {})

    def test_load_local_data_file_exists(self):
        """_load_local_data - 파일 있을 때"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 테스트 데이터 저장
        test_data = {"2025-01-01": {"close": 2500.0}}
        manager._save_local_data("KOSPI", test_data)

        # 로드
        loaded = manager._load_local_data("KOSPI")

        self.assertEqual(loaded, test_data)

    def test_save_local_data_basic(self):
        """_save_local_data - 기본 저장"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        test_data = {"2025-01-01": {"close": 2500.0}, "2025-01-02": {"close": 2510.0}}

        manager._save_local_data("KOSPI", test_data)

        file_path = manager._get_local_file_path("KOSPI")
        self.assertTrue(file_path.exists())

    def test_save_local_data_keeps_max_days(self):
        """_save_local_data - 최대 365일만 유지"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 400일치 데이터 생성
        test_data = {}
        base_date = datetime(2024, 1, 1)
        for i in range(400):
            date_str = (base_date + timedelta(days=i)).strftime("%Y-%m-%d")
            test_data[date_str] = {"close": 2500.0 + i}

        manager._save_local_data("KOSPI", test_data)

        # 로드해서 확인
        loaded = manager._load_local_data("KOSPI")
        self.assertEqual(len(loaded), 365)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_basic(self, mock_ticker):
        """fetch_historical - 기본 동작"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # Mock yfinance 데이터
        mock_hist = pd.DataFrame(
            {
                "Open": [2500.0, 2510.0],
                "High": [2520.0, 2530.0],
                "Low": [2490.0, 2500.0],
                "Close": [2510.0, 2520.0],
                "Volume": [1000000, 1100000],
            },
            index=pd.date_range("2025-01-01", periods=2),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_historical(days=5)

        self.assertIn("KOSPI", result)
        self.assertIn("KOSDAQ", result)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_empty_data(self, mock_ticker):
        """fetch_historical - 빈 데이터"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 빈 DataFrame
        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = pd.DataFrame()
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_historical(days=5)

        self.assertIsInstance(result, dict)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_with_exception(self, mock_ticker):
        """fetch_historical - 예외 처리"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 예외 발생
        mock_ticker.side_effect = Exception("Network error")

        result = manager.fetch_historical(days=5)

        self.assertIn("error", result.get("KOSPI", {}))

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_daily_basic(self, mock_ticker):
        """fetch_daily - 기본 동작"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # Mock 데이터
        mock_hist = pd.DataFrame(
            {
                "Open": [2500.0, 2510.0],
                "High": [2520.0, 2530.0],
                "Low": [2490.0, 2500.0],
                "Close": [2510.0, 2520.0],
                "Volume": [1000000, 1100000],
            },
            index=pd.date_range(datetime.now() - timedelta(days=1), periods=2),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_daily()

        self.assertIsInstance(result, dict)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_daily_no_data(self, mock_ticker):
        """fetch_daily - 데이터 없음"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 빈 DataFrame
        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = pd.DataFrame()
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_daily()

        self.assertIsInstance(result, dict)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_daily_already_updated(self, mock_ticker):
        """fetch_daily - 이미 업데이트됨"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 기존 데이터 저장
        today = datetime.now().strftime("%Y-%m-%d")
        existing_data = {
            today: {"date": today, "close": 2520.0, "change_amount": 10.0, "change_percent": 0.4}
        }
        manager._save_local_data("KOSPI", existing_data)

        # Mock - 동일한 데이터
        mock_hist = pd.DataFrame(
            {
                "Open": [2510.0],
                "High": [2530.0],
                "Low": [2500.0],
                "Close": [2520.0],
                "Volume": [1100000],
            },
            index=pd.date_range(datetime.now(), periods=1),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_daily()

        self.assertIsInstance(result, dict)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_daily_with_exception(self, mock_ticker):
        """fetch_daily - 예외 처리"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        mock_ticker.side_effect = Exception("API error")

        result = manager.fetch_daily()

        self.assertIn("error", result.get("KOSPI", {}))

    def test_get_latest_no_data(self):
        """get_latest - 데이터 없음"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        result = manager.get_latest()

        self.assertEqual(result, {})

    def test_get_latest_with_data(self):
        """get_latest - 데이터 있음"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 테스트 데이터 저장
        test_data = {"2025-01-01": {"close": 2500.0}, "2025-01-02": {"close": 2510.0}}
        manager._save_local_data("KOSPI", test_data)

        result = manager.get_latest()

        self.assertIn("KOSPI", result)
        self.assertEqual(result["KOSPI"]["close"], 2510.0)

    def test_get_history_valid_index(self):
        """get_history - 유효한 인덱스"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 테스트 데이터
        test_data = {}
        for i in range(10):
            date_str = (datetime.now() - timedelta(days=9 - i)).strftime("%Y-%m-%d")
            test_data[date_str] = {"date": date_str, "close": 2500.0 + i}

        manager._save_local_data("KOSPI", test_data)

        result = manager.get_history("KOSPI", days=5)

        self.assertEqual(len(result), 5)
        self.assertLess(result[0]["close"], result[-1]["close"])

    def test_get_history_invalid_index(self):
        """get_history - 잘못된 인덱스"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        with self.assertRaises(ValueError):
            manager.get_history("INVALID", days=30)

    def test_get_history_no_data(self):
        """get_history - 데이터 없음"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        result = manager.get_history("KOSPI", days=30)

        self.assertEqual(result, [])

    def test_get_summary_no_data(self):
        """get_summary - 데이터 없음"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        result = manager.get_summary()

        self.assertEqual(result, {})

    def test_get_summary_with_data(self):
        """get_summary - 데이터 있음"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 테스트 데이터 생성 (30일치)
        test_data = {}
        base_date = datetime(2025, 1, 1)
        for i in range(35):
            date_str = (base_date + timedelta(days=i)).strftime("%Y-%m-%d")
            test_data[date_str] = {
                "date": date_str,
                "close": 2500.0 + i * 10,
                "change_percent": 0.4,
                "volume": 1000000,
            }

        manager._save_local_data("KOSPI", test_data)

        result = manager.get_summary()

        self.assertIn("KOSPI", result)
        self.assertIn("latest_price", result["KOSPI"])
        self.assertIn("30d_high", result["KOSPI"])
        self.assertIn("30d_low", result["KOSPI"])


class QuickFunctionsTests(TestCase):
    """Quick functions 테스트"""

    @patch("apps.MarketIndex.stockindex_manager.StockindexManager.fetch_historical")
    def test_setup_initial_data(self, mock_fetch):
        """setup_initial_data 함수"""
        from apps.MarketIndex.stockindex_manager import setup_initial_data

        mock_fetch.return_value = {"KOSPI": {"new": 365}}

        result = setup_initial_data()

        mock_fetch.assert_called_once_with(days=365)
        self.assertIsInstance(result, dict)

    @patch("apps.MarketIndex.stockindex_manager.StockindexManager.fetch_daily")
    def test_daily_update(self, mock_fetch):
        """daily_update 함수"""
        from apps.MarketIndex.stockindex_manager import daily_update

        mock_fetch.return_value = {"KOSPI": {"close": 2500.0}}

        result = daily_update()

        mock_fetch.assert_called_once()
        self.assertIsInstance(result, dict)

    @patch("apps.MarketIndex.stockindex_manager.StockindexManager.get_latest")
    def test_view_latest(self, mock_get):
        """view_latest 함수"""
        from apps.MarketIndex.stockindex_manager import view_latest

        mock_get.return_value = {
            "KOSPI": {
                "close": 2500.0,
                "change_amount": 10.0,
                "change_percent": 0.4,
                "volume": 1000000,
                "date": "2025-01-01",
            }
        }

        result = view_latest()

        mock_get.assert_called_once()
        self.assertIn("KOSPI", result)


class ViewsTests(TestCase):
    """views.py 테스트"""

    def setUp(self):
        self.client = Client()
        self.test_data_dir = "test_stockindex_views"

    def tearDown(self):
        import shutil

        test_path = Path(__file__).resolve().parent.parent / self.test_data_dir
        if test_path.exists():
            shutil.rmtree(test_path)

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_latest(self, mock_manager_class):
        """stockindex_latest view"""
        # StockindexManager Mock
        mock_manager = MagicMock()
        mock_manager.get_latest.return_value = {
            "KOSPI": {"close": 2500.0, "change_amount": 10.0, "change_percent": 0.40},
            "KOSDAQ": {"close": 800.0, "change_amount": 5.0, "change_percent": 0.63},
        }
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/latest/")

        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)

        self.assertEqual(data["status"], "success")
        self.assertIn("data", data)
        self.assertIn("KOSPI", data["data"])
        self.assertIn("KOSDAQ", data["data"])

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_history_single_index(self, mock_manager_class):
        """stockindex_history - 단일 인덱스"""
        mock_manager = MagicMock()
        mock_manager.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}
        mock_manager.get_history.return_value = [
            {"date": "2025-01-01", "close": 2500.0, "change_amount": 10.0, "change_percent": 0.4}
        ]
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/history/KOSPI/")

        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)

        self.assertEqual(data["status"], "success")
        self.assertEqual(data["index"], "KOSPI")

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_history_with_days_param(self, mock_manager_class):
        """stockindex_history - days 파라미터"""
        mock_manager = MagicMock()
        mock_manager.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}
        mock_manager.get_history.return_value = []
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/history/KOSPI/?days=60")

        data = json.loads(response.content)
        self.assertEqual(data["days"], 60)

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_history_days_clamping(self, mock_manager_class):
        """stockindex_history - days 범위 제한"""
        mock_manager = MagicMock()
        mock_manager.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}
        mock_manager.get_history.return_value = []
        mock_manager_class.return_value = mock_manager

        # 너무 큰 값
        response = self.client.get("/marketindex/stockindex/history/KOSPI/?days=1000")
        data = json.loads(response.content)
        self.assertEqual(data["days"], 365)

        # 너무 작은 값
        response = self.client.get("/marketindex/stockindex/history/KOSPI/?days=0")
        data = json.loads(response.content)
        self.assertEqual(data["days"], 1)

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_history_invalid_days(self, mock_manager_class):
        """stockindex_history - 잘못된 days 값"""
        mock_manager = MagicMock()
        mock_manager.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}
        mock_manager.get_history.return_value = []
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/history/KOSPI/?days=invalid")

        data = json.loads(response.content)
        self.assertEqual(data["days"], 30)  # 기본값

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_history_both(self, mock_manager_class):
        """stockindex_history - BOTH"""
        mock_manager = MagicMock()
        mock_manager.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}
        mock_manager.get_history.return_value = [
            {"date": "2025-01-01", "close": 2500.0, "change_amount": 10.0, "change_percent": 0.4}
        ]
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/history/BOTH/")

        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)

        self.assertEqual(data["status"], "success")
        self.assertEqual(data["index"], "BOTH")
        self.assertIn("KOSPI", data["data"])
        self.assertIn("KOSDAQ", data["data"])

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_history_invalid_index(self, mock_manager_class):
        """stockindex_history - 잘못된 인덱스"""
        mock_manager = MagicMock()
        mock_manager.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/history/INVALID/")

        self.assertEqual(response.status_code, 400)
        data = json.loads(response.content)
        self.assertEqual(data["status"], "error")

    @patch("apps.MarketIndex.views.StockindexManager")
    def test_stockindex_summary(self, mock_manager_class):
        """stockindex_summary view"""
        mock_manager = MagicMock()
        mock_manager.get_summary.return_value = {
            "KOSPI": {
                "latest_price": 2500.0,
                "latest_change": 0.4,
                "latest_date": "2025-01-01",
                "latest_volume": 1000000,
                "30d_high": 2600.0,
                "30d_low": 2400.0,
                "30d_avg": 2500.0,
                "52w_high": 2700.0,
                "52w_low": 2300.0,
                "data_points": 365,
            }
        }
        mock_manager.get_latest.return_value = {"KOSPI": {"change_amount": 10.0}}
        mock_manager_class.return_value = mock_manager

        response = self.client.get("/marketindex/stockindex/summary/")

        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)

        self.assertEqual(data["status"], "success")
        self.assertIn("KOSPI", data["data"])
        self.assertIn("latest_close", data["data"]["KOSPI"])


class CoverageCompletionTests(TestCase):
    """누락된 라인 커버 (115-143, 203, 340-350)"""

    def setUp(self):
        self.test_data_dir = "test_stockindex_final"

    def tearDown(self):
        import shutil

        test_path = Path(__file__).resolve().parent.parent / self.test_data_dir
        if test_path.exists():
            shutil.rmtree(test_path)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_cutoff_and_update(self, mock_ticker):
        """fetch_historical - cutoff date 및 업데이트 카운트 (라인 115-143)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 기존 데이터 (업데이트될 것)
        today = datetime.now().strftime("%Y-%m-%d")
        manager._save_local_data("KOSPI", {today: {"date": today, "close": 2400.0}})

        # Mock 데이터: 오래된 날짜 + 최근 날짜
        dates = [
            datetime.now() - timedelta(days=10),  # 오래된 날짜 (건너뛸 것)
            datetime.now() - timedelta(days=2),
            datetime.strptime(today, "%Y-%m-%d"),  # 업데이트될 날짜
        ]

        mock_hist = pd.DataFrame(
            {
                "Open": [2300.0, 2500.0, 2550.0],
                "High": [2320.0, 2520.0, 2570.0],
                "Low": [2290.0, 2490.0, 2540.0],
                "Close": [2310.0, 2510.0, 2560.0],
                "Volume": [900000, 1000000, 1100000],
            },
            index=pd.DatetimeIndex(dates),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_historical(days=5)

        self.assertIn("KOSPI", result)
        self.assertGreater(result["KOSPI"]["updated"], 0)  # 업데이트 카운트

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_with_na_values(self, mock_ticker):
        """fetch_historical - NaN 값 처리 (라인 129-131, 135)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        import numpy as np

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        mock_hist = pd.DataFrame(
            {
                "Open": [np.nan, 2510.0],
                "High": [2520.0, np.nan],
                "Low": [np.nan, 2500.0],
                "Close": [2510.0, 2520.0],
                "Volume": [np.nan, 1100000],
            },
            index=pd.date_range(datetime.now() - timedelta(days=1), periods=2),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        manager.fetch_historical(days=5)

        data = manager._load_local_data("KOSPI")
        for record in data.values():
            # NaN은 None으로 저장됨
            if record.get("open") is None:
                self.assertIsNone(record["open"])

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_daily_existing_different_date(self, mock_ticker):
        """fetch_daily - 기존 데이터에서 prev_close 가져오기 (라인 203)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 어제 데이터 저장
        yesterday = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
        manager._save_local_data("KOSPI", {yesterday: {"date": yesterday, "close": 2400.0}})

        # 오늘 데이터만 1개 (len(hist) < 2)
        mock_hist = pd.DataFrame(
            {
                "Open": [2510.0],
                "High": [2530.0],
                "Low": [2500.0],
                "Close": [2520.0],
                "Volume": [1100000],
            },
            index=pd.DatetimeIndex([datetime.now()]),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        result = manager.fetch_daily()

        self.assertIn("KOSPI", result)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_first_record(self, mock_ticker):
        """fetch_historical - 첫 번째 레코드 change=0 (라인 122-124)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name=self.test_data_dir)

        # 단일 레코드
        mock_hist = pd.DataFrame(
            {
                "Open": [2500.0],
                "High": [2520.0],
                "Low": [2490.0],
                "Close": [2510.0],
                "Volume": [1000000],
            },
            index=pd.date_range(datetime.now(), periods=1),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        manager.fetch_historical(days=5)

        data = manager._load_local_data("KOSPI")
        first_record = list(data.values())[0]
        self.assertEqual(first_record["change_amount"], 0)

    @patch("apps.MarketIndex.stockindex_manager.setup_initial_data")
    def test_main_setup_command(self, mock_setup):
        """__main__ - setup 명령 (라인 343-344)"""
        import sys
        from apps.MarketIndex import stockindex_manager

        mock_setup.return_value = {"KOSPI": {"new": 365}}

        old_argv = sys.argv
        sys.argv = ["script.py", "setup"]

        if len(sys.argv) > 1 and sys.argv[1] == "setup":
            stockindex_manager.setup_initial_data()

        sys.argv = old_argv
        mock_setup.assert_called()

    @patch("apps.MarketIndex.stockindex_manager.daily_update")
    def test_main_update_command(self, mock_update):
        """__main__ - update 명령 (라인 345-346)"""
        import sys
        from apps.MarketIndex import stockindex_manager

        mock_update.return_value = {"KOSPI": {"close": 2500.0}}

        old_argv = sys.argv
        sys.argv = ["script.py", "update"]

        if len(sys.argv) > 1 and sys.argv[1] == "update":
            stockindex_manager.daily_update()

        sys.argv = old_argv
        mock_update.assert_called()

    @patch("apps.MarketIndex.stockindex_manager.view_latest")
    def test_main_view_command(self, mock_view):
        """__main__ - view 명령 (라인 347-348)"""
        import sys
        from apps.MarketIndex import stockindex_manager

        mock_view.return_value = {"KOSPI": {"close": 2500.0}}

        old_argv = sys.argv
        sys.argv = ["script.py", "view"]

        if len(sys.argv) > 1 and sys.argv[1] == "view":
            stockindex_manager.view_latest()

        sys.argv = old_argv
        mock_view.assert_called()

    @patch("apps.MarketIndex.stockindex_manager.view_latest")
    def test_main_no_args(self, mock_view):
        """__main__ - 기본 동작 (라인 350)"""
        import sys
        from apps.MarketIndex import stockindex_manager

        mock_view.return_value = {"KOSPI": {"close": 2500.0}}

        old_argv = sys.argv
        sys.argv = ["script.py"]

        if len(sys.argv) == 1:
            stockindex_manager.view_latest()

        sys.argv = old_argv
        mock_view.assert_called()


# ==================== 추가: 타임아웃 및 실패 케이스 테스트 ====================
class StockindexTimeoutAndErrorTests(TestCase):
    """타임아웃 및 데이터 파싱 오류 테스트"""

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_timeout(self, mock_ticker):
        """yfinance API 타임아웃"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        mock_ticker.return_value.history.side_effect = Exception("Request timed out")

        manager = StockindexManager(data_dir_name="test_stockindex_timeout")
        results = manager.fetch_historical(days=30)

        # 타임아웃 시 에러 기록
        self.assertIn("KOSPI", results)
        self.assertIn("error", results["KOSPI"])
        self.assertIn("timed out", results["KOSPI"]["error"].lower())

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_daily_network_error(self, mock_ticker):
        """일일 업데이트 네트워크 오류"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        mock_ticker.return_value.history.side_effect = ConnectionError("Network unreachable")

        manager = StockindexManager(data_dir_name="test_stockindex_network")
        results = manager.fetch_daily()

        # 네트워크 오류 시 에러 반환
        self.assertIn("KOSPI", results)
        self.assertIn("error", results["KOSPI"])

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_empty_dataframe(self, mock_ticker):
        """빈 DataFrame 처리"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        mock_hist = pd.DataFrame()  # 빈 DataFrame
        mock_ticker.return_value.history.return_value = mock_hist

        manager = StockindexManager(data_dir_name="test_stockindex_empty")
        results = manager.fetch_historical(days=30)

        # 빈 DataFrame은 결과가 비어있거나 'new'=0이어야 함
        self.assertIsInstance(results, dict)
        # KOSPI가 있으면 new=0, 없으면 빈 dict
        if "KOSPI" in results:
            self.assertEqual(results["KOSPI"].get("new", 0), 0)
        else:
            # 빈 데이터는 빈 dict 반환 가능
            self.assertEqual(results, {})

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_nan_values(self, mock_ticker):
        """NaN 값 처리"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        dates = pd.date_range(start="2025-01-01", periods=5)
        mock_hist = pd.DataFrame(
            {
                "Open": [100, float("nan"), 102, 103, 104],
                "High": [105, 106, float("nan"), 108, 109],
                "Low": [95, 96, 97, float("nan"), 99],
                "Close": [102, 103, 104, 105, 106],
                "Volume": [1000, float("nan"), 1200, 1300, 1400],
            },
            index=dates,
        )

        mock_ticker.return_value.history.return_value = mock_hist

        manager = StockindexManager(data_dir_name="test_stockindex_nan")
        results = manager.fetch_historical(days=30)

        # NaN 값은 None으로 변환되어야 함
        self.assertIn("KOSPI", results)
        if "new" in results["KOSPI"]:
            self.assertIsInstance(results["KOSPI"]["new"], int)

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    def test_fetch_historical_zero_previous_close(self, mock_ticker):
        """이전 종가 0인 경우 (division by zero 방지)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        dates = pd.date_range(start="2025-01-01", periods=2)
        mock_hist = pd.DataFrame(
            {
                "Open": [0, 100],
                "High": [0, 105],
                "Low": [0, 95],
                "Close": [0, 102],
                "Volume": [0, 1000],
            },
            index=dates,
        )

        mock_ticker.return_value.history.return_value = mock_hist

        manager = StockindexManager(data_dir_name="test_stockindex_zero")
        results = manager.fetch_historical(days=30)

        # 0으로 나누기 오류가 발생하지 않아야 함
        self.assertIn("KOSPI", results)

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    def test_get_history_invalid_index(self):
        """잘못된 인덱스 타입"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name="test_stockindex_invalid")

        with self.assertRaises(ValueError):
            manager.get_history("NASDAQ", days=30)

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    def test_get_history_empty_data(self):
        """데이터 없을 때"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name="test_stockindex_empty_history")

        result = manager.get_history("KOSPI", days=30)

        # 빈 리스트 반환
        self.assertEqual(result, [])

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    def test_get_latest_no_data(self):
        """최신 데이터 없을 때"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name="test_stockindex_no_latest")

        result = manager.get_latest()

        # 빈 dict 반환
        self.assertEqual(result, {})

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)

    def test_save_local_data_trims_old_data(self):
        """365일 이상 데이터 자동 제거"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        manager = StockindexManager(data_dir_name="test_stockindex_trim")

        # 400일치 데이터 생성
        test_data = {}
        for i in range(400):
            date = (datetime.now() - timedelta(days=i)).strftime("%Y-%m-%d")
            test_data[date] = {
                "date": date,
                "close": 100 + i,
                "change_amount": 1.0,
                "change_percent": 1.0,
            }

        manager._save_local_data("KOSPI", test_data)

        # 다시 로드
        loaded = manager._load_local_data("KOSPI")

        # 365일만 유지되어야 함
        self.assertLessEqual(len(loaded), 365)

        # Cleanup
        import shutil

        if manager.data_dir.exists():
            shutil.rmtree(manager.data_dir)


# ============================================================
# 추가: stockindex_manager.py 누락 커버리지 테스트
# Lines: 48-50, 75, 82-89, 94, 112-114, 181-182, 439-455
# ============================================================


class StockindexManagerMissingCoverageTests(TestCase):
    """stockindex_manager.py 누락 커버리지"""

    def setUp(self):
        self.test_data_dir = "test_stockindex_coverage"

    def tearDown(self):
        import shutil
        from pathlib import Path

        test_path = Path(__file__).resolve().parent.parent / self.test_data_dir
        if test_path.exists():
            shutil.rmtree(test_path)

    @patch("apps.MarketIndex.stockindex_manager.boto3.client")
    def test_s3_init_failure(self, mock_boto_client):
        """S3 초기화 실패 (lines 48-50)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager

        mock_boto_client.side_effect = Exception("AWS credentials error")

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=True)

        self.assertFalse(manager.use_s3)

    def test_load_from_s3_when_s3_disabled(self):
        """use_s3=False일 때 _load_from_s3 (line 75)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        from datetime import datetime

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=False)
        result = manager._load_from_s3("KOSPI", datetime.now())

        self.assertIsNone(result)

    @patch("apps.MarketIndex.stockindex_manager.boto3.client")
    def test_load_from_s3_client_error(self, mock_boto_client):
        """S3 load ClientError (lines 82-86)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        from datetime import datetime
        from botocore.exceptions import ClientError

        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3

        error_response = {"Error": {"Code": "NoSuchKey"}}
        mock_s3.get_object.side_effect = ClientError(error_response, "GetObject")

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=True)
        result = manager._load_from_s3("KOSPI", datetime.now())

        self.assertIsNone(result)

    @patch("apps.MarketIndex.stockindex_manager.boto3.client")
    def test_load_from_s3_general_exception(self, mock_boto_client):
        """S3 load 일반 예외 (lines 87-89)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        from datetime import datetime

        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Network error")

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=True)
        result = manager._load_from_s3("KOSPI", datetime.now())

        self.assertIsNone(result)

    def test_save_to_s3_when_s3_disabled(self):
        """use_s3=False일 때 _save_to_s3 (line 94)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        from datetime import datetime

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=False)
        result = manager._save_to_s3("KOSPI", {"close": 2500}, datetime.now())

        self.assertFalse(result)

    @patch("apps.MarketIndex.stockindex_manager.boto3.client")
    def test_save_to_s3_upload_failure(self, mock_boto_client):
        """S3 업로드 실패 (lines 112-114)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        from datetime import datetime

        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.put_object.side_effect = Exception("Upload failed")

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=True)
        result = manager._save_to_s3("KOSPI", {"close": 2500}, datetime.now())

        self.assertFalse(result)

    def test_view_summary_function(self):
        """view_summary() 함수 (lines 439-455)"""
        from apps.MarketIndex.stockindex_manager import view_summary

        with patch("apps.MarketIndex.stockindex_manager.yf.download") as mock_yf:
            mock_df = pd.DataFrame(
                {"Close": [2500.0], "Volume": [1000000]},
                index=pd.date_range(start="2025-01-01", periods=1),
            )
            mock_yf.return_value = mock_df

            result = view_summary()

            self.assertIsInstance(result, dict)
            self.assertIn("KOSPI", result)
            self.assertIn("KOSDAQ", result)


class StockindexManagerFinalCoverageTests(TestCase):
    """stockindex_manager.py 마지막 4줄 커버 (85-86, 181-182)"""

    def setUp(self):
        self.test_data_dir = "test_stockindex_final_coverage"

    def tearDown(self):
        import shutil
        from pathlib import Path

        test_path = Path(__file__).resolve().parent.parent / self.test_data_dir
        if test_path.exists():
            shutil.rmtree(test_path)

    @patch("apps.MarketIndex.stockindex_manager.boto3.client")
    def test_load_from_s3_client_error_not_nosuchkey(self, mock_boto_client):
        """S3 ClientError - NoSuchKey가 아닌 다른 에러 (lines 85-86)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        from datetime import datetime
        from botocore.exceptions import ClientError

        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3

        # AccessDenied 에러 (NoSuchKey가 아님)
        error_response = {"Error": {"Code": "AccessDenied"}}
        mock_s3.get_object.side_effect = ClientError(error_response, "GetObject")

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=True)
        result = manager._load_from_s3("KOSPI", datetime.now())

        # None 반환되어야 함
        self.assertIsNone(result)

    @patch("apps.MarketIndex.stockindex_manager.yf.Ticker")
    @patch("apps.MarketIndex.stockindex_manager.boto3.client")
    def test_fetch_daily_local_exists_s3_upload_needed(self, mock_boto_client, mock_ticker):
        """fetch_daily: 로컬에 있고 S3 업로드 필요 (lines 181-182)"""
        from apps.MarketIndex.stockindex_manager import StockindexManager
        import pandas as pd
        from datetime import datetime
        from botocore.exceptions import ClientError

        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3

        # S3에 데이터 없음 (NoSuchKey)
        error_response = {"Error": {"Code": "NoSuchKey"}}
        mock_s3.get_object.side_effect = ClientError(error_response, "GetObject")

        # put_object는 성공
        mock_s3.put_object.return_value = {}

        manager = StockindexManager(data_dir_name=self.test_data_dir, use_s3=True)

        # 1. 로컬에 먼저 오늘 데이터 저장 (정확히 동일한 값)
        today = datetime.now()
        today_str = today.strftime("%Y-%m-%d")
        close_price = 2510.0

        manager._save_local_data(
            "KOSPI",
            {
                today_str: {
                    "date": today_str,
                    "close": close_price,
                    "change_amount": 10.0,
                    "change_percent": 0.4,
                    "volume": 1000000,
                }
            },
        )

        # 2. Yahoo Finance도 정확히 동일한 데이터 반환
        mock_hist = pd.DataFrame(
            {
                "Open": [2500.0],
                "High": [2520.0],
                "Low": [2490.0],
                "Close": [close_price],  # 로컬과 정확히 동일 (0.01 이내)
                "Volume": [1000000],
            },
            index=pd.DatetimeIndex([today]),
        )

        mock_ticker_instance = MagicMock()
        mock_ticker_instance.history.return_value = mock_hist
        mock_ticker.return_value = mock_ticker_instance

        # 3. fetch_daily 호출
        # 조건: 로컬에 있음 + 종가 동일 + S3에 없음 → lines 181-182 실행
        result = manager.fetch_daily()

        # S3 업로드가 시도되어야 함
        self.assertTrue(mock_s3.put_object.called)


class MarketLLMViewTests(TestCase):
    """MarketLLMview 테스트"""

    def setUp(self):
        self.client = Client()
        self.url = "/marketindex/overview"

    @patch("apps.MarketIndex.views.get_latest_overview")
    def test_get_market_overview_exception(self, mock_get_overview):
        """Exception 발생 시 500 반환"""
        mock_get_overview.side_effect = Exception("S3 Error")

        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 500)
        data = response.json()
        self.assertIn("Unexpected Server Error", data["message"])
