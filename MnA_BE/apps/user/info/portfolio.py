from django.http import JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action
from decorators import *
import json

class PortfolioView(viewsets.ViewSet):

    @action(detail=True, methods=['get'])
    @default_error_handler
    @require_auth
    def get(self, request, user):
        """
        GET: get user's portfolio. (check user by cookie)
        """

        if request.method == "GET":
            ### GET ###

            return JsonResponse({"portfolio": user.portfolio}, status=200)

    @action(detail=True, methods=['post'])
    @default_error_handler
    @require_auth
    def post(self, request, user):
        """
        POST: change user's portfolio.
        """
        body = json.loads(request.body.decode('utf-8'))
        portfolio = body.get("portfolio")

        if portfolio is None:
            return JsonResponse({"message": "CANNOT FIND INPUT"}, status=400)

        # TODO: stock name validation

        try:
            user.portfolio = portfolio
            user.save()
        except Exception as e:
            return JsonResponse({"message": "PORTFOLIO SAVE FAILED"}, status=500)

        return JsonResponse({"message": "PORTFOLIO UPDATE SUCCESS"}, status=200)