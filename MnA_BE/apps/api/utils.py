# apps/api/utils.py
from django.http import JsonResponse
from datetime import datetime, timezone

def iso_now():
    return datetime.now(timezone.utc).isoformat()

def get_pagination(request, default_limit=20, max_limit=100):
    try:
        limit = int(request.GET.get("limit", default_limit))
    except ValueError:
        limit = default_limit
    try:
        offset = int(request.GET.get("offset", 0))
    except ValueError:
        offset = 0
    limit = max(1, min(limit, max_limit))
    offset = max(0, offset)
    return limit, offset

def ok(payload: dict, status=200, **meta):
    if "asOf" not in payload:
        payload["asOf"] = iso_now()
    if meta:
        payload.update(meta)
    return JsonResponse(payload, status=status, json_dumps_params={"ensure_ascii": False})

def degraded(msg: str, source="s3", status=200, **extra):
    return ok({"degraded": True, "error": str(msg)[:200], "source": source}, status=status, **extra)
