# MnA_BE/S3/__init__.py  (통합본)

import os
import io
import base64
import traceback
import mimetypes
from io import BytesIO
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


class S3Client:
    """
    Boto3 S3 래퍼: 기존 HEAD의 메서드들을 유지/보강.
    - ENV 키 지원: IAM_ACCESS_KEY_ID / AWS_ACCESS_KEY_ID
                   IAM_SECRET_KEY   / AWS_SECRET_ACCESS_KEY
                   AWS_REGION (선택)
    """

    def __init__(self):
        access_key = _get_env("IAM_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID")
        secret_key = _get_env("IAM_SECRET_KEY", "AWS_SECRET_ACCESS_KEY")
        region = _get_env("AWS_REGION")

        kwargs = {}
        if region:
            kwargs["region_name"] = region
        if access_key and secret_key:
            kwargs["aws_access_key_id"] = access_key
            kwargs["aws_secret_access_key"] = secret_key

        try:
            self._client = boto3.client("s3", **kwargs)
        except Exception:
            debug_print(traceback.format_exc())
            raise

    # --- basic objects ---

    def get(self, bucket: str, key: str) -> bytes:
        """S3 object body bytes"""
        try:
            obj = self._client.get_object(Bucket=bucket, Key=key)
            return obj["Body"].read()
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't get object '{key}' from bucket '{bucket}'.")

    def delete(self, bucket: str, key: str) -> None:
        try:
            return self._client.delete_object(Bucket=bucket, Key=key)
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't delete object '{key}' from bucket '{bucket}'.")

    def put_file(self, bucket: str, key: str, path: str) -> None:
        """
        로컬 파일을 업로드. ContentType은 확장자로 추정.
        """
        try:
            ctype, _ = mimetypes.guess_type(path)
            extra = {"ContentType": ctype} if ctype else {}
            with open(path, "rb") as f:
                self._client.put_object(Bucket=bucket, Key=key, Body=f, **extra)
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't put object to bucket '{bucket}' from '{path}'.")

    # --- images ---

    def get_image_url(self, bucket: str, key: str) -> str:
        """
        이미지 바이트를 data:URL(base64)로 반환.
        """
        try:
            obj = self._client.get_object(Bucket=bucket, Key=key)
            content_type = obj.get("ContentType") or "application/octet-stream"
            b = obj["Body"].read()
            b64 = base64.b64encode(b).decode("utf-8")
            return f"data:{content_type};base64,{b64}"
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't get image from bucket '{bucket}' (key='{key}').")

    def put_image(self, bucket: str, key: str, image_url: str) -> None:
        """
        data:URL(base64) 포맷을 받아 업로드.
        """
        try:
            if ";base64," not in image_url:
                raise Exception("S3 ERROR: Invalid image data: not a base64 data URL.")

            header, b64 = image_url.split(";base64,", 1)
            content_type = header.split(":", 1)[1] if ":" in header else "application/octet-stream"
            try:
                data = base64.b64decode(b64)
            except Exception:
                raise Exception("S3 ERROR: Invalid base64 data.")

            self._client.put_object(
                Bucket=bucket,
                Key=key,
                Body=io.BytesIO(data),
                ContentType=content_type,
            )
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't put image bytes to bucket '{bucket}'.")

    # --- pandas helpers ---

    def get_dataframe(self, bucket: str, key: str) -> pd.DataFrame:
        """
        parquet/csv를 자동 판별하여 DataFrame으로 로드.
        """
        try:
            data = self.get(bucket, key)
            if key.lower().endswith((".parquet", ".pq")):
                return pd.read_parquet(io.BytesIO(data))
            elif key.lower().endswith(".csv"):
                return pd.read_csv(io.BytesIO(data))
            else:
                # 기본 parquet로 시도 → 실패시 csv
                try:
                    return pd.read_parquet(io.BytesIO(data))
                except Exception:
                    return pd.read_csv(io.BytesIO(data))
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't load DataFrame from s3://{bucket}/{key}")

    def put_dataframe(self, bucket: str, key: str, df: pd.DataFrame) -> None:
        """
        parquet/csv로 저장. 확장자에 맞춰 직렬화.
        """
        try:
            if key.lower().endswith((".parquet", ".pq")):
                bio = io.BytesIO()
                # pyarrow/fastparquet 중 하나가 설치되어 있어야 함
                df.to_parquet(bio, index=False)
                bio.seek(0)
                self._client.put_object(
                    Bucket=bucket,
                    Key=key,
                    Body=bio,
                    ContentType="application/octet-stream",  # 일부 클라이언트 호환
                )
            elif key.lower().endswith(".csv"):
                csv_bytes = df.to_csv(index=False).encode("utf-8")
                self._client.put_object(
                    Bucket=bucket,
                    Key=key,
                    Body=csv_bytes,
                    ContentType="text/csv; charset=utf-8",
                )
            else:
                # 기본 parquet
                bio = io.BytesIO()
                df.to_parquet(bio, index=False)
                bio.seek(0)
                self._client.put_object(
                    Bucket=bucket,
                    Key=key,
                    Body=bio,
                    ContentType="application/octet-stream",
                )
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(f"S3 ERROR: Couldn't write DataFrame to s3://{bucket}/{key}")


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
