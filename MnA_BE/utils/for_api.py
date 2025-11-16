import pytz, datetime as _dt
from django.http import JsonResponse
from utils.time import iso_now
import re

KST = pytz.timezone("Asia/Seoul")

def market_date_kst():
    return _dt.datetime.now(KST).strftime("%Y-%m-%d")

def get_path_with_date(content, year, month, day):
    # Convert to string and pad with zeros if needed
    year_str = str(year)
    month_str = str(month).zfill(2)
    day_str = str(day).zfill(2)
    
    if re.match(r"^\d{4}$", year_str) is None:
        raise ValueError("Year must be in YYYY format")
    if re.match(r"^(0[1-9]|1[0-2])$", month_str) is None:
        raise ValueError("Month must be in MM format")
    if re.match(r"^(0[1-9]|[12][0-9]|3[01])$", day_str) is None:
        raise ValueError("Day must be in DD format")

    return f"year={year_str}/month={month_str}/content={content}/{year_str}-{month_str}-{day_str}"

def ok(payload: dict, status=200, **meta):
    if "asOf" not in payload:
        payload["asOf"] = iso_now()
    if meta:
        payload.update(meta)
    return JsonResponse(payload, status=status, json_dumps_params={"ensure_ascii": False})

def degraded(msg: str, source="s3", status=200, **extra):
    return ok({"degraded": True, "error": str(msg)[:200], "source": source}, status=status, **extra)