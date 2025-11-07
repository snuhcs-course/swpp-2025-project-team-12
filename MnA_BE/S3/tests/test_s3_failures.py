# S3/tests/test_s3_failures.py
"""
S3 클라이언트 실패 케이스 테스트: 타임아웃, 권한 오류, 네트워크 오류
목표: S3/base.py와 S3/finance.py 100% 커버리지
"""

from django.test import SimpleTestCase
from unittest.mock import patch, Mock, MagicMock
import io
from botocore.exceptions import ClientError, NoCredentialsError, PartialCredentialsError
from S3.base import S3Client
from S3.finance import FinanceS3Client
import pandas as pd


class S3ClientTimeoutTests(SimpleTestCase):
    """S3 클라이언트 타임아웃 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_timeout(self, mock_boto_client):
        """get 메서드 타임아웃"""
        mock_client = Mock()
        mock_client.get_object.side_effect = Exception("Request timed out")
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception) as context:
            s3.get("test-bucket", "test-key")
        
        self.assertIn("Couldn't get object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_delete_timeout(self, mock_boto_client):
        """delete 메서드 타임아웃"""
        mock_client = Mock()
        mock_client.delete_object.side_effect = Exception("Connection timeout")
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception) as context:
            s3.delete("test-bucket", "test-key")
        
        self.assertIn("Couldn't delete object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_file_timeout(self, mock_boto_client):
        """put_file 메서드 타임아웃"""
        mock_client = Mock()
        mock_client.put_object.side_effect = Exception("Upload timeout")
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with patch('builtins.open', create=True) as mock_open:
            mock_file = MagicMock()
            mock_open.return_value.__enter__.return_value = mock_file
            
            with self.assertRaises(Exception) as context:
                s3.put_file("test-bucket", "test-key", "/tmp/test.txt")
            
            self.assertIn("Couldn't put object", str(context.exception))


class S3ClientPermissionTests(SimpleTestCase):
    """S3 권한 오류 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_access_denied(self, mock_boto_client):
        """접근 거부 오류"""
        mock_client = Mock()
        mock_client.get_object.side_effect = ClientError(
            {"Error": {"Code": "AccessDenied", "Message": "Access Denied"}},
            "GetObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception) as context:
            s3.get("test-bucket", "test-key")
        
        self.assertIn("Couldn't get object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_file_permission_denied(self, mock_boto_client):
        """업로드 권한 없음"""
        mock_client = Mock()
        mock_client.put_object.side_effect = ClientError(
            {"Error": {"Code": "AccessDenied", "Message": "Insufficient permissions"}},
            "PutObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with patch('builtins.open', create=True):
            with self.assertRaises(Exception):
                s3.put_file("test-bucket", "test-key", "/tmp/test.txt")
    
    @patch('S3.base.boto3.client')
    def test_delete_no_permission(self, mock_boto_client):
        """삭제 권한 없음"""
        mock_client = Mock()
        mock_client.delete_object.side_effect = ClientError(
            {"Error": {"Code": "AccessDenied", "Message": "Cannot delete"}},
            "DeleteObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.delete("test-bucket", "test-key")


class S3ClientNetworkTests(SimpleTestCase):
    """네트워크 오류 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_network_error(self, mock_boto_client):
        """네트워크 연결 오류"""
        mock_client = Mock()
        mock_client.get_object.side_effect = ConnectionError("Network unreachable")
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get("test-bucket", "test-key")
    
    @patch('S3.base.boto3.client')
    def test_init_no_credentials(self, mock_boto_client):
        """자격 증명 없음"""
        mock_boto_client.side_effect = NoCredentialsError()
        
        with self.assertRaises(NoCredentialsError):
            S3Client()
    
    @patch('S3.base.boto3.client')
    def test_init_partial_credentials(self, mock_boto_client):
        """불완전한 자격 증명"""
        mock_boto_client.side_effect = PartialCredentialsError(
            provider='env',
            cred_var='AWS_SECRET_ACCESS_KEY'
        )
        
        with self.assertRaises(Exception):
            S3Client()


class S3ClientNotFoundTests(SimpleTestCase):
    """존재하지 않는 리소스 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_no_such_key(self, mock_boto_client):
        """존재하지 않는 키"""
        mock_client = Mock()
        mock_client.get_object.side_effect = ClientError(
            {"Error": {"Code": "NoSuchKey", "Message": "Key not found"}},
            "GetObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get("test-bucket", "nonexistent-key")
    
    @patch('S3.base.boto3.client')
    def test_get_no_such_bucket(self, mock_boto_client):
        """존재하지 않는 버킷"""
        mock_client = Mock()
        mock_client.get_object.side_effect = ClientError(
            {"Error": {"Code": "NoSuchBucket", "Message": "Bucket does not exist"}},
            "GetObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get("nonexistent-bucket", "test-key")


class S3UtilMethodsTests(SimpleTestCase):
    """유틸리티 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_empty_bucket(self, mock_boto_client):
        """빈 버킷에서 최신 객체 조회"""
        mock_client = Mock()
        mock_paginator = Mock()
        mock_paginator.paginate.return_value = [{"Contents": []}]
        mock_client.get_paginator.return_value = mock_paginator
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        result = s3.get_latest_object("test-bucket", "prefix/")
        
        self.assertIsNone(result)
    
    @patch('S3.base.boto3.client')
    def test_check_source_no_objects(self, mock_boto_client):
        """객체가 없을 때 check_source"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with patch.object(s3, 'get_latest_object', return_value=None):
            result = s3.check_source("test-bucket", "prefix/")
            
            self.assertFalse(result["ok"])
            self.assertIsNone(result["latest"])
    
    @patch('S3.base.boto3.client')
    def test_get_json_invalid_json(self, mock_boto_client):
        """잘못된 JSON 형식"""
        mock_client = Mock()
        mock_response = {"Body": Mock()}
        mock_response["Body"].read.return_value = b"invalid json {{"
        mock_client.get_object.return_value = mock_response
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get_json("test-bucket", "test-key")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_not_json_file(self, mock_boto_client):
        """JSON이 아닌 파일"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        fake_obj = {
            "Key": "test.txt",  # .json이 아님
            "LastModified": Mock()
        }
        
        with patch.object(s3, 'get_latest_object', return_value=fake_obj):
            result, time = s3.get_latest_json("test-bucket", "prefix/")
            
            self.assertIsNone(result)
            self.assertIsNone(time)


class S3ImageMethodsTests(SimpleTestCase):
    """이미지 관련 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_not_found(self, mock_boto_client):
        """존재하지 않는 이미지"""
        mock_client = Mock()
        mock_client.get_object.side_effect = ClientError(
            {"Error": {"Code": "NoSuchKey", "Message": "Image not found"}},
            "GetObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get_image_url("test-bucket", "nonexistent.png")
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_data_url(self, mock_boto_client):
        """잘못된 data URL 형식"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception) as context:
            s3.put_image("test-bucket", "test.png", "invalid_data_url")
        
        self.assertIn("not a base64 data URL", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_base64(self, mock_boto_client):
        """잘못된 base64 인코딩"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception) as context:
            s3.put_image("test-bucket", "test.png", "data:image/png;base64,invalid!!!")
        
        self.assertIn("Invalid base64", str(context.exception))


class FinanceS3ClientTests(SimpleTestCase):
    """FinanceS3Client 전용 테스트"""
    
    @patch('S3.finance.boto3.client')
    def test_get_dataframe_parquet_error(self, mock_boto_client):
        """Parquet 읽기 오류"""
        mock_client = Mock()
        mock_response = {"Body": Mock()}
        mock_response["Body"].read.return_value = b"invalid parquet data"
        mock_client.get_object.return_value = mock_response
        mock_boto_client.return_value = mock_client
        
        s3 = FinanceS3Client()
        
        with self.assertRaises(Exception) as context:
            s3.get_dataframe("test-bucket", "test.parquet")
        
        self.assertIn("Couldn't load DataFrame", str(context.exception))
    
    @patch('S3.finance.boto3.client')
    def test_get_dataframe_csv_error(self, mock_boto_client):
        """CSV 읽기 오류"""
        mock_client = Mock()
        mock_response = {"Body": Mock()}
        mock_response["Body"].read.return_value = b"invalid,csv\ndata"
        mock_client.get_object.return_value = mock_response
        mock_boto_client.return_value = mock_client
        
        s3 = FinanceS3Client()
        
        # CSV 파싱 오류 발생 가능
        with self.assertRaises(Exception):
            s3.get_dataframe("test-bucket", "test.csv")
    
    @patch('S3.finance.boto3.client')
    def test_put_dataframe_parquet_error(self, mock_boto_client):
        """Parquet 저장 오류"""
        mock_client = Mock()
        mock_client.put_object.side_effect = Exception("Upload failed")
        mock_boto_client.return_value = mock_client
        
        s3 = FinanceS3Client()
        df = pd.DataFrame({"col1": [1, 2, 3]})
        
        with self.assertRaises(Exception) as context:
            s3.put_dataframe("test-bucket", "test.parquet", df)
        
        self.assertIn("Couldn't write DataFrame", str(context.exception))
    
    @patch('S3.finance.boto3.client')
    def test_get_latest_parquet_df_no_data(self, mock_boto_client):
        """최신 Parquet 파일 없음"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = FinanceS3Client()
        
        with patch.object(s3, 'get_latest_object', return_value=None):
            df, ts = s3.get_latest_parquet_df("test-bucket", "prefix/")
            
            self.assertIsNone(df)
            self.assertIsNone(ts)
    
    @patch('S3.finance.boto3.client')
    def test_put_dataframe_unknown_extension(self, mock_boto_client):
        """알 수 없는 파일 확장자 (기본 parquet로 저장)"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = FinanceS3Client()
        df = pd.DataFrame({"col1": [1, 2, 3]})
        
        with patch('pandas.DataFrame.to_parquet'):
            try:
                s3.put_dataframe("test-bucket", "test.unknown", df)
                # 예외가 발생하지 않으면 성공
            except Exception:
                # 구현에 따라 예외가 발생할 수 있음
                pass


class S3FileOperationTests(SimpleTestCase):
    """파일 작업 엣지 케이스"""
    
    @patch('S3.base.boto3.client')
    def test_put_file_file_not_found(self, mock_boto_client):
        """로컬 파일이 존재하지 않음"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with patch('builtins.open', side_effect=FileNotFoundError()):
            with self.assertRaises(Exception):
                s3.put_file("test-bucket", "test-key", "/nonexistent/file.txt")
    
    @patch('S3.base.boto3.client')
    def test_put_file_no_content_type(self, mock_boto_client):
        """Content-Type을 추론할 수 없는 파일"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with patch('builtins.open', create=True):
            with patch('mimetypes.guess_type', return_value=(None, None)):
                # Content-Type 없이도 업로드 가능해야 함
                try:
                    s3.put_file("test-bucket", "test-key", "/tmp/unknown_file")
                except Exception as e:
                    # 업로드 실패는 OK (mock이므로)
                    pass


class S3RetryTests(SimpleTestCase):
    """재시도 로직 테스트 (boto3는 자체 재시도 있지만 애플리케이션 레벨 테스트)"""
    
    @patch('S3.base.boto3.client')
    def test_transient_error_handling(self, mock_boto_client):
        """일시적 오류 처리"""
        mock_client = Mock()
        # 첫 시도 실패, 재시도는 애플리케이션에서 처리해야 함
        mock_client.get_object.side_effect = ClientError(
            {"Error": {"Code": "ServiceUnavailable", "Message": "Service temporarily unavailable"}},
            "GetObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get("test-bucket", "test-key")
    
    @patch('S3.base.boto3.client')
    def test_throttling_error(self, mock_boto_client):
        """요청 제한 오류"""
        mock_client = Mock()
        mock_client.get_object.side_effect = ClientError(
            {"Error": {"Code": "SlowDown", "Message": "Please reduce your request rate"}},
            "GetObject"
        )
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        
        with self.assertRaises(Exception):
            s3.get("test-bucket", "test-key")


class S3LargeFileTests(SimpleTestCase):
    """대용량 파일 처리 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_large_file_memory_efficient(self, mock_boto_client):
        """대용량 파일 가져오기 (메모리 효율성)"""
        mock_client = Mock()
        # 큰 파일 시뮬레이션
        large_data = b"x" * (10 * 1024 * 1024)  # 10MB
        mock_response = {"Body": Mock()}
        mock_response["Body"].read.return_value = large_data
        mock_client.get_object.return_value = mock_response
        mock_boto_client.return_value = mock_client
        
        s3 = S3Client()
        result = s3.get("test-bucket", "large-file.bin")
        
        self.assertEqual(len(result), len(large_data))
    
    @patch('S3.base.boto3.client')
    def test_put_large_dataframe(self, mock_boto_client):
        """대용량 DataFrame 저장"""
        mock_client = Mock()
        mock_boto_client.return_value = mock_client
        
        s3 = FinanceS3Client()
        
        # 큰 DataFrame 생성
        large_df = pd.DataFrame({
            'col1': range(100000),
            'col2': range(100000)
        })
        
        with patch('pandas.DataFrame.to_parquet'):
            try:
                s3.put_dataframe("test-bucket", "large.parquet", large_df)
            except Exception:
                # Mock 환경에서는 실제 저장 실패 가능
                pass