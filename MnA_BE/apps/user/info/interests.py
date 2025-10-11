from django.http import JsonResponse
from decorators import *
from apps.user.models import Interests
from django.core.paginator import Paginator
import json

PAGE_SIZE = 10

@default_error_handler
@require_auth
def interests_view(request, user):
    """
    POST: create interests change history
    """

    if request.method == 'POST':
        ### POST ###

        body = json.loads(request.body.decode('utf-8'))
        interests = body.get('interests')

        if interests is None:
            return JsonResponse({ "message": "INTERESTS REQUIRED" }, status=400)

        try:
            new_interests = Interests.objects.create(
                user=user,
                interests=interests
            )
            new_interests.save()
        except Exception as e:
            return JsonResponse({ "message": "SAVE INTERESTS FAILED" }, status=500)

        return JsonResponse({ "message": "INTERESTS UPDATE SUCCESS" }, status=200)

    else:
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)


@default_error_handler
@require_auth
def get_recent(request, user):
    """
    GET: return most recent interests
    """

    if request.method == "GET":
        ### GET ###

        try:
            interests_row = user.interests_set.all()[0]
            interests = {
                "interests": interests_row.interests,
                "create_at": interests_row.create_at
            }
        except Exception as e:
            interests = dict()

        return JsonResponse({ "interests": interests }, status=200)

    else:
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)


@default_error_handler
@require_auth
def get_page(request, page_index, user):
    """
    GET: return interests history by page. page index is start from 1.
    """

    if request.method == "GET":
        ### GET ###

        try:
            interests_list = user.interests_set.all()
            paginator = Paginator(interests_list, PAGE_SIZE)
            interests_page = [
                {
                    "interests": entry.interests,
                    "create_at": entry.create_at
                } for entry in paginator.page(page_index).object_list
            ]

        except :
            interests_page = []

        return JsonResponse({ "interests_page": interests_page }, status=200)

    else:
        return JsonResponse({ "message": "NOT ALLOWED METHOD" }, status=405)