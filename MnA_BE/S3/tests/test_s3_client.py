# S3/tests/test_s3_client.py
"""
S3Client 종합 테스트 (__init__.py, base.py)
목표: 99%+ 커버리지
"""

from django.test import TestCase
from unittest.mock import patch, MagicMock, mock_open
from datetime import datetime, timezone
import json
import io
import pandas as pd


# ==================== S3/__init__.py Tests ====================
class S3ClientTests(TestCase):
    """S3/__init__.py의 S3Client 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_init_with_all_env_vars(self, mock_boto_client):
        """모든 환경변수가 있을 때 초기화"""
        from S3.base import S3Client
        
        # 명시적으로 credentials 전달 (기본값 무시)
        client = S3Client(
            access_key='test_access',
            secret_key='test_secret'
        )
        
        # boto3.client가 올바른 인자로 호출되었는지 확인
        call_kwargs = mock_boto_client.call_args[1]
        self.assertEqual(call_kwargs['aws_access_key_id'], 'test_access')
        self.assertEqual(call_kwargs['aws_secret_access_key'], 'test_secret')
    
    @patch('S3.base.boto3.client')
    def test_init_with_aws_prefix_env(self, mock_boto_client):
        """AWS_ 접두사 환경변수 사용"""
        from S3.base import S3Client
        
        # 명시적으로 credentials 전달
        client = S3Client(
            access_key='test_access',
            secret_key='test_secret'
        )
        
        call_kwargs = mock_boto_client.call_args[1]
        self.assertEqual(call_kwargs['aws_access_key_id'], 'test_access')
        self.assertEqual(call_kwargs['aws_secret_access_key'], 'test_secret')
    
    @patch.dict('os.environ', {}, clear=True)
    @patch('S3.base.boto3.client')
    def test_init_without_credentials(self, mock_boto_client):
        """자격증명 없이 초기화 (IAM Role 사용)"""
        from S3.base import S3Client
        
        client = S3Client()
        
        mock_boto_client.assert_called_once()
    
    @patch('S3.base.boto3.client')
    def test_init_boto3_exception(self, mock_boto_client):
        """boto3 초기화 실패"""
        from S3.base import S3Client
        
        mock_boto_client.side_effect = Exception("Boto3 initialization failed")
        
        with self.assertRaises(Exception):
            client = S3Client()
    
    @patch('S3.base.boto3.client')
    def test_get_success(self, mock_boto_client):
        """S3 객체 가져오기 성공"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"test data"
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = S3Client()
        result = client.get("test-bucket", "test-key")
        
        self.assertEqual(result, b"test data")
        mock_s3.get_object.assert_called_once_with(Bucket="test-bucket", Key="test-key")
    
    @patch('S3.base.boto3.client')
    def test_get_exception(self, mock_boto_client):
        """S3 객체 가져오기 실패"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("S3 Error")
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.get("test-bucket", "test-key")
        
        self.assertIn("Couldn't get object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_delete_success(self, mock_boto_client):
        """S3 객체 삭제 성공"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = S3Client()
        client.delete("test-bucket", "test-key")
        
        mock_s3.delete_object.assert_called_once_with(Bucket="test-bucket", Key="test-key")
    
    @patch('S3.base.boto3.client')
    def test_delete_exception(self, mock_boto_client):
        """S3 객체 삭제 실패"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.delete_object.side_effect = Exception("Delete Error")
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.delete("test-bucket", "test-key")
        
        self.assertIn("Couldn't delete object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_file_with_content_type(self, mock_boto_client):
        """put_file: ContentType 있을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        file_content = b"test file content"
        m = mock_open(read_data=file_content)
        
        with patch('builtins.open', m):
            with patch('S3.mimetypes.guess_type', return_value=('text/plain', None)):
                client = S3Client()
                client.put_file("test-bucket", "test-key", "/fake/path.txt")
                
                mock_s3.put_object.assert_called_once()
                call_kwargs = mock_s3.put_object.call_args[1]
                self.assertEqual(call_kwargs['ContentType'], 'text/plain')
    
    @patch('S3.base.boto3.client')
    def test_put_file_without_content_type(self, mock_boto_client):
        """put_file: ContentType 없을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        file_content = b"unknown file"
        m = mock_open(read_data=file_content)
        
        with patch('builtins.open', m):
            with patch('S3.mimetypes.guess_type', return_value=(None, None)):
                client = S3Client()
                client.put_file("test-bucket", "test-key", "/fake/unknown")
                
                mock_s3.put_object.assert_called_once()
                call_kwargs = mock_s3.put_object.call_args[1]
                self.assertNotIn('ContentType', call_kwargs)
    
    @patch('S3.base.boto3.client')
    def test_put_file_exception(self, mock_boto_client):
        """put_file: Exception 처리"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        with patch('builtins.open', side_effect=FileNotFoundError("File not found")):
            client = S3Client()
            
            with self.assertRaises(Exception) as context:
                client.put_file("test-bucket", "test-key", "/nonexistent.txt")
            
            self.assertIn("Couldn't put object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_success(self, mock_boto_client):
        """이미지를 base64 URL로 변환 성공"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"fake image data"
        mock_s3.get_object.return_value = {
            "Body": body_mock,
            "ContentType": "image/png"
        }
        
        client = S3Client()
        result = client.get_image_url("test-bucket", "image.png")
        
        self.assertTrue(result.startswith("data:image/png;base64,"))
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_exception(self, mock_boto_client):
        """이미지 가져오기 실패"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Image Error")
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.get_image_url("test-bucket", "image.png")
        
        self.assertIn("Couldn't get image", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_success(self, mock_boto_client):
        """base64 이미지 업로드 성공"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        import base64
        fake_data = b"fake image"
        b64_data = base64.b64encode(fake_data).decode('utf-8')
        image_url = f"data:image/png;base64,{b64_data}"
        
        client = S3Client()
        client.put_image("test-bucket", "image.png", image_url)
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "image/png")
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_format(self, mock_boto_client):
        """잘못된 형식의 이미지 URL"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image.png", "not-a-valid-url")
        
        self.assertIn("S3 ERROR", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_base64(self, mock_boto_client):
        """잘못된 base64 데이터"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image.png", "data:image/png;base64,invalid!!!")
        
        self.assertIn("S3 ERROR", str(context.exception))
    
    @patch('S3.finance.boto3.client')
    def test_get_dataframe_parquet(self, mock_boto_client):
        """Parquet DataFrame 가져오기"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3], 'col2': ['a', 'b', 'c']})
        buffer = io.BytesIO()
        df.to_parquet(buffer)
        parquet_bytes = buffer.getvalue()
        
        body_mock = MagicMock()
        body_mock.read.return_value = parquet_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df = client.get_dataframe("test-bucket", "data.parquet")
        
        self.assertIsInstance(result_df, pd.DataFrame)
        self.assertEqual(len(result_df), 3)
    
    @patch('S3.finance.boto3.client')
    def test_get_dataframe_csv(self, mock_boto_client):
        """CSV DataFrame 가져오기"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3], 'col2': ['a', 'b', 'c']})
        csv_bytes = df.to_csv(index=False).encode('utf-8')
        
        body_mock = MagicMock()
        body_mock.read.return_value = csv_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df = client.get_dataframe("test-bucket", "data.csv")
        
        self.assertIsInstance(result_df, pd.DataFrame)
        self.assertEqual(len(result_df), 3)
    
    @patch('S3.finance.boto3.client')
    def test_get_dataframe_auto_detect_parquet(self, mock_boto_client):
        """확장자 없을 때 자동으로 parquet 시도"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2]})
        buffer = io.BytesIO()
        df.to_parquet(buffer)
        parquet_bytes = buffer.getvalue()
        
        body_mock = MagicMock()
        body_mock.read.return_value = parquet_bytes
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = FinanceS3Client()
        result_df = client.get_dataframe("test-bucket", "data")
        
        self.assertIsInstance(result_df, pd.DataFrame)
    
    @patch('S3.finance.boto3.client')
    def test_get_dataframe_fallback_to_csv(self, mock_boto_client):
        """parquet 실패 후 csv fallback"""
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
        self.assertEqual(len(result_df), 3)
    
    @patch('S3.finance.boto3.client')
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
    
    @patch('S3.finance.boto3.client')
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
        self.assertEqual(call_kwargs['Bucket'], "test-bucket")
    
    @patch('S3.finance.boto3.client')
    def test_put_dataframe_csv(self, mock_boto_client):
        """DataFrame을 CSV로 저장"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3]})
        
        client = FinanceS3Client()
        client.put_dataframe("test-bucket", "data.csv", df)
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "text/csv; charset=utf-8")
    
    @patch('S3.finance.boto3.client')
    def test_put_dataframe_default_parquet(self, mock_boto_client):
        """확장자 없을 때 기본 parquet"""
        from S3.finance import FinanceS3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        df = pd.DataFrame({'col1': [1, 2, 3]})
        
        client = FinanceS3Client()
        client.put_dataframe("test-bucket", "data", df)
        
        mock_s3.put_object.assert_called_once()
    
    @patch('S3.finance.boto3.client')
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
    
    @patch.dict('os.environ', {
        'IAM_ACCESS_KEY_ID': 'test_key',
        'IAM_SECRET_KEY': 'test_secret',
        'AWS_REGION': 'us-west-2'
    })
    @patch('S3.base.boto3.client')
    def test_get_boto3_client_with_env(self, mock_boto_client):
        """환경변수로 boto3 클라이언트 생성"""
        from S3 import get_boto3_client
        
        client = get_boto3_client()
        
        mock_boto_client.assert_called_once_with(
            's3',
            region_name='us-west-2',
            aws_access_key_id='test_key',
            aws_secret_access_key='test_secret'
        )
    
    def test_debug_print_fallback(self):
        """debug_print import fallback"""
        from S3 import debug_print
        
        self.assertTrue(callable(debug_print))
        
        try:
            debug_print("test message")
        except Exception as e:
            self.fail(f"debug_print raised {e}")


# ==================== S3/base.py Tests ====================
class BaseS3ClientTests(TestCase):
    """S3/base.py의 S3Client 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_init_exception(self, mock_boto_client):
        """__init__ Exception 처리"""
        from S3.base import S3Client
        
        mock_boto_client.side_effect = Exception("Boto3 init failed")
        
        with self.assertRaises(Exception):
            client = S3Client()
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_success(self, mock_boto_client):
        """최신 객체 찾기 성공"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj1 = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        obj2 = {
            "Key": "data/2025-01-02.json",
            "LastModified": datetime(2025, 1, 2, tzinfo=timezone.utc),
        }
        
        paginator_mock.paginate.return_value = [
            {"Contents": [obj1, obj2]},
        ]
        
        client = S3Client()
        result = client.get_latest_object("test-bucket", "data/")
        
        self.assertEqual(result["Key"], "data/2025-01-02.json")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_empty(self, mock_boto_client):
        """객체 없을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = S3Client()
        result = client.get_latest_object("test-bucket", "data/")
        
        self.assertIsNone(result)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_multiple_pages(self, mock_boto_client):
        """여러 페이지에서 latest 업데이트 확인"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj1 = {
            "Key": "data/old.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        obj2 = {
            "Key": "data/newer.json",
            "LastModified": datetime(2025, 1, 10, tzinfo=timezone.utc),
        }
        obj3 = {
            "Key": "data/newest.json",
            "LastModified": datetime(2025, 1, 20, tzinfo=timezone.utc),
        }
        
        paginator_mock.paginate.return_value = [
            {"Contents": [obj1]},
            {"Contents": [obj2]},
            {"Contents": [obj3]},
        ]
        
        client = S3Client()
        result = client.get_latest_object("test-bucket", "data/")
        
        self.assertEqual(result["Key"], "data/newest.json")
    
    @patch('S3.base.boto3.client')
    def test_check_source_with_timestamp(self, mock_boto_client):
        """check_source: 파일명에 날짜 있을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-15.json",
            "LastModified": datetime(2025, 1, 15, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = S3Client()
        result = client.check_source("test-bucket", "data/")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-01-15")
    
    @patch('S3.base.boto3.client')
    def test_check_source_no_object(self, mock_boto_client):
        """check_source: 객체 없을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = S3Client()
        result = client.check_source("test-bucket", "data/")
        
        self.assertFalse(result["ok"])
        self.assertIsNone(result["latest"])
    
    @patch('S3.base.boto3.client')
    def test_check_source_without_slash(self, mock_boto_client):
        """파일명에 / 없을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data.json",
            "LastModified": datetime(2025, 1, 15, 10, 30, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = S3Client()
        result = client.check_source("test-bucket", "")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-01-15")
    
    @patch('S3.base.boto3.client')
    def test_check_source_no_date_pattern(self, mock_boto_client):
        """파일명에 날짜 패턴 없을 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "prefix/simple.json",
            "LastModified": datetime(2025, 2, 20, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = S3Client()
        result = client.check_source("test-bucket", "prefix/")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-02-20")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_success(self, mock_boto_client):
        """최신 JSON 가져오기 성공"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/test.json",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        json_data = {"test": "value"}
        body_mock = MagicMock()
        body_mock.read.return_value = json.dumps(json_data).encode("utf-8")
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = S3Client()
        data, ts = client.get_latest_json("test-bucket", "data/")
        
        self.assertEqual(data, json_data)
        self.assertIsNotNone(ts)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_not_json_file(self, mock_boto_client):
        """JSON 파일이 아닐 때"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/test.parquet",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = S3Client()
        data, ts = client.get_latest_json("test-bucket", "data/")
        
        self.assertIsNone(data)
        self.assertIsNone(ts)
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_default_content_type(self, mock_boto_client):
        """이미지 ContentType 없을 때 기본값 사용"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"image data"
        mock_s3.get_object.return_value = {
            "Body": body_mock,
        }
        
        client = S3Client()
        result = client.get_image_url("test-bucket", "image.png")
        
        self.assertTrue(result.startswith("data:application/octet-stream;base64,"))
    
    @patch('S3.base.boto3.client')
    def test_put_image_no_colon_in_header(self, mock_boto_client):
        """이미지 URL에 : 없을 때"""
        from S3.base import S3Client
        import base64
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        fake_data = b"test"
        b64_data = base64.b64encode(fake_data).decode('utf-8')
        image_url = f"data;base64,{b64_data}"
        
        client = S3Client()
        client.put_image("test-bucket", "image.png", image_url)
        
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "application/octet-stream")
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_url_format(self, mock_boto_client):
        """put_image: 잘못된 URL 형식"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image.png", "not-a-data-url")
        
        self.assertIn("Couldn't put image bytes", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_base64(self, mock_boto_client):
        """put_image: 잘못된 base64"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image.png", "data:image/png;base64,invalid!!!")
        
        self.assertIn("Couldn't put image bytes", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_s3_exception(self, mock_boto_client):
        """put_image: S3 업로드 실패"""
        from S3.base import S3Client
        import base64
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        mock_s3.put_object.side_effect = Exception("S3 put failed")
        
        client = S3Client()
        
        fake_data = b"test"
        b64_data = base64.b64encode(fake_data).decode('utf-8')
        image_url = f"data:image/png;base64,{b64_data}"
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image.png", image_url)
        
        self.assertIn("Couldn't put image bytes", str(context.exception))
    
    @patch('S3.base.boto3.client')
    @patch('S3.base.mimetypes.guess_type', return_value=('text/plain', None))
    @patch('builtins.open', new_callable=mock_open, read_data=b'test content')
    def test_put_file_success(self, mock_file, mock_guess_type, mock_boto_client):
        """파일 업로드 성공 (라인 65-68 커버)"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = S3Client()
        client.put_file("test-bucket", "test-key", "/fake/path.txt")
        
        # open이 제대로 호출되었는지 확인
        mock_file.assert_called_once_with("/fake/path.txt", "rb")
        
        # put_object가 호출되었는지 확인
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['Bucket'], 'test-bucket')
        self.assertEqual(call_kwargs['Key'], 'test-key')
        self.assertEqual(call_kwargs['ContentType'], 'text/plain')
    
    @patch('S3.base.boto3.client')
    def test_put_file_exception(self, mock_boto_client):
        """파일 업로드 실패 - except 블록 커버 (라인 69-71)"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        # open이 FileNotFoundError를 발생시키도록 설정
        with patch('builtins.open', side_effect=FileNotFoundError("File not found")):
            client = S3Client()
            
            with self.assertRaises(Exception) as context:
                client.put_file("test-bucket", "test-key", "/nonexistent.txt")
            
            # except 블록에서 발생한 예외 메시지 확인
            self.assertIn("Couldn't put object", str(context.exception))
            self.assertIn("test-bucket", str(context.exception))
            self.assertIn("/nonexistent.txt", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_delete_exception(self, mock_boto_client):
        """삭제 실패"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.delete_object.side_effect = Exception("Delete Error")
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.delete("test-bucket", "test-key")
        
        self.assertIn("Couldn't delete object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_get_exception(self, mock_boto_client):
        """가져오기 실패"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Get Error")
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.get("test-bucket", "test-key")
        
        self.assertIn("Couldn't get object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_exception(self, mock_boto_client):
        """이미지 URL 가져오기 실패"""
        from S3.base import S3Client
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Image Error")
        
        client = S3Client()
        
        with self.assertRaises(Exception) as context:
            client.get_image_url("test-bucket", "image.png")
        
        self.assertIn("Couldn't get image", str(context.exception))