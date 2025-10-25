# apps/api/urls.py
from django.urls import path, include
from . import views
from .articles.urls import urlpatterns as  articles_url
from .recommendations.urls import urlpatterns as recommendations_url

urlpatterns = [
    path("health", views.health, name="health"),
    path("indices", views.indices, name="indices"),
    path("company-profiles", views.company_profiles, name="company_profiles"),
    path("reports/<str:symbol>", views.reports_detail, name="reports_detail"),

    path("articles/", include(articles_url)),

    path("recommendations/", include(recommendations_url))
]