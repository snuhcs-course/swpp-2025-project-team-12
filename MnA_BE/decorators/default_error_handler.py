from django.http import JsonResponse
from utils.debug_print import debug_print
import traceback

def default_error_handler(function):
    """
    ensure server never stopped by unexpected errors
    """
    def wrapper(*args, **kwargs):
        try:
            return function(*args, **kwargs)
        except Exception as e:
            debug_print(traceback.format_exc())

            return JsonResponse({
                "message": "INTERNAL ERROR"
            }, status=500)

    return wrapper
