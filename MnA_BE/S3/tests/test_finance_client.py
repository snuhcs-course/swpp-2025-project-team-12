# S3/tests/test_finance_client.py
"""
FinanceS3Client 종합 테스트 (finance.py)
목표: 100% 커버리지
"""

from django.test import TestCase
from unittest.mock import patch, MagicMock
from datetime import datetime, timezone
import io
import pandas as pd


class FinanceS3ClientTests(TestCase):
    """S3/finance.py의 FinanceS3Client 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_init_with_finance_credentials(self, mock_boto_client):
        """FINANCE_ 접두사 환경변수로 초기화"""
        from S3.finance import FinanceS3Client
        
        client = FinanceS3Client()
        
        self.assertTrue(mock_boto_client.called)
    
    @patch('S3.base.boto3.client')
    def test_get_dataframe_parquet(self, mock_boto_client):
        """Parquet DataFrame 가져오기"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'price': [100, 200, 300]})
        buffer = io.BytesIO()
        df.to_parquet(buffer)
        parquet_bytes = buffer.getvalue()
        
        body_mock = MagicMock()
        body_mock.read.return_value = parquet_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df = client.get_dataframe("test-bucket", "prices.parquet")
        
        self.assertIsInstance(result_df, pd.DataFrame)
        self.assertIn('price', result_df.columns)
    
    @patch('S3.base.boto3.client')
    def test_get_dataframe_csv_direct(self, mock_boto_client):
        """CSV 파일 직접 읽기"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'price': [100, 200]})
        csv_bytes = df.to_csv(index=False).encode('utf-8')
        
        body_mock = MagicMock()
        body_mock.read.return_value = csv_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df = client.get_dataframe("test-bucket", "data.csv")
        
        self.assertIsInstance(result_df, pd.DataFrame)
        self.assertIn('price', result_df.columns)
    
    @patch('S3.base.boto3.client')
    def test_get_dataframe_auto_detect_fallback_csv(self, mock_boto_client):
        """확장자 없을 때 parquet 실패 후 csv 시도"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3]})
        csv_bytes = df.to_csv(index=False).encode('utf-8')
        
        body_mock = MagicMock()
        body_mock.read.return_value = csv_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df = client.get_dataframe("test-bucket", "data")
        
        self.assertIsInstance(result_df, pd.DataFrame)
    
    @patch('S3.base.boto3.client')
    def test_get_dataframe_exception(self, mock_boto_client):
        """DataFrame 로드 실패"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("S3 Error")
        
        client = FinanceS3Client()
        
        with self.assertRaises(Exception) as context:
            client.get_dataframe("test-bucket", "data.parquet")
        
        self.assertIn("Couldn't load DataFrame", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_get_latest_parquet_df_success(self, mock_boto_client):
        """최신 Parquet DataFrame 가져오기"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-01.parquet",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        df = pd.DataFrame({'col1': [1, 2, 3]})
        buffer = io.BytesIO()
        df.to_parquet(buffer)
        parquet_bytes = buffer.getvalue()
        
        body_mock = MagicMock()
        body_mock.read.return_value = parquet_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df, ts = client.get_latest_parquet_df("test-bucket", "data/")
        
        self.assertIsInstance(result_df, pd.DataFrame)
        self.assertEqual(len(result_df), 3)
        self.assertIsNotNone(ts)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_parquet_df_no_object(self, mock_boto_client):
        """최신 Parquet 없을 때"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = FinanceS3Client()
        df, ts = client.get_latest_parquet_df("test-bucket", "data/")
        
        self.assertIsNone(df)
        self.assertIsNone(ts)
    
    @patch('S3.base.boto3.client')
    def test_put_dataframe_parquet(self, mock_boto_client):
        """DataFrame을 Parquet으로 저장"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3]})
        
        client = FinanceS3Client()
        client.put_dataframe("test-bucket", "data.parquet", df)
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "application/octet-stream")
    
    @patch('S3.base.boto3.client')
    def test_put_dataframe_csv(self, mock_boto_client):
        """DataFrame을 CSV로 저장"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'stock': ['AAPL', 'GOOGL'], 'price': [150, 2800]})
        
        client = FinanceS3Client()
        client.put_dataframe("test-bucket", "stocks.csv", df)
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "text/csv; charset=utf-8")
    
    @patch('S3.base.boto3.client')
    def test_put_dataframe_default_extension(self, mock_boto_client):
        """확장자 없을 때 기본 parquet"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3]})
        
        client = FinanceS3Client()
        client.put_dataframe("test-bucket", "data", df)
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "application/octet-stream")
    
    @patch('S3.base.boto3.client')
    def test_put_dataframe_exception(self, mock_boto_client):
        """DataFrame 저장 실패"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.put_object.side_effect = Exception("Put Error")
        
        client = FinanceS3Client()
        df = pd.DataFrame({'col1': [1]})
        
        with self.assertRaises(Exception) as context:
            client.put_dataframe("test-bucket", "data.parquet", df)
        
        self.assertIn("Couldn't write DataFrame", str(context.exception))

class DebugPrintFallbackTests(TestCase):
    """S3/__init__.py 라인 17-19 커버 (debug_print fallback)"""
    
    @patch.dict('sys.modules', {'utils.debug_print': None})
    def test_debug_print_fallback_used(self):
        """utils.debug_print import 실패 시 fallback 사용"""
        import importlib
        import S3
        
        # S3 모듈 리로드하여 fallback 경로 실행
        importlib.reload(S3)
        
        # fallback debug_print가 정의되어 있는지 확인
        from S3 import debug_print
        self.assertTrue(callable(debug_print))
        
        # 실제로 호출해서 에러 없이 동작하는지 확인
        try:
            debug_print("test message")
        except Exception as e:
            self.fail(f"debug_print raised {e}")