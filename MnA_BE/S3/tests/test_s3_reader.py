from django.test import TestCase
from unittest.mock import patch, MagicMock
from datetime import datetime, timezone
import json
import io
import pandas as pd
from S3.s3_reader import (
    find_latest_object,
    read_object_bytes,
    read_latest_parquet_df,
    read_latest_json,
)


class S3ReaderTests(TestCase):
    """
    S3 Reader 함수 유닛 테스트
    실제 S3 대신 Mock 사용
    """
    
    @patch('S3.s3_reader._client')
    def test_find_latest_object_success(self, mock_client):
        """최신 객체 찾기 성공"""
        # Mock 설정
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        paginator_mock = MagicMock()
        s3_mock.get_paginator.return_value = paginator_mock
        
        # 가짜 S3 객체들
        obj1 = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
            "Size": 1000,
        }
        obj2 = {
            "Key": "data/2025-01-02.json",
            "LastModified": datetime(2025, 1, 2, tzinfo=timezone.utc),
            "Size": 2000,
        }
        obj3 = {
            "Key": "data/2025-01-03.json",
            "LastModified": datetime(2025, 1, 3, tzinfo=timezone.utc),
            "Size": 3000,
        }
        
        paginator_mock.paginate.return_value = [
            {"Contents": [obj1, obj2]},
            {"Contents": [obj3]},
        ]
        
        # 테스트
        result = find_latest_object("data/")
        
        # 검증
        self.assertIsNotNone(result)
        self.assertEqual(result["Key"], "data/2025-01-03.json")
        self.assertEqual(result["Size"], 3000)
        s3_mock.get_paginator.assert_called_once_with("list_objects_v2")
    
    @patch('S3.s3_reader._client')
    def test_find_latest_object_empty(self, mock_client):
        """객체가 없으면 None 반환"""
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        paginator_mock = MagicMock()
        s3_mock.get_paginator.return_value = paginator_mock
        
        # 빈 결과
        paginator_mock.paginate.return_value = [
            {"Contents": []},
        ]
        
        result = find_latest_object("nonexistent/")
        
        self.assertIsNone(result)
    
    @patch('S3.s3_reader._client')
    def test_find_latest_object_single(self, mock_client):
        """객체가 하나만 있으면 그것 반환"""
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        paginator_mock = MagicMock()
        s3_mock.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/only.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
            "Size": 500,
        }
        
        paginator_mock.paginate.return_value = [
            {"Contents": [obj]},
        ]
        
        result = find_latest_object("data/")
        
        self.assertIsNotNone(result)
        self.assertEqual(result["Key"], "data/only.json")
    
    @patch('S3.s3_reader._client')
    def test_read_object_bytes_success(self, mock_client):
        """객체 바이트 읽기 성공"""
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        # 가짜 바이트 데이터
        fake_data = b"Hello, S3!"
        body_mock = MagicMock()
        body_mock.read.return_value = fake_data
        
        s3_mock.get_object.return_value = {"Body": body_mock}
        
        result = read_object_bytes("test.txt")
        
        self.assertEqual(result, fake_data)
        s3_mock.get_object.assert_called_once()
    
    @patch('S3.s3_reader._client')
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_json_success(self, mock_find, mock_client):
        """최신 JSON 읽기 성공"""
        # find_latest_object Mock
        latest_obj = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        mock_find.return_value = latest_obj
        
        # read_object_bytes Mock
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        json_data = {"test": "value", "number": 123}
        body_mock = MagicMock()
        body_mock.read.return_value = json.dumps(json_data).encode("utf-8")
        s3_mock.get_object.return_value = {"Body": body_mock}
        
        # 테스트
        result_data, result_ts = read_latest_json("data/")
        
        # 검증
        self.assertIsNotNone(result_data)
        self.assertEqual(result_data, json_data)
        self.assertIn("2025-01-01T12:00:00", result_ts)
        mock_find.assert_called_once_with("data/")
    
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_json_not_found(self, mock_find):
        """객체가 없으면 None 반환"""
        mock_find.return_value = None
        
        result_data, result_ts = read_latest_json("nonexistent/")
        
        self.assertIsNone(result_data)
        self.assertIsNone(result_ts)
    
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_json_not_json_file(self, mock_find):
        """JSON 파일이 아니면 None 반환"""
        latest_obj = {
            "Key": "data/2025-01-01.parquet",  # .json이 아님
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        mock_find.return_value = latest_obj
        
        result_data, result_ts = read_latest_json("data/")
        
        self.assertIsNone(result_data)
        self.assertIsNone(result_ts)
    
    @patch('S3.s3_reader._client')
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_parquet_df_success(self, mock_find, mock_client):
        """최신 Parquet DataFrame 읽기 성공"""
        # find_latest_object Mock
        latest_obj = {
            "Key": "data/2025-01-01.parquet",
            "LastModified": datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
        mock_find.return_value = latest_obj
        
        # read_object_bytes Mock
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        # 가짜 DataFrame을 Parquet로 변환
        df = pd.DataFrame({"col1": [1, 2, 3], "col2": ["a", "b", "c"]})
        buffer = io.BytesIO()
        df.to_parquet(buffer)
        parquet_bytes = buffer.getvalue()
        
        body_mock = MagicMock()
        body_mock.read.return_value = parquet_bytes
        s3_mock.get_object.return_value = {"Body": body_mock}
        
        # 테스트
        result_df, result_ts = read_latest_parquet_df("data/")
        
        # 검증
        self.assertIsNotNone(result_df)
        self.assertIsInstance(result_df, pd.DataFrame)
        self.assertEqual(len(result_df), 3)
        self.assertIn("col1", result_df.columns)
        self.assertIn("2025-01-01T12:00:00", result_ts)
        mock_find.assert_called_once_with("data/")
    
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_parquet_df_not_found(self, mock_find):
        """객체가 없으면 None 반환"""
        mock_find.return_value = None
        
        result_df, result_ts = read_latest_parquet_df("nonexistent/")
        
        self.assertIsNone(result_df)
        self.assertIsNone(result_ts)
    
    @patch('S3.s3_reader._client')
    def test_read_object_bytes_with_bucket_param(self, mock_client):
        """BUCKET 환경변수 사용 확인"""
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        body_mock = MagicMock()
        body_mock.read.return_value = b"data"
        s3_mock.get_object.return_value = {"Body": body_mock}
        
        read_object_bytes("test.txt")
        
        # get_object 호출 시 Bucket 파라미터 확인
        call_args = s3_mock.get_object.call_args
        self.assertIn("Bucket", call_args.kwargs)
    
    @patch('S3.s3_reader._client')
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_json_timestamp_format(self, mock_find, mock_client):
        """타임스탬프가 ISO 8601 형식인지 확인"""
        latest_obj = {
            "Key": "data/test.json",
            "LastModified": datetime(2025, 1, 15, 14, 30, 45, tzinfo=timezone.utc),
        }
        mock_find.return_value = latest_obj
        
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        json_data = {"key": "value"}
        body_mock = MagicMock()
        body_mock.read.return_value = json.dumps(json_data).encode("utf-8")
        s3_mock.get_object.return_value = {"Body": body_mock}
        
        _, result_ts = read_latest_json("data/")
        
        # ISO 8601 형식 확인
        self.assertRegex(
            result_ts,
            r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}',
            "Timestamp should be in ISO 8601 format"
        )
        self.assertIn("2025-01-15", result_ts)
        self.assertIn("14:30:45", result_ts)
    
    @patch('S3.s3_reader._client')
    @patch('S3.s3_reader.find_latest_object')
    def test_read_latest_json_utf8_encoding(self, mock_find, mock_client):
        """UTF-8 인코딩 처리 확인"""
        latest_obj = {
            "Key": "data/korean.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        mock_find.return_value = latest_obj
        
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        # 한글 포함 JSON
        json_data = {"name": "삼성전자", "market": "코스피"}
        body_mock = MagicMock()
        body_mock.read.return_value = json.dumps(json_data, ensure_ascii=False).encode("utf-8")
        s3_mock.get_object.return_value = {"Body": body_mock}
        
        result_data, _ = read_latest_json("data/")
        
        self.assertEqual(result_data["name"], "삼성전자")
        self.assertEqual(result_data["market"], "코스피")
    
    @patch('S3.s3_reader._client')
    def test_find_latest_object_with_multiple_pages(self, mock_client):
        """여러 페이지에 걸친 객체들 중 최신 찾기"""
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        paginator_mock = MagicMock()
        s3_mock.get_paginator.return_value = paginator_mock
        
        # 페이지 1
        obj1 = {
            "Key": "data/2025-01-01.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        obj2 = {
            "Key": "data/2025-01-02.json",
            "LastModified": datetime(2025, 1, 2, tzinfo=timezone.utc),
        }
        
        # 페이지 2
        obj3 = {
            "Key": "data/2025-01-05.json",
            "LastModified": datetime(2025, 1, 5, tzinfo=timezone.utc),
        }
        
        # 페이지 3
        obj4 = {
            "Key": "data/2025-01-03.json",
            "LastModified": datetime(2025, 1, 3, tzinfo=timezone.utc),
        }
        
        paginator_mock.paginate.return_value = [
            {"Contents": [obj1, obj2]},
            {"Contents": [obj3]},
            {"Contents": [obj4]},
        ]
        
        result = find_latest_object("data/")
        
        # 2025-01-05가 최신
        self.assertEqual(result["Key"], "data/2025-01-05.json")
    
    @patch('S3.s3_reader._client')
    def test_find_latest_object_no_contents_key(self, mock_client):
        """Contents 키가 없는 페이지 처리"""
        s3_mock = MagicMock()
        mock_client.return_value = s3_mock
        
        paginator_mock = MagicMock()
        s3_mock.get_paginator.return_value = paginator_mock
        
        obj = {
            "Key": "data/test.json",
            "LastModified": datetime(2025, 1, 1, tzinfo=timezone.utc),
        }
        
        # 일부 페이지는 Contents 없음
        paginator_mock.paginate.return_value = [
            {},  # Contents 없음
            {"Contents": [obj]},
            {},  # Contents 없음
        ]
        
        result = find_latest_object("data/")
        
        self.assertIsNotNone(result)
        self.assertEqual(result["Key"], "data/test.json")