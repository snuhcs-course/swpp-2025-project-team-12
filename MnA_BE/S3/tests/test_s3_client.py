# S3/tests/test_s3_client.py
"""
S3Client 테스트
"""

from django.test import TestCase
from unittest.mock import patch, MagicMock, mock_open
from datetime import datetime, timezone
import json
import io
import pandas as pd


# ==================== S3/base.py Tests ====================
class BaseS3ClientTests(TestCase):
    """S3/base.py의 S3Client 테스트"""
    
    @patch('S3.base.boto3.client')
    def test_init_exception(self, mock_boto_client):
        """__init__ Exception 처리"""
        from S3.base import BaseBucket
        
        mock_boto_client.side_effect = Exception("Boto3 init failed")
        
        with self.assertRaises(Exception):
            client = BaseBucket()
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_success(self, mock_boto_client):
        """최신 객체 찾기 성공"""
        from S3.base import BaseBucket
        
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
        
        client = BaseBucket()
        result = client.get_latest_object("data/")
        
        self.assertEqual(result["Key"], "data/2025-01-02.json")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_empty(self, mock_boto_client):
        """객체 없을 때"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = BaseBucket()
        result = client.get_latest_object("data/")
        
        self.assertIsNone(result)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_object_multiple_pages(self, mock_boto_client):
        """여러 페이지에서 latest 업데이트 확인"""
        from S3.base import BaseBucket
        
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
        
        client = BaseBucket()
        result = client.get_latest_object("data/")
        
        self.assertEqual(result["Key"], "data/newest.json")
    
    @patch('S3.base.boto3.client')
    def test_check_source_with_timestamp(self, mock_boto_client):
        """check_source: 파일명에 날짜 있을 때"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/2025-01-15.json",
            "LastModified": datetime(2025, 1, 15, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        result = client.check_source("data/")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-01-15")
    
    @patch('S3.base.boto3.client')
    def test_check_source_no_object(self, mock_boto_client):
        """check_source: 객체 없을 때"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        paginator_mock.paginate.return_value = [{"Contents": []}]
        
        client = BaseBucket()
        result = client.check_source("data/")
        
        self.assertFalse(result["ok"])
        self.assertIsNone(result["latest"])
    
    @patch('S3.base.boto3.client')
    def test_check_source_without_slash(self, mock_boto_client):
        """파일명에 / 없을 때"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data.json",
            "LastModified": datetime(2025, 1, 15, 10, 30, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        result = client.check_source("")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-01-15")
    
    @patch('S3.base.boto3.client')
    def test_check_source_no_date_pattern(self, mock_boto_client):
        """파일명에 날짜 패턴 없을 때"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "prefix/simple.json",
            "LastModified": datetime(2025, 2, 20, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        result = client.check_source("prefix/")
        
        self.assertTrue(result["ok"])
        self.assertEqual(result["latest"], "2025-02-20")
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_success(self, mock_boto_client):
        """최신 JSON 가져오기 성공"""
        from S3.base import BaseBucket
        
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
        
        client = BaseBucket()
        data, ts = client.get_latest_json("data/")
        
        self.assertEqual(data, json_data)
        self.assertIsNotNone(ts)
    
    @patch('S3.base.boto3.client')
    def test_get_latest_json_not_json_file(self, mock_boto_client):
        """JSON 파일이 아닐 때"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        paginator_mock = MagicMock()
        mock_s3.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/test.parquet",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        paginator_mock.paginate.return_value = [{"Contents": [obj]}]
        
        client = BaseBucket()
        data, ts = client.get_latest_json("data/")
        
        self.assertIsNone(data)
        self.assertIsNone(ts)
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_default_content_type(self, mock_boto_client):
        """이미지 ContentType 없을 때 기본값 사용"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"image data"
        mock_s3.get_object.return_value = {
            "Body": body_mock,
        }
        
        client = BaseBucket()
        result = client.get_image_url("image.png")
        
        self.assertTrue(result.startswith("data:application/octet-stream;base64,"))
    
    @patch('S3.base.boto3.client')
    def test_put_image_no_colon_in_header(self, mock_boto_client):
        """이미지 URL에 : 없을 때"""
        from S3.base import BaseBucket
        import base64
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        fake_data = b"test"
        b64_data = base64.b64encode(fake_data).decode('utf-8')
        image_url = f"data;base64,{b64_data}"
        
        client = BaseBucket()
        client.put_image("image.png", image_url)
        
        call_kwargs = mock_s3.put_object.call_args[1]
        self.assertEqual(call_kwargs['ContentType'], "application/octet-stream")
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_url_format(self, mock_boto_client):
        """put_image: 잘못된 URL 형식"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.put_image("image.png", "not-a-data-url")
        
        self.assertIn("Couldn't put image bytes", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_invalid_base64(self, mock_boto_client):
        """put_image: 잘못된 base64"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.put_image("image.png", "data:image/png;base64,invalid!!!")
        
        self.assertIn("Couldn't put image bytes", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_put_image_s3_exception(self, mock_boto_client):
        """put_image: S3 업로드 실패"""
        from S3.base import BaseBucket
        import base64
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        mock_s3.put_object.side_effect = Exception("S3 put failed")
        
        client = BaseBucket()
        
        fake_data = b"test"
        b64_data = base64.b64encode(fake_data).decode('utf-8')
        image_url = f"data:image/png;base64,{b64_data}"
        
        with self.assertRaises(Exception) as context:
            client.put_image("image.png", image_url)
        
        self.assertIn("Couldn't put image bytes", str(context.exception))
    
    @patch('S3.base.boto3.client')
    @patch('S3.base.mimetypes.guess_type', return_value=('text/plain', None))
    @patch('builtins.open', new_callable=mock_open, read_data=b'test content')
    def test_put_file_success(self, mock_file, mock_guess_type, mock_boto_client):
        """파일 업로드 성공 (라인 65-68 커버)"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        client = BaseBucket()
        client.put_file("test-key", "/fake/path.txt")
        
        # open이 제대로 호출되었는지 확인
        mock_file.assert_called_once_with("/fake/path.txt", "rb")
        
        # put_object가 호출되었는지 확인
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args[1]
        # Bucket은 환경변수에 따라 설정되므로 체크하지 않음
        self.assertEqual(call_kwargs['Key'], 'test-key')
        self.assertEqual(call_kwargs['ContentType'], 'text/plain')
    
    @patch('S3.base.boto3.client')
    def test_put_file_exception(self, mock_boto_client):
        """파일 업로드 실패 - except 블록 커버 (라인 69-71)"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        
        # open이 FileNotFoundError를 발생시키도록 설정
        with patch('builtins.open', side_effect=FileNotFoundError("File not found")):
            client = BaseBucket()
            
            with self.assertRaises(Exception) as context:
                client.put_file("test-key", "/nonexistent.txt")
            
            # except 블록에서 발생한 예외 메시지 확인
            self.assertIn("Couldn't put object", str(context.exception))
            if client._bucket:
                self.assertIn(str(client._bucket), str(context.exception))
            self.assertIn("/nonexistent.txt", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_delete_exception(self, mock_boto_client):
        """삭제 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.delete_object.side_effect = Exception("Delete Error")
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.delete("test-key")
        
        self.assertIn("Couldn't delete object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_get_exception(self, mock_boto_client):
        """가져오기 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Get Error")
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.get("test-key")
        
        self.assertIn("Couldn't get object", str(context.exception))
    
    @patch('S3.base.boto3.client')
    def test_get_image_url_exception(self, mock_boto_client):
        """이미지 URL 가져오기 실패"""
        from S3.base import BaseBucket
        
        mock_s3 = MagicMock()
        mock_boto_client.return_value = mock_s3
        mock_s3.get_object.side_effect = Exception("Image Error")
        
        client = BaseBucket()
        
        with self.assertRaises(Exception) as context:
            client.get_image_url("image.png")
        
        self.assertIn("Couldn't get image", str(context.exception))