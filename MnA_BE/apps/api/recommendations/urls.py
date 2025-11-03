from django.urls import path
from .general import GeneralRecommendationsView
from .personalized import PersonalizedRecommendationsView

urlpatterns = [
    path('general', GeneralRecommendationsView.as_view({
        'get': 'recommendations_general'
    }), name="reco_general"),
    path('personalized', PersonalizedRecommendationsView.as_view({
        'get': 'recommendations_personalized'
    }), name="reco_personalized"),
]