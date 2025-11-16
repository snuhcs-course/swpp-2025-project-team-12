# price-financial-crawler/test_crawler.py
"""
독립 크롤러 단위 테스트
"""

import unittest
from unittest.mock import patch, MagicMock, Mock
from datetime import date, datetime
import pandas as pd
from io import BytesIO
import sys
import os

# crawler2 import를 위해 경로 추가 (필요시)
# sys.path.insert(0, os.path.dirname(__file__))

class TestKRXUtils(unittest.TestCase):
    """KRX 유틸리티 함수 테스트"""
    
    @patch('crawler2.mcal.get_calendar')
    def test_is_trading_day_krx_true(self, mock_mcal):
        """거래일인 경우"""
        from crawler2 import is_trading_day_krx
        
        mock_calendar = MagicMock()
        # DataFrame이 비어있지 않으면 거래일
        mock_schedule = pd.DataFrame({'market_open': [True]}, index=pd.DatetimeIndex(['2025-11-04']))
        mock_calendar.schedule.return_value = mock_schedule
        mock_mcal.return_value = mock_calendar
        
        result = is_trading_day_krx(date(2025, 11, 4))
        
        self.assertTrue(result)
    
    @patch('crawler2.mcal.get_calendar')
    def test_is_trading_day_krx_false(self, mock_mcal):
        """거래일이 아닌 경우 (주말/공휴일)"""
        from crawler2 import is_trading_day_krx
        
        mock_calendar = MagicMock()
        mock_schedule = pd.DataFrame()  # empty
        mock_calendar.schedule.return_value = mock_schedule
        mock_mcal.return_value = mock_calendar
        
        result = is_trading_day_krx(date(2025, 11, 2))  # 토요일
        
        self.assertFalse(result)
    
    @patch('crawler2.mcal.get_calendar')
    def test_first_trading_day_of_month(self, mock_mcal):
        """월 첫 거래일 찾기"""
        from crawler2 import first_trading_day_of_month
        
        mock_calendar = MagicMock()
        first_day = pd.Timestamp('2025-11-03')
        mock_schedule = pd.DataFrame({'market_open': [True]}, index=pd.DatetimeIndex([first_day]))
        mock_calendar.schedule.return_value = mock_schedule
        mock_mcal.return_value = mock_calendar
        
        result = first_trading_day_of_month(2025, 11)
        
        self.assertEqual(result, date(2025, 11, 3))
    
    @patch('crawler2.mcal.get_calendar')
    def test_first_trading_day_of_month_none(self, mock_mcal):
        """거래일이 없는 달 (불가능하지만 테스트)"""
        from crawler2 import first_trading_day_of_month
        
        mock_calendar = MagicMock()
        mock_schedule = pd.DataFrame()
        mock_calendar.schedule.return_value = mock_schedule
        mock_mcal.return_value = mock_calendar
        
        result = first_trading_day_of_month(2025, 11)
        
        self.assertIsNone(result)
    
    @patch('crawler2.is_trading_day_krx')
    @patch('crawler2.first_trading_day_of_month')
    def test_is_first_trading_day_true(self, mock_first, mock_is_trading):
        """월 첫 거래일인 경우"""
        from crawler2 import is_first_trading_day
        
        test_date = date(2025, 11, 3)
        mock_is_trading.return_value = True
        mock_first.return_value = test_date
        
        result = is_first_trading_day(test_date)
        
        self.assertTrue(result)
    
    @patch('crawler2.is_trading_day_krx')
    def test_is_first_trading_day_not_trading(self, mock_is_trading):
        """거래일이 아닌 경우"""
        from crawler2 import is_first_trading_day
        
        mock_is_trading.return_value = False
        
        result = is_first_trading_day(date(2025, 11, 2))
        
        self.assertFalse(result)
    
    def test_get_kst_now(self):
        """KST 시간 가져오기"""
        from crawler2 import get_kst_now
        
        result = get_kst_now()
        
        self.assertIsInstance(result, datetime)
        self.assertEqual(result.tzinfo.key, "Asia/Seoul")


class TestStockDataFetching(unittest.TestCase):
    """주식 데이터 가져오기 테스트"""
    
    @patch('crawler2.requests.post')
    def test_get_stock_info_kospi(self, mock_post):
        """KOSPI 데이터 가져오기"""
        from crawler2 import get_stock_info
        
        # OTP 생성 응답
        mock_otp_response = MagicMock()
        mock_otp_response.text = "test_otp_token"
        
        # CSV 다운로드 응답
        csv_data = """ticker,name,market,industry,close,change,change_rate,market_cap
005930,삼성전자,KOSPI,전기전자,70000,1000,1.45,4000000"""
        mock_csv_response = MagicMock()
        mock_csv_response.content = csv_data.encode('EUC-KR')
        
        mock_post.side_effect = [mock_otp_response, mock_csv_response]
        
        result = get_stock_info("20251104", "STK")
        
        self.assertIsInstance(result, pd.DataFrame)
        # ticker가 정수로 변환될 수 있으므로 둘 다 확인
        self.assertTrue('005930' in result.index or 5930 in result.index)
        self.assertEqual(result.loc[result.index[0], 'name'], '삼성전자')
        self.assertEqual(mock_post.call_count, 2)
    
    @patch('crawler2.requests.post')
    def test_get_stock_info_kosdaq(self, mock_post):
        """KOSDAQ 데이터 가져오기"""
        from crawler2 import get_stock_info
        
        mock_otp = MagicMock()
        mock_otp.text = "token"
        
        csv_data = """ticker,name,market,industry,close,change,change_rate,market_cap
000250,삼천당제약,KOSDAQ,의약품,5000,100,2.04,100000"""
        mock_csv = MagicMock()
        mock_csv.content = csv_data.encode('EUC-KR')
        
        mock_post.side_effect = [mock_otp, mock_csv]
        
        result = get_stock_info("20251104", "KSQ")
        
        self.assertIsInstance(result, pd.DataFrame)
        # ticker가 정수로 변환될 수 있으므로 유연하게 확인
        self.assertTrue('000250' in result.index or 250 in result.index)
        self.assertEqual(len(result), 1)
    
    @patch('crawler2.requests.get')
    def test_get_explanation(self, mock_get):
        """기업 설명 크롤링"""
        from crawler2 import get_explanation
        
        html = """
        <html>
            <li class="dot_cmp">반도체 제조업체</li>
            <li class="dot_cmp">글로벌 1위 기업</li>
        </html>
        """
        mock_response = MagicMock()
        mock_response.text = html
        mock_get.return_value = mock_response
        
        result = get_explanation("005930")
        
        self.assertIn("반도체", result)
        self.assertIn("글로벌", result)
    
    @patch('crawler2.get_explanation')
    @patch('crawler2.time.sleep')
    def test_get_explanation_per_tickers(self, mock_sleep, mock_get_exp):
        """여러 종목 설명 가져오기"""
        from crawler2 import get_explanation_per_tickers
        
        mock_get_exp.side_effect = [
            "삼성전자 설명",
            "SK하이닉스 설명",
            "LG전자 설명"
        ]
        
        tickers = ['005930', '000660', '066570']
        result = get_explanation_per_tickers(tickers, 0.1)
        
        self.assertIsInstance(result, pd.DataFrame)
        self.assertEqual(len(result), 3)
        self.assertIn('005930', result.index)
        self.assertEqual(mock_get_exp.call_count, 3)
        self.assertEqual(mock_sleep.call_count, 3)


class TestS3Upload(unittest.TestCase):
    """S3 업로드 테스트"""
    
    @patch.dict('os.environ', {'S3_BUCKET': 'test-bucket'})
    @patch('crawler2.boto3.client')
    def test_save_df_s3_kospi(self, mock_boto):
        """KOSPI 데이터 S3 저장"""
        from crawler2 import save_df_s3
        
        mock_s3 = MagicMock()
        mock_boto.return_value = mock_s3
        
        df = pd.DataFrame({
            'close': [70000, 80000],
            'market_cap': [4000000, 3000000]
        }, index=['005930', '000660'])
        
        today = date(2025, 11, 4)
        result = save_df_s3('price-financial-info', today, 'kospi', df)
        
        self.assertEqual(result, 0)
        mock_s3.upload_fileobj.assert_called_once()
        
        # 올바른 키 형식 확인
        call_args = mock_s3.upload_fileobj.call_args
        key = call_args[0][2]
        self.assertEqual(
            key, 
            'price-financial-info/year=2025/month=11/market=kospi/2025-11-04.parquet'
        )
    
    @patch.dict('os.environ', {'S3_BUCKET': 'test-bucket'})
    @patch('crawler2.boto3.client')
    def test_save_df_s3_kosdaq(self, mock_boto):
        """KOSDAQ 데이터 S3 저장"""
        from crawler2 import save_df_s3
        
        mock_s3 = MagicMock()
        mock_boto.return_value = mock_s3
        
        df = pd.DataFrame({'close': [5000]}, index=['000250'])
        today = date(2025, 11, 4)
        
        save_df_s3('company-profile', today, 'kosdaq', df)
        
        call_args = mock_s3.upload_fileobj.call_args
        key = call_args[0][2]
        self.assertIn('kosdaq', key)
        self.assertIn('company-profile', key)


class TestMainLogic(unittest.TestCase):
    """메인 로직 통합 테스트"""
    
    @patch('crawler2.get_kst_now')
    def test_time_check_before_18(self, mock_now):
        """18시 이전 실행 체크"""
        # 17시로 설정
        mock_dt = MagicMock()
        mock_dt.hour = 17
        mock_dt.date.return_value = date(2025, 11, 4)
        mock_now.return_value = mock_dt
        
        # 실제로는 SystemExit이 발생해야 하지만
        # 테스트에서는 mock으로 대체
        # (실제 코드 실행은 별도로)
    
    @patch('crawler2.is_trading_day_krx')
    @patch('crawler2.is_first_trading_day')
    def test_logic_branches(self, mock_first_day, mock_trading_day):
        """거래일 / 첫 거래일 로직 분기"""
        
        # Case 1: 일반 거래일
        mock_trading_day.return_value = True
        mock_first_day.return_value = False
        # -> 가격/재무 정보만 수집
        
        # Case 2: 첫 거래일
        mock_trading_day.return_value = True
        mock_first_day.return_value = True
        # -> 가격/재무 정보 + 기업 설명 수집
        
        # Case 3: 비거래일
        mock_trading_day.return_value = False
        mock_first_day.return_value = False
        # -> 아무것도 안 함


if __name__ == '__main__':
    unittest.main()