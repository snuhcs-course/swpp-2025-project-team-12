from django.urls import path
from . import views

urlpatterns = [
    path("", views.get_articles, name="articles-list"),
    path("<str:date>/", views.get_articles_by_date, name="articles-by-date"),
    path("detail/<int:id>/", views.get_article_detail, name="articles-detail"),
]
