from django.urls import path
from .top import articles_top

urlpatterns = [
    path('top', articles_top , name="articles_top")
]