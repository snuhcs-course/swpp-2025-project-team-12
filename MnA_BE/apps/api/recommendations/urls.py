from django.urls import path
from .general import recommendations_general
from .personalized import recommendations_personalized

urlpatterns = [
    path('general', recommendations_general, name="reco_general"),
    path('personalized', recommendations_personalized, name="reco_personalized"),
]