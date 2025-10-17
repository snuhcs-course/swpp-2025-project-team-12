# apps/common/s3_reader.py
import os, io, json
from typing import Optional, Tuple
from datetime import timezone
import boto3
import pandas as pd

BUCKET = os.getenv("S3_BUCKET", "swpp-12-bucket")
AWS_REGION = os.getenv("AWS_REGION", "ap-northeast-2")

def _client():
    return boto3.client("s3", region_name=AWS_REGION)

def find_latest_object(prefix: str) -> Optional[dict]:
    """
    지정 prefix 아래 최종 수정시간(LastModified)이 가장 최신인 객체를 반환.
    객체 dict에는 'Key', 'LastModified', 'Size' 등이 포함됨.
    """
    s3 = _client()
    paginator = s3.get_paginator("list_objects_v2")
    latest = None
    for page in paginator.paginate(Bucket=BUCKET, Prefix=prefix):
        for obj in page.get("Contents", []):
            if (latest is None) or (obj["LastModified"] > latest["LastModified"]):
                latest = obj
    return latest

def read_object_bytes(key: str) -> bytes:
    s3 = _client()
    obj = s3.get_object(Bucket=BUCKET, Key=key)
    return obj["Body"].read()

def read_latest_parquet_df(prefix: str) -> Tuple[Optional[pd.DataFrame], Optional[str]]:
    latest = find_latest_object(prefix)
    if not latest:
        return None, None
    data = read_object_bytes(latest["Key"])
    df = pd.read_parquet(io.BytesIO(data))
    ts = latest["LastModified"].astimezone(timezone.utc).isoformat()
    return df, ts

def read_latest_json(prefix: str) -> Tuple[Optional[dict], Optional[str]]:
    latest = find_latest_object(prefix)
    if not latest:
        return None, None
    if not latest["Key"].lower().endswith(".json"):
        return None, None
    data = read_object_bytes(latest["Key"]).decode("utf-8")
    ts = latest["LastModified"].astimezone(timezone.utc).isoformat()
    return json.loads(data), ts
