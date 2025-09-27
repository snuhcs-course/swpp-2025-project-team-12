from django.http import JsonResponse
import json

def hello(request):
    return JsonResponse({"message": "Hello, world!"})

def login(request):
    return JsonResponse({"message": "NOT IMPLEMENTED"}, status=500)

def logout(request):
    return JsonResponse({"message": "NOT IMPLEMENTED"}, status=500)

def signup(request):
    return JsonResponse({"message": "NOT IMPLEMENTED"}, status=500)

def withdraw(request):
    return JsonResponse({"message": "NOT IMPLEMENTED"}, status=500)