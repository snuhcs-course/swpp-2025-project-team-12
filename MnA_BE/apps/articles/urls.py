from django.urls import path
from .views import ArticleView

urlpatterns = [
    path("", ArticleView.as_view({ 'get': 'get_articles'}), name="articles-list"),
    path("<str:date>/", ArticleView.as_view({ 'get': 'get_articles_by_date'}), name="articles-by-date"),
    path("detail/<int:id>/", ArticleView.as_view({ 'get': 'get_article_detail'}), name="articles-detail"),
]
