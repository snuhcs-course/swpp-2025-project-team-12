from django.http import JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action
from decorators import *
from apps.user.models import Style
import json

class StyleView(viewsets.ViewSet):
    """
    ViewSet for handling user interests and strategy styles.
    """

    @action(detail=False, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        try:
            style_row = user.style_set.all()[0]

            # filtering style columns
            style = {
                "interests": style_row.interests,
                "strategy": style_row.strategy,
                "create_at": style_row.create_at
            }
        except Exception as e:
            style = None

        return JsonResponse({"style": style}, status=200)


    @action(detail=False, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        body = json.loads(request.body.decode('utf-8'))
        interests = body.get('interests')
        strategy = body.get('strategy')

        if interests is None:
            return JsonResponse({"message": "INTERESTS REQUIRED"}, status=400)

        if strategy is None:
            return JsonResponse({"message": "STRATEGY REQUIRED"}, status=400)

        try:
            new_style = Style.objects.create(
                user=user,
                interests=interests,
                strategy=strategy
            )
            new_style.save()
        except Exception as e:
            return JsonResponse({"message": "SAVE INTERESTS FAILED"}, status=500)

        return JsonResponse({"message": "INTERESTS UPDATE SUCCESS"}, status=200)

