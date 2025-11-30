import base64
import io
import json
import mimetypes
import traceback
from datetime import timezone

from S3 import _get_env, debug_print
from S3.client_factory import S3ClientFactory


class BaseBucket:
    """
    Boto3 S3 래퍼: 기존 HEAD의 메서드들을 유지/보강.

    변경점:
    - S3 Client 생성 책임은 S3ClientFactory 로 분리
    - BaseBucket 은 "이미 만들어진 client + bucket_name" 을 사용
    """

    _client = None
    _bucket = None

    def __init__(self, client=None, bucket_name: str | None = None):
        """
        client / bucket_name 을 직접 주입하거나,
        미지정 시 Factory + ENV 로부터 기본값 사용.
        """
        try:
            # client 미지정 시: 기본 ENV 기반 클라이언트 (공통)
            if client is None:
                client = S3ClientFactory.from_env()

            # bucket_name 미지정 시: 공통 BUCKET_NAME 사용
            if bucket_name is None:
                bucket_name = _get_env("BUCKET_NAME")

            self._client = client
            self._bucket = bucket_name
        except Exception:
            debug_print(traceback.format_exc())
            raise

    # --- basic objects ---

    def get(self, key: str) -> bytes:
        """S3 object body bytes"""
        try:
            obj = self._client.get_object(Bucket=self._bucket, Key=key)
            return obj["Body"].read()
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(
                f"S3 ERROR: Couldn't get object '{key}' from bucket '{self._bucket}'."
            )

    def delete(self, key: str) -> None:
        try:
            self._client.delete_object(Bucket=self._bucket, Key=key)
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(
                f"S3 ERROR: Couldn't delete object '{key}' from bucket '{self._bucket}'."
            )

    def put_file(self, key: str, path: str) -> None:
        """
        로컬 파일을 업로드. ContentType은 확장자로 추정.
        """
        try:
            ctype, _ = mimetypes.guess_type(path)
            extra = {"ContentType": ctype} if ctype else {}
            with open(path, "rb") as f:
                self._client.put_object(
                    Bucket=self._bucket,
                    Key=key,
                    Body=f,
                    **extra,
                )
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(
                f"S3 ERROR: Couldn't put object to bucket '{self._bucket}' from '{path}'."
            )

    # --- utils  ---

    def get_list_v2(self, prefix: str):
        return self._client.list_objects_v2(Bucket=self._bucket, Prefix=prefix)

    def get_latest_object(self, prefix: str):
        paginator = self._client.get_paginator("list_objects_v2")
        latest = None
        pages = paginator.paginate(Bucket=self._bucket, Prefix=prefix)

        for page in pages:
            for obj in page.get("Contents", []):
                if latest is None:
                    latest = obj
                elif obj["LastModified"] > latest["LastModified"]:
                    latest = obj

        return latest

    def check_source(self, prefix: str):
        obj = self.get_latest_object(prefix)

        if not obj:
            return {"ok": False, "latest": None}

        key = obj["Key"]
        ts = None

        if "/" in key:
            filename = key.split("/")[-1]
            # assume  YYYY-MM-DD.{ext}
            if filename.count("-") >= 2:
                ts = filename.split(".")[0]  # ex) 2025-10-01
        return {
            "ok": True,
            "latest": ts or obj["LastModified"].strftime("%Y-%m-%d"),
        }

    # --- json ---

    def get_json(self, key):
        if not key.lower().endswith(".json"):
            return None
        data = self.get(key).decode("utf-8")
        return json.loads(data)

    def get_latest_json(self, prefix):
        latest = self.get_latest_object(prefix)

        if not latest:
            return None, None

        if not latest["Key"].lower().endswith(".json"):
            return None, None

        data = self.get(latest["Key"]).decode("utf-8")
        time = latest["LastModified"].astimezone(timezone.utc).isoformat()
        return json.loads(data), time

    # --- images ---

    def get_image_url(self, key: str) -> str:
        """
        이미지 바이트를 data:URL(base64)로 반환.
        """
        try:
            obj = self._client.get_object(Bucket=self._bucket, Key=key)
            content_type = obj.get("ContentType") or "application/octet-stream"
            b = obj["Body"].read()
            b64 = base64.b64encode(b).decode("utf-8")
            return f"data:{content_type};base64,{b64}"
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(
                f"S3 ERROR: Couldn't get image from bucket '{self._bucket}' (key='{key}')."
            )

    def put_image(self, key: str, image_url: str) -> None:
        """
        data:URL(base64) 포맷을 받아 업로드.
        """
        try:
            if ";base64," not in image_url:
                raise Exception(
                    "S3 ERROR: Invalid image data: not a base64 data URL."
                )

            header, b64 = image_url.split(";base64,", 1)
            content_type = (
                header.split(":", 1)[1] if ":" in header else "application/octet-stream"
            )
            try:
                data = base64.b64decode(b64)
            except Exception:
                raise Exception("S3 ERROR: Invalid base64 data.")

            self._client.put_object(
                Bucket=self._bucket,
                Key=key,
                Body=io.BytesIO(data),
                ContentType=content_type,
            )
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(
                f"S3 ERROR: Couldn't put image bytes to bucket '{self._bucket}'."
            )
