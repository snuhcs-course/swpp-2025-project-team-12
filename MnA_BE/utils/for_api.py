import pytz, datetime as _dt
from django.http import JsonResponse
from utils.time import iso_now
import re

KST = pytz.timezone("Asia/Seoul")

def market_date_kst():
    return _dt.datetime.now(KST).strftime("%Y-%m-%d")

def get_path_with_date(content, year, month, day):
    if re.match(r"^\d{4}$", str(year)) is None:
        raise ValueError("Year must be in YYYY format")
    if re.match(r"^(0[1-9]|1[0-2])$", str(month)) is None:
        raise ValueError("Month must be in MM format")
    if re.match(r"^(0[1-9]|[12][0-9]|3[01])$", str(day)) is None:
        raise ValueError("Day must be in DD format")

    return f"year={year}/month={month}/content={content}/{year}-{month}-{day}"

def ok(payload: dict, status=200, **meta):
    if "asOf" not in payload:
        payload["asOf"] = iso_now()
    if meta:
        payload.update(meta)
    return JsonResponse(payload, status=status, json_dumps_params={"ensure_ascii": False})

def degraded(msg: str, source="s3", status=200, **extra):
    return ok({"degraded": True, "error": str(msg)[:200], "source": source}, status=status, **extra)
