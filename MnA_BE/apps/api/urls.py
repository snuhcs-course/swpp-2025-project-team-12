# apps/api/urls.py
from django.urls import path, include
from .views import APIView
from .articles.urls import urlpatterns as  articles_url
from .recommendations.urls import urlpatterns as recommendations_url

urlpatterns = [
    path("health", APIView.as_view({
        'get': 'get_health'
    }), name="health"),
    path("indices", APIView.as_view({
        'get': 'get_indices'
    }), name="indices"),
    path("company-profiles", APIView.as_view({
        'get': 'get_company_profiles'
    }), name="company_profiles"),
    path("reports/<str:symbol>", APIView.as_view({
        'get': 'get_reports_detail'
    }), name="reports_detail"),

    path("articles/", include(articles_url)),

    path("recommendations/", include(recommendations_url))
]