from django.http import JsonResponse
from decorators import *

@default_error_handler
@require_auth
def interests_view(request, user):
    if request.method == 'GET':
        pass

    elif request.method == 'POST':
        pass

    else:
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)