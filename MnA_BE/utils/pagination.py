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
