from django.urls import path
from . import views

urlpatterns = [
    path('hello', views.hello, name='hello'),
    path('login', views.login, name='login'),
    path('logout', views.logout, name='logout'),
    path('signup', views.signup, name='signup'),
    path('withdraw', views.withdraw, name='withdraw'),
]