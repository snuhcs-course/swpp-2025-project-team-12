from django.urls import path
from .general import GeneralRecommendationsView
from .personalized import PersonalizedRecommendationsView

urlpatterns = [
    path("general", GeneralRecommendationsView.as_view({"get": "get"}), name="reco_general"),
    path(
        "general/<str:year>/<str:month>/<str:day>",
        GeneralRecommendationsView.as_view({"get": "get"}),
        name="reco_general_date",
    ),
    path(
        "personalized",
        PersonalizedRecommendationsView.as_view({"get": "get"}),
        name="reco_personalized",
    ),
    path(
        "personalized/<str:year>/<str:month>/<str:day>",
        PersonalizedRecommendationsView.as_view({"get": "get"}),
        name="reco_personalized_date",
    ),
]
