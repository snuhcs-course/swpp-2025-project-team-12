# apps/articles/views.py
from django.http import JsonResponse, HttpResponseBadRequest
from rest_framework import viewsets
from rest_framework.decorators import action
from .services import list_articles, get_article_by_id


class ArticleView(viewsets.ViewSet):
    """
    Article Views
    ---
    Provides endpoints to retrieve articles, including listing all articles,
    """

    @action(detail=False, methods=['get'])
    def get(self, request):
        data = list_articles(None)
        return JsonResponse({"data": data}, status=200)

    @action(detail=False, methods=['get'])
    def get_by_date(self, request, date):
        try:
            data = list_articles(date)
        except ValueError:
            return HttpResponseBadRequest("Invalid date format, expected YYYY-MM-DD")
        return JsonResponse({"date": date, "data": data}, status=200)

    @action(detail=False, methods=['get'])
    def get_detail(self, request, id):
        # (선택) ?date=YYYY-MM-DD 허용: 특정 날짜에서 찾고 싶을 때
        date = request.GET.get("date")
        doc = get_article_by_id(id, date)
        if not doc:
            return JsonResponse({"message": "Not found"}, status=404)
        return JsonResponse(doc, status=200)
