# S3/tests/test_s3_base_enhanced.py
"""
S3 Base 클라이언트 추가 테스트 (base.py 누락 부분)
목표: 56% → 95%+ 커버리지
"""

from django.test import TestCase
from unittest.mock import patch, MagicMock, mock_open
from datetime import datetime, timezone
import json
import io


class S3ClientInitTests(TestCase):
    """S3Client 초기화 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_init_with_region(self, mock_boto_client):
        """리전 설정과 함께 초기화"""
        from S3.base import BaseBucket
        
        with patch.dict('os.environ', {
            'IAM_ACCESS_KEY_ID': 'test_access_key',
            'IAM_SECRET_KEY': 'test_secret_key',
            'AWS_REGION': 'us-west-2'
        }):
            client = BaseBucket()
            
            mock_boto_client.assert_called_once()
            call_kwargs = mock_boto_client.call_args[1]
            self.assertEqual(call_kwargs['region_name'], 'us-west-2')
    
    @patch('S3.base.boto3.client')
    def test_init_without_region(self, mock_boto_client):
        """리전 없이 초기화"""
        from S3.base import BaseBucket
        
        with patch.dict('os.environ', {
            'IAM_ACCESS_KEY_ID': 'test_access_key',
            'IAM_SECRET_KEY': 'test_secret_key'
        }, clear=True):
            client = BaseBucket()
            
            mock_boto_client.assert_called_once()
            call_kwargs = mock_boto_client.call_args[1]
            self.assertNotIn('region_name', call_kwargs)
    
    @patch('S3.base.boto3.client')
    def test_init_failure(self, mock_boto_client):
        """초기화 실패"""
        from S3.base import BaseBucket
        
        mock_boto_client.side_effect = Exception("Boto3 error")
        
        with self.assertRaises(Exception):
            client = BaseBucket()


class S3ClientGetTests(TestCase):
    """get 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_success(self, mock_boto_client):
        """정상적으로 객체 가져오기"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"test content"
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = BaseBucket()
        result = client.get("test-bucket", "test-key")
        
        self.assertEqual(result, b"test content")
        mock_s3.get_object.assert_called_once_with(Bucket="test-bucket", Key="test-key")
    
    @patch('S3.base.boto3.client')
    def test_get_exception(self, mock_boto_client):
        """객체 가져오기 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("S3 Error")
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.get("test-bucket", "test-key")
        
        self.assertIn("Couldn't get object", str(context.exception))


class S3ClientDeleteTests(TestCase):
    """delete 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_delete_success(self, mock_boto_client):
        """정상적으로 객체 삭제"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        client.delete("test-bucket", "test-key")
        
        mock_s3.delete_object.assert_called_once_with(Bucket="test-bucket", Key="test-key")
    
    @patch('S3.base.boto3.client')
    def test_delete_exception(self, mock_boto_client):
        """객체 삭제 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.delete_object.side_effect = Exception("Delete Error")
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.delete("test-bucket", "test-key")
        
        self.assertIn("Couldn't delete object", str(context.exception))


class S3ClientPutFileTests(TestCase):
    """put_file 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    @patch('builtins.open', new_callable=mock_open, read_data=b"file content")
    def test_put_file_with_content_type(self, mock_file, mock_boto_client):
        """ContentType과 함께 파일 업로드"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        client.put_file("test-bucket", "test-key", "/path/to/file.json")
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], 'application/json')
    
    @patch('S3.base.boto3.client')
    @patch('builtins.open', new_callable=mock_open, read_data=b"file content")
    def test_put_file_without_content_type(self, mock_file, mock_boto_client):
        """ContentType 추정 불가능한 파일 업로드"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        client.put_file("test-bucket", "test-key", "/path/to/file.unknown")
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertNotIn('ContentType', call_kwargs)
    
    @patch('S3.base.boto3.client')
    def test_put_file_exception(self, mock_boto_client):
        """파일 업로드 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.put_file("test-bucket", "test-key", "/nonexistent/file.txt")
        
        self.assertIn("Couldn't put object", str(context.exception))


class S3ClientGetLatestObjectTests(TestCase):
    """get_latest_object 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_single(self, mock_boto_client):
        """단일 객체 반환"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        latest = client.get_latest_object("test-bucket", "data/")
        
        self.assertEqual(latest["Key"], "data/2025-01-01.json")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_multiple(self, mock_boto_client):
        """여러 객체 중 최신 반환"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        objs = [
            {
                "Key": "data/2025-01-01.json",
                "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
            },
            {
                "Key": "data/2025-01-02.json",
                "LastModified": datetime(2025, 1, 2, 12, 0, 0, tzinfo=timezone.utc),
            },
            {
                "Key": "data/2025-01-03.json",
                "LastModified": datetime(2025, 1, 3, 12, 0, 0, tzinfo=timezone.utc),
            }
        ]
        paginator_mock.paginate.return_value = [{"Contents": objs}]
        
        client = BaseBucket()
        latest = client.get_latest_object("test-bucket", "data/")
        
        self.assertEqual(latest["Key"], "data/2025-01-03.json")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_empty(self, mock_boto_client):
        """객체 없음"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = BaseBucket()
        latest = client.get_latest_object("test-bucket", "data/")
        
        self.assertIsNone(latest)


class S3ClientCheckSourceTests(TestCase):
    """check_source 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_check_source_with_date_in_filename(self, mock_boto_client):
        """파일명에 날짜가 있는 경우"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        result = client.check_source("test-bucket", "data/")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-01-01")
    
    @patch('S3.base.boto3.client')
    def test_check_source_without_date_in_filename(self, mock_boto_client):
        """파일명에 날짜가 없는 경우 (LastModified 사용)"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/file.json",
            "LastModified": datetime(2025, 1, 5, 10, 30, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        result = client.check_source("test-bucket", "data/")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-01-05")
    
    @patch('S3.base.boto3.client')
    def test_check_source_no_object(self, mock_boto_client):
        """객체 없음"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = BaseBucket()
        result = client.check_source("test-bucket", "data/")
        
        self.assertFalse(result["ok"])
        self.assertIsNone(result["latest"])


class S3ClientGetJsonTests(TestCase):
    """get_json 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_json_success(self, mock_boto_client):
        """JSON 가져오기 성공"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        json_data = {"key": "value", "number": 123}
        body_mock = MagicMock()
        body_mock.read.return_value = json.dumps(json_data).encode("utf-8")
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = BaseBucket()
        result = client.get_json("test-bucket", "data")
        
        self.assertEqual(result, json_data)
        mock_s3.get_object.assert_called_once_with(Bucket="test-bucket", Key="data.json")


class S3ClientGetLatestJsonTests(TestCase):
    """get_latest_json 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_success(self, mock_boto_client):
        """최신 JSON 가져오기 성공"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        json_data = {"key": "value"}
        body_mock = MagicMock()
        body_mock.read.return_value = json.dumps(json_data).encode("utf-8")
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = BaseBucket()
        result_json, result_time = client.get_latest_json("test-bucket", "data/")
        
        self.assertEqual(result_json, json_data)
        self.assertIsNotNone(result_time)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_no_object(self, mock_boto_client):
        """객체 없음"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = BaseBucket()
        result_json, result_time = client.get_latest_json("test-bucket", "data/")
        
        self.assertIsNone(result_json)
        self.assertIsNone(result_time)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_not_json_file(self, mock_boto_client):
        """JSON 파일이 아님"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-01.txt",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        result_json, result_time = client.get_latest_json("test-bucket", "data/")
        
        self.assertIsNone(result_json)
        self.assertIsNone(result_time)


class S3ClientGetImageUrlTests(TestCase):
    """get_image_url 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_success(self, mock_boto_client):
        """이미지 URL 가져오기 성공"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"\x89PNG\r\n\x1a\n"
        mock_s3.get_object.return_value = {
            "Body": body_mock,
            "ContentType": "image/png"
        }
        
        client = BaseBucket()
        result = client.get_image_url("test-bucket", "image-key")
        
        self.assertTrue(result.startswith("data:image/png;base64,"))
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_no_content_type(self, mock_boto_client):
        """ContentType 없는 경우"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"image data"
        mock_s3.get_object.return_value = {"Body": body_mock}
        
        client = BaseBucket()
        result = client.get_image_url("test-bucket", "image-key")
        
        self.assertTrue(result.startswith("data:application/octet-stream;base64,"))
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_exception(self, mock_boto_client):
        """이미지 가져오기 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Get Error")
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.get_image_url("test-bucket", "image-key")
        
        self.assertIn("Couldn't get image", str(context.exception))


class S3ClientPutImageTests(TestCase):
    """put_image 메서드 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_put_image_success(self, mock_boto_client):
        """이미지 업로드 성공"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        image_url = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        
        client = BaseBucket()
        client.put_image("test-bucket", "image-key", image_url)
        
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "image/png")
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_format(self, mock_boto_client):
        """잘못된 이미지 형식"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image-key", "invalid-image-url")
        
        # S3 base.py가 모든 에러를 Couldn't put image로 래핑함
        self.assertIn("Couldn't put image", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_base64(self, mock_boto_client):
        """잘못된 base64 데이터"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image-key", "data:image/png;base64,invalid!!!")
        
        # S3 base.py가 모든 에러를 Couldn't put image로 래핑함
        self.assertIn("Couldn't put image", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_exception(self, mock_boto_client):
        """이미지 업로드 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.put_object.side_effect = Exception("Put Error")
        
        image_url = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.put_image("test-bucket", "image-key", image_url)
        
        self.assertIn("Couldn't put image", str(context.exception))