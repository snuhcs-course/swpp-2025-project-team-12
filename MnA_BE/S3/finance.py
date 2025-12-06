import io
import traceback
from datetime import timezone

import pandas as pd

from S3 import _get_env, debug_print
from S3.base import BaseBucket
from S3.client_factory import S3ClientFactory


class FinanceBucket(BaseBucket):
    """
    Finance 전용 S3 버킷 래퍼.

    변경점:
    - S3 Client 생성은 S3ClientFactory.for_finance() 에 위임
    - bucket_name 은 FINANCE_BUCKET_NAME ENV 기본 사용
    """

    def __init__(self, client=None, bucket_name: str | None = None):
        try:
            if client is None:
                client = S3ClientFactory().create("finance")
            if bucket_name is None:
                bucket_name = _get_env("FINANCE_BUCKET_NAME")

            super().__init__(client=client, bucket_name=bucket_name)
        except Exception:
            debug_print(traceback.format_exc())
            raise

    # --- pandas helpers ---

    def get_dataframe(self, key: str) -> pd.DataFrame:
        """
        parquet/csv를 자동 판별하여 DataFrame으로 로드.
        """
        try:
            data = self.get(key)
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
            raise Exception(
                f"S3 ERROR: Couldn't load DataFrame from s3://{self._bucket}/{key}"
            )

    def get_latest_parquet_df(self, prefix):
        latest = self.get_latest_object(prefix)

        if not latest:
            return None, None

        data = self.get(latest["Key"])
        df = pd.read_parquet(io.BytesIO(data))
        ts = latest["LastModified"].astimezone(timezone.utc).isoformat()

        return df, ts

    def put_dataframe(self, key: str, df: pd.DataFrame) -> None:
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
                    Bucket=self._bucket,
                    Key=key,
                    Body=bio,
                    ContentType="application/octet-stream",  # 일부 클라이언트 호환
                )
            elif key.lower().endswith(".csv"):
                csv_bytes = df.to_csv(index=False).encode("utf-8")
                self._client.put_object(
                    Bucket=self._bucket,
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
                    Bucket=self._bucket,
                    Key=key,
                    Body=bio,
                    ContentType="application/octet-stream",
                )
        except Exception:
            debug_print(traceback.format_exc())
            raise Exception(
                f"S3 ERROR: Couldn't write DataFrame to s3://{self._bucket}/{key}"
            )
