from django.urls import path
from .top import TopArticleView

urlpatterns = [path("top", TopArticleView.as_view({"get": "get_top"}), name="articles_top")]
