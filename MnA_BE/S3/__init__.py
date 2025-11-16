# MnA_BE/S3/__init__.py  (통합본)

import os
import io
import base64
import traceback
import mimetypes
from io import BytesIO
from typing import Optional
import pandas as pd

import boto3

# debug_print 폴백
try:
    from utils.debug_print import debug_print
except Exception:  # util이 없으면 print로 대체
    def debug_print(msg):
        print(msg)

def _get_env(*keys: str, default: Optional[str] = None) -> Optional[str]:
    """
    환경변수 키 후보를 순서대로 조회. (IAM_* | AWS_* 모두 지원)
    """
    for k in keys:
        v = os.getenv(k)
        if v:
            return v
    return default


# 모듈 수준의 간단 클라이언트도 제공(기존 코드 호환용)
def get_boto3_client():
    access_key = _get_env("IAM_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID")
    secret_key = _get_env("IAM_SECRET_KEY", "AWS_SECRET_ACCESS_KEY")
    region = _get_env("AWS_REGION")

    kwargs = {}
    if region:
        kwargs["region_name"] = region
    if access_key and secret_key:
        kwargs["aws_access_key_id"] = access_key
        kwargs["aws_secret_access_key"] = secret_key

    return boto3.client("s3", **kwargs)
