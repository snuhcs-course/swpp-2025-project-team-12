# apps/articles/services.py
# 방법 2: URL의 int:id에 맞춰 services.py를 인덱스 기반으로 수정

from __future__ import annotations
import os, json, datetime, hashlib, typing as t
import boto3
from botocore.exceptions import BotoCoreError, ClientError

LOCAL_BASE = os.getenv("ARTICLES_LOCAL_BASE", "articles")
BUCKET = os.getenv("ARTICLES_S3_BUCKET", "swpp-12-bucket")
REGION = os.getenv("ARTICLES_S3_REGION", "ap-northeast-2")
PREFIX = os.getenv("ARTICLES_S3_PREFIX", "news-articles")

# S3 클라이언트를 모듈 레벨로 (모킹 가능)
s3 = boto3.client("s3", region_name=REGION)


def _yyyymmdd(d: datetime.date) -> str:
    return d.strftime("%Y%m%d")

def _local_path(d: datetime.date) -> str:
    return os.path.join(LOCAL_BASE, _yyyymmdd(d), "business_top50.json")

def _s3_key(d: datetime.date) -> str:
    return f"{PREFIX}/year={d.year}/month={d.month}/day={d.day}/business_top50.json"

def _load_payload(d: datetime.date) -> dict:
    # 1) 로컬 파일 우선
    p = _local_path(d)
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            return json.load(f)
    # 2) S3 폴백
    obj = s3.get_object(Bucket=BUCKET, Key=_s3_key(d))
    return json.load(obj["Body"])

def list_articles(date_str: t.Optional[str] = None) -> list[dict]:
    """기사 목록 - 인덱스 기반 ID 사용"""
    d = datetime.date.today() if not date_str else datetime.datetime.strptime(date_str, "%Y-%m-%d").date()
    payload = _load_payload(d)
    arts = payload.get("articles", [])
    
    # 인덱스를 ID로 사용
    out = []
    for idx, a in enumerate(arts):
        a2 = dict(a)
        a2["id"] = idx  # 0, 1, 2, ...
        out.append(a2)
    return out

def get_article_by_id(article_id: int, date_str: t.Optional[str] = None) -> t.Optional[dict]:
    """특정 ID 기사 조회 - 인덱스 기반"""
    d = datetime.date.today() if not date_str else datetime.datetime.strptime(date_str, "%Y-%m-%d").date()
    payload = _load_payload(d)
    arts = payload.get("articles", [])
    
    # 인덱스로 직접 접근
    if 0 <= article_id < len(arts):
        a2 = dict(arts[article_id])
        a2["id"] = article_id
        a2["date"] = d.strftime("%Y-%m-%d")
        return a2
    
    return None