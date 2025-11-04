from django.http import JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action
from decorators import *
from apps.user.models import Style
from django.core.paginator import Paginator
import json

PAGE_SIZE = 10

class StylePageView(viewsets.ViewSet):
    """
    ViewSet for handling paginated retrieval of user interests history.
    """

    @action(detail=False, methods=['get'])
    @default_error_handler
    @require_auth
    def get_page(self, request, page_index, user):
        """
        GET: return interests history by page. page index is start from 1.
        """

        if request.method == "GET":
            ### GET ###

            try:
                style_list = user.style_set.all()
                paginator = Paginator(style_list, PAGE_SIZE)
                style_page = [
                    {
                        "interests": entry.interests,
                        "strategy": entry.strategy,
                        "create_at": entry.create_at
                    } for entry in paginator.page(page_index).object_list
                ]

            except Exception as e:
                style_page = []

            return JsonResponse({"style_page": style_page}, status=200)

        else:
            return JsonResponse({"message": "NOT ALLOWED METHOD"}, status=405)