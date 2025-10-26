import pytz, datetime as _dt
from django.http import JsonResponse
from utils.time import iso_now

KST = pytz.timezone("Asia/Seoul")

def market_date_kst():
    return _dt.datetime.now(KST).strftime("%Y-%m-%d")

def get_path_with_date(content):
    cur_dt = _dt.datetime.now(KST)
    year    = cur_dt.year
    month   = cur_dt.month
    day     = cur_dt.day

    return f"year={year}/month={month}/content={content}/{year}-{month}-{day}"

def ok(payload: dict, status=200, **meta):
    if "asOf" not in payload:
        payload["asOf"] = iso_now()
    if meta:
        payload.update(meta)
    return JsonResponse(payload, status=status, json_dumps_params={"ensure_ascii": False})

def degraded(msg: str, source="s3", status=200, **extra):
    return ok({"degraded": True, "error": str(msg)[:200], "source": source}, status=status, **extra)
