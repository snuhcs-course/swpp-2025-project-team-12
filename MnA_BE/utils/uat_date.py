# utils/uat_date.py
"""
UAT(User Acceptance Testing)용 날짜 고정 유틸리티

사용법:
    1. 환경변수 설정: export UAT_DATE=2025-10-16
    2. 모든 S3 데이터가 해당 날짜 기준으로 반환됨
    3. 환경변수 제거하면 정상 운영 모드로 복귀
"""

import os
from datetime import datetime


def get_uat_date() -> str | None:
    """
    UAT 모드면 고정 날짜 반환, 아니면 None
    
    Returns:
        str | None: "2025-10-16" 형태의 날짜 문자열 또는 None
    """
    return os.getenv("UAT_DATE", None)


def is_uat_mode() -> bool:
    """UAT 모드 여부 확인"""
    return get_uat_date() is not None


def parse_date_from_key(key: str) -> str | None:
    """
    S3 key에서 날짜 추출
    
    예시:
        - "llm_output/company-overview/year=2025/month=10/2025-10-16.json" -> "2025-10-16"
        - "price-financial-info-instant/year=2025/month=10/day=16/data.parquet" -> "2025-10-16"
    """
    # 파일명에서 YYYY-MM-DD 패턴 찾기
    if "/" in key:
        filename = key.split("/")[-1]
        if filename.count("-") >= 2:
            date_part = filename.split(".")[0]
            # YYYY-MM-DD 형식 검증
            try:
                datetime.strptime(date_part, "%Y-%m-%d")
                return date_part
            except ValueError:
                pass
    
    # year=YYYY/month=MM/day=DD 패턴에서 추출
    year = month = day = None
    for part in key.split("/"):
        if part.startswith("year="):
            year = part.split("=")[1]
        elif part.startswith("month="):
            month = part.split("=")[1]
        elif part.startswith("day="):
            day = part.split("=")[1]
    
    if year and month and day:
        return f"{year}-{month.zfill(2)}-{day.zfill(2)}"
    
    return None


def matches_uat_date(key: str, uat_date: str) -> bool:
    """
    S3 key가 UAT 날짜와 정확히 일치하는지 확인
    
    지원하는 S3 key 패턴:
    1. 파일명에 날짜 포함: .../2025-11-26.parquet
    2. year/month/day 분리: .../year=2025/month=11/day=26/KOSPI.json
    
    Args:
        key: S3 object key
        uat_date: "2025-10-16" 형태의 날짜
    
    Returns:
        bool: 날짜 일치 여부
    """
    year, month, day = uat_date.split("-")
    
    # 방법 1: 파일명에 날짜가 직접 포함 (instant, llm_output, company-profile)
    # 예: price-financial-info-instant/year=2025/month=11/2025-11-26.parquet
    if uat_date in key:
        return True
    
    # 방법 2: year=/month=/day= 패턴 (stock-indices)
    # 예: stock-indices/year=2024/month=10/day=16/KOSPI.json
    # day가 앞자리 0 없이 저장될 수 있음 (day=1, day=16 등)
    # 주의: day=2가 day=27에 부분 매칭되지 않도록 뒤에 / 추가
    patterns = [
        f"year={year}/month={month}/day={day}/",           # day=01/
        f"year={year}/month={month}/day={int(day)}/",      # day=1/
        f"year={year}/month={int(month)}/day={day}/",      # month=1/day=01/
        f"year={year}/month={int(month)}/day={int(day)}/", # month=1/day=1/
    ]
    
    return any(pattern in key for pattern in patterns)


def extract_date_from_key(key: str) -> str | None:
    """
    S3 key에서 날짜 추출
    
    Args:
        key: S3 object key
    
    Returns:
        str | None: "2025-10-16" 형태의 날짜 또는 None
    """
    # 방법 1: 파일명에서 YYYY-MM-DD 추출
    if "/" in key:
        filename = key.split("/")[-1]
        if filename.count("-") >= 2:
            date_part = filename.split(".")[0]
            try:
                datetime.strptime(date_part, "%Y-%m-%d")
                return date_part
            except ValueError:
                pass
    
    # 방법 2: year=/month=/day= 패턴에서 추출
    year = month = day = None
    for part in key.split("/"):
        if part.startswith("year="):
            year = part.split("=")[1]
        elif part.startswith("month="):
            month = part.split("=")[1]
        elif part.startswith("day="):
            day = part.split("=")[1]
    
    if year and month and day:
        return f"{year}-{month.zfill(2)}-{day.zfill(2)}"
    
    return None


def is_date_on_or_before(key: str, uat_date: str) -> bool:
    """
    S3 key의 날짜가 UAT 날짜 이하인지 확인
    (company-profile처럼 월초에만 생성되는 데이터용)
    
    Args:
        key: S3 object key
        uat_date: "2025-10-16" 형태의 날짜
    
    Returns:
        bool: key의 날짜 <= uat_date 이면 True
    """
    key_date = extract_date_from_key(key)
    if not key_date:
        return False
    
    return key_date <= uat_date