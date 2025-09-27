from django.http import JsonResponse
import json

def hello(request):
    return JsonResponse({"message": "Hello, world!"})