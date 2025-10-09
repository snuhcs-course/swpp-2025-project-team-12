from django.http import JsonResponse
import traceback
import os

def default_error_handler(function):
    def wrapper(*args, **kwargs):
        try:
            return function(*args, **kwargs)
        except Exception as e:
            if os.getenv("DEBUG") == "True":
                print(traceback.format_exc())

            return JsonResponse({
                "message": "INTERNAL ERROR"
            }, status=500)

    return wrapper
