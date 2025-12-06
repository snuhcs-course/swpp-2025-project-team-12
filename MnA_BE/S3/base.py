import base64
import io
import json
import mimetypes
import traceback
from datetime import timezone

from S3 import _get_env, debug_print
from S3.client_factory import S3ClientFactory

# UAT 날짜 유틸리티 import
try:
    from utils.uat_date import get_uat_date, is_date_on_or_before, extract_date_from_key
except ImportError:
    # utils.uat_date가 없으면 UAT 모드 비활성화
    def get_uat_date():
        return None
    def is_date_on_or_before(key, uat_date):
        return False
    def extract_date_from_key(key):
        return None


class BaseBucket:
    """
    Boto3 S3 래퍼: 기존 HEAD의 메서드들을 유지/보강.

    변경점:
    - S3 Client 생성 책임은 S3ClientFactory 로 분리
    - BaseBucket 은 "이미 만들어진 client + bucket_name" 을 사용
    - UAT 모드: UAT_DATE 환경변수 설정 시 해당 날짜 데이터만 반환
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
                client = S3ClientFactory().create()

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
        """
        prefix 내 최신 객체 반환.
        
        UAT 모드(UAT_DATE 환경변수 설정 시):
            해당 날짜 이전의 가장 최근 객체 반환
        
        일반 모드:
            LastModified 기준 최신 객체 반환
        """
        paginator = self._client.get_paginator("list_objects_v2")
        pages = paginator.paginate(Bucket=self._bucket, Prefix=prefix)
        
        uat_date = get_uat_date()
        
        if uat_date:
            # UAT 모드: 해당 날짜 이전의 가장 최근 데이터
            matching_objects = []
            for page in pages:
                for obj in page.get("Contents", []):
                    if is_date_on_or_before(obj["Key"], uat_date):
                        matching_objects.append(obj)
            
            if not matching_objects:
                debug_print(f"[UAT] No objects found on or before {uat_date} in prefix {prefix}")
                return None
            
            # 날짜 기준 가장 최근 것 선택
            def get_date(obj):
                return extract_date_from_key(obj["Key"]) or ""
            
            latest = max(matching_objects, key=get_date)
            debug_print(f"[UAT] Using object on or before {uat_date}: {latest['Key']}")
            return latest
        
        # 일반 모드: 기존 로직
        latest = None
        for page in pages:
            for obj in page.get("Contents", []):
                if latest is None:
                    latest = obj
                elif obj["LastModified"] > latest["LastModified"]:
                    latest = obj

        return latest

    def check_source(self, prefix: str):
        """
        prefix 내 최신 데이터 소스 확인.
        
        UAT 모드: UAT_DATE 반환
        일반 모드: 파일명에서 날짜 추출 또는 LastModified
        """
        uat_date = get_uat_date()
        
        if uat_date:
            # UAT 모드: 해당 날짜 객체가 존재하는지 확인
            obj = self.get_latest_object(prefix)
            if obj:
                return {"ok": True, "latest": uat_date}
            return {"ok": False, "latest": None}
        
        # 일반 모드: 기존 로직
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