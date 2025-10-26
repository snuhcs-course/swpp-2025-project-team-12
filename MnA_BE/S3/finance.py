import io
import traceback

import boto3
import pandas as pd

from datetime import timezone

from S3 import _get_env, debug_print
from S3.base import S3Client


class FinanceS3Client(S3Client):
    """
    Boto3 S3 래퍼: 기존 HEAD의 메서드들을 유지/보강.
    - ENV 키 지원: FINANCE_IAM_ACCESS_KEY_ID / FINANCE_AWS_ACCESS_KEY_ID
                    IAM_SECRET_KEY   / AWS_SECRET_ACCESS_KEY
                    AWS_REGION (선택)
    """

    def __init__(
        self,
        access_key = _get_env("FINANCE_IAM_ACCESS_KEY_ID", "FINANCE_AWS_ACCESS_KEY_ID"),
        secret_key = _get_env("FINANCE_IAM_SECRET_KEY", "FINANCE_AWS_SECRET_ACCESS_KEY")

    ):
        print(access_key, secret_key)
        super().__init__(access_key, secret_key)

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

    def get_latest_parquet_df(self, bucket, prefix):
        latest = self.get_latest_object(prefix)

        if not latest: return None, None

        data = self.get(bucket, latest["Key"])
        df = pd.read_parquet(io.BytesIO(data))
        ts = latest["LastModified"].astimezone(timezone.utc).isoformat()

        return df, ts


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
