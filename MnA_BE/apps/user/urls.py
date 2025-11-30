from django.urls import path, include
from .views import UserView
from .info import urlpatterns as info_urls
from .style import urlpatterns as style_urls


urlpatterns = [
    path("login", UserView.as_view({"post": "login"}), name="login"),
    path("logout", UserView.as_view({"post": "logout"}), name="logout"),
    path("signup", UserView.as_view({"post": "signup"}), name="signup"),
    path("withdraw", UserView.as_view({"delete": "withdraw"}), name="withdraw"),
    path("info/", include(info_urls)),
    path("style/", include(style_urls)),
]
