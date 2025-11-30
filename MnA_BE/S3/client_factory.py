import traceback
from typing import Iterable, Optional

import boto3

from S3 import _get_env, debug_print


def _get_first_env(*keys: str) -> Optional[str]:
    """
    내부 헬퍼: _get_env 를 그대로 래핑 (가독성용).
    """
    return _get_env(*keys)


class S3ClientFactory:
    """
    S3 Client 생성을 담당하는 Factory.

    - from_env: 기본(공통) S3 클라이언트
    - for_finance: Finance 전용 S3 클라이언트
    """

    @classmethod
    def from_env(
        cls,
        access_key_envs: Iterable[str] = ("IAM_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID"),
        secret_key_envs: Iterable[str] = ("IAM_SECRET_KEY", "AWS_SECRET_ACCESS_KEY"),
        region_envs: Iterable[str] = ("AWS_REGION",),
    ):
        """
        주어진 ENV 키 후보들에서 자격증명/리전을 읽어 S3 Client 생성.
        """
        access_key = _get_first_env(*access_key_envs)
        secret_key = _get_first_env(*secret_key_envs)
        region = _get_first_env(*region_envs)

        kwargs = {}
        if region:
            kwargs["region_name"] = region
        if access_key and secret_key:
            kwargs["aws_access_key_id"] = access_key
            kwargs["aws_secret_access_key"] = secret_key

        try:
            return boto3.client("s3", **kwargs)
        except Exception:
            # 공통 디버그 출력
            debug_print(traceback.format_exc())
            raise

    @classmethod
    def for_finance(cls):
        """
        Finance 전용 ENV 세트를 사용해 S3 Client 생성.
        """
        return cls.from_env(
            access_key_envs=("FINANCE_IAM_ACCESS_KEY_ID", "FINANCE_AWS_ACCESS_KEY_ID"),
            secret_key_envs=(
                "FINANCE_IAM_SECRET_KEY",
                "FINANCE_AWS_SECRET_ACCESS_KEY",
            ),
            # 필요시 "FINANCE_AWS_REGION" 같은 걸 먼저 검색하고 싶다면 추가 가능
            region_envs=("FINANCE_AWS_REGION", "AWS_REGION"),
        )
