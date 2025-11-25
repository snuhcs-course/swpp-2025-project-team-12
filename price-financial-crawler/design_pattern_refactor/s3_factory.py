from __future__ import annotations
from typing import Optional
import boto3
from botocore.client import BaseClient


class S3ClientFactory:
    _client: Optional[BaseClient] = None

    @classmethod
    def get_client(cls) -> BaseClient:
        if cls._client is None:
            cls._client = boto3.client("s3")
        return cls._client

    @classmethod
    def set_client(cls, client: BaseClient) -> None:
        cls._client = client
