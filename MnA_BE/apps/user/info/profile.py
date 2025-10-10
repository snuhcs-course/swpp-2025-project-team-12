from django.http import JsonResponse
from decorators import *
from S3 import S3
import json

@default_error_handler
@require_auth
def profile_view(request, user):
    if request.method == 'GET':
        pass

    elif request.method == 'POST':
        pass


    else:
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)