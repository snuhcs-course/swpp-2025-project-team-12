# apps/articles/services.py
from __future__ import annotations
import os, json, datetime, hashlib, typing as t
import boto3
from botocore.exceptions import BotoCoreError, ClientError

LOCAL_BASE = os.getenv("ARTICLES_LOCAL_BASE", "articles")  # e.g. articles/20251025/...
BUCKET = os.getenv("ARTICLES_S3_BUCKET", "swpp-12-bucket")
REGION = os.getenv("ARTICLES_S3_REGION", "ap-northeast-2")
PREFIX = os.getenv("ARTICLES_S3_PREFIX", "news-articles")

def _yyyymmdd(d: datetime.date) -> str:
    return d.strftime("%Y%m%d")

def _local_path(d: datetime.date) -> str:
    return os.path.join(LOCAL_BASE, _yyyymmdd(d), "business_top50.json")

def _s3_key(d: datetime.date) -> str:
    return f"{PREFIX}/year={d.year}/month={d.month}/day={d.day}/business_top50.json"

def _hash_id(url: str) -> str:
    # url 기반 안정적 id (상세 조회용 키)
    return hashlib.sha1(url.encode("utf-8")).hexdigest()[:16]

def _load_payload(d: datetime.date) -> dict:
    # 1) 로컬 파일 우선
    p = _local_path(d)
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            return json.load(f)
    # 2) S3 폴백
    s3 = boto3.client("s3", region_name=REGION)
    obj = s3.get_object(Bucket=BUCKET, Key=_s3_key(d))
    return json.load(obj["Body"])

def list_articles(date_str: t.Optional[str] = None) -> list[dict]:
    # 오늘 또는 지정 날짜의 기사 목록(+id 주입)
    d = datetime.date.today() if not date_str else datetime.datetime.strptime(date_str, "%Y-%m-%d").date()
    payload = _load_payload(d)
    arts = payload.get("articles", [])
    # 목록에 detail 호출용 id를 주입
    out = []
    for a in arts:
        a2 = dict(a)
        a2["id"] = _hash_id(a["url"])
        out.append(a2)
    return out

def get_article_by_id(article_id: str, date_str: t.Optional[str] = None) -> t.Optional[dict]:
    # 해당 날짜(없으면 오늘)에서 id가 일치하는 기사 하나 반환
    d = datetime.date.today() if not date_str else datetime.datetime.strptime(date_str, "%Y-%m-%d").date()
    payload = _load_payload(d)
    for a in payload.get("articles", []):
        if _hash_id(a["url"]) == article_id:
            # 상세에 id 포함해서 돌려주기
            a2 = dict(a)
            a2["id"] = article_id
            a2["date"] = d.strftime("%Y-%m-%d")
            return a2
    return None
