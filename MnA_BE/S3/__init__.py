# MnA_BE/S3/__init__.py  (통합본)

import os
from typing import Optional

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


# Pure S3 Client
def get_boto3_client(
    access_key = _get_env("IAM_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID"),
    secret_key = _get_env("IAM_SECRET_KEY", "AWS_SECRET_ACCESS_KEY")
):
    """
    Puer S3 Client
    """
    region = _get_env("AWS_REGION")

    kwargs = {}
    if region:
        kwargs["region_name"] = region
    if access_key and secret_key:
        kwargs["aws_access_key_id"] = access_key
        kwargs["aws_secret_access_key"] = secret_key

    return boto3.client("s3", **kwargs)
