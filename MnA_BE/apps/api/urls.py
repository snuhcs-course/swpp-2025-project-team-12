# apps/api/urls.py
from django.urls import path
from . import views

urlpatterns = [
    path("health", views.health, name="health"),
    path("indices", views.indices, name="indices"),
    path("articles/top", views.articles_top, name="articles_top"),
    path("company-profiles", views.company_profiles, name="company_profiles"),
    path("reports/<str:symbol>", views.reports_detail, name="reports_detail"),
    path("recommendations/general", views.recommendations_general, name="reco_general"),
    path("recommendations/personalized", views.recommendations_personalized, name="reco_personalized"),
    path("recommendations/general", views.recommendations_general, name="reco_general"),
]