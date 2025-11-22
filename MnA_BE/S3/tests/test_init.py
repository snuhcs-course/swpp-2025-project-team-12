# S3/tests/test_init.py
"""
S3/__init__.py 테스트
"""
from django.test import TestCase
from unittest.mock import patch, MagicMock
import os


class S3InitTests(TestCase):
    """S3/__init__.py 모듈 함수 테스트"""
    
    @patch.dict(os.environ, {
        'IAM_ACCESS_KEY_ID': 'test_iam_key',
        'IAM_SECRET_KEY': 'test_iam_secret',
        'AWS_REGION': 'us-west-2'
    }, clear=True)
    @patch('S3.boto3.client')
    def test_get_boto3_client_with_iam_keys(self, mock_boto_client):
        """IAM 키로 boto3 클라이언트 생성"""
        from S3 import get_boto3_client
        
        mock_client = MagicMock()
        mock_boto_client.return_value = mock_client
        
        client = get_boto3_client()
        
        # boto3.client가 올바른 인자로 호출되었는지 확인
        mock_boto_client.assert_called_once_with(
            's3',
            region_name='us-west-2',
            aws_access_key_id='test_iam_key',
            aws_secret_access_key='test_iam_secret'
        )
        self.assertEqual(client, mock_client)
    
    @patch.dict(os.environ, {
        'AWS_ACCESS_KEY_ID': 'test_aws_key',
        'AWS_SECRET_ACCESS_KEY': 'test_aws_secret'
    }, clear=True)
    @patch('S3.boto3.client')
    def test_get_boto3_client_with_aws_keys(self, mock_boto_client):
        """AWS 키로 boto3 클라이언트 생성"""
        from S3 import get_boto3_client
        
        mock_client = MagicMock()
        mock_boto_client.return_value = mock_client
        
        client = get_boto3_client()
        
        mock_boto_client.assert_called_once_with(
            's3',
            aws_access_key_id='test_aws_key',
            aws_secret_access_key='test_aws_secret'
        )
        self.assertEqual(client, mock_client)
    
    @patch.dict(os.environ, {}, clear=True)
    @patch('S3.boto3.client')
    def test_get_boto3_client_no_keys(self, mock_boto_client):
        """키 없이 boto3 클라이언트 생성 (기본 자격 증명 사용)"""
        from S3 import get_boto3_client
        
        mock_client = MagicMock()
        mock_boto_client.return_value = mock_client
        
        client = get_boto3_client()
        
        # 키가 없으면 빈 kwargs로 호출
        mock_boto_client.assert_called_once_with('s3')
        self.assertEqual(client, mock_client)
    
    @patch.dict(os.environ, {
        'IAM_ACCESS_KEY_ID': 'iam_key',
        'AWS_ACCESS_KEY_ID': 'aws_key',
        'IAM_SECRET_KEY': 'iam_secret',
        'AWS_SECRET_ACCESS_KEY': 'aws_secret'
    }, clear=True)
    @patch('S3.boto3.client')
    def test_get_boto3_client_priority_iam_over_aws(self, mock_boto_client):
        """IAM 키가 AWS 키보다 우선순위 높음"""
        from S3 import get_boto3_client
        
        mock_client = MagicMock()
        mock_boto_client.return_value = mock_client
        
        client = get_boto3_client()
        
        # IAM 키가 사용되어야 함
        call_kwargs = mock_boto_client.call_args[1]
        self.assertEqual(call_kwargs['aws_access_key_id'], 'iam_key')
        self.assertEqual(call_kwargs['aws_secret_access_key'], 'iam_secret')