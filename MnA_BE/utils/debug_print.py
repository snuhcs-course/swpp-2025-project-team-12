import os

def debug_print(*args, **kwargs):
    if os.environ.get("DEBUG") == "True":
        print(*args, **kwargs)