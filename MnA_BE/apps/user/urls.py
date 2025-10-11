from django.urls import path, include
from . import views
from .info import urlpatterns as info_urls

urlpatterns = [
    path('login', views.login, name='login'),
    path('logout', views.logout, name='logout'),
    path('signup', views.signup, name='signup'),
    path('withdraw', views.withdraw, name='withdraw'),

    path('info/', include(info_urls)),
]