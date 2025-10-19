from django.urls import path
from .password import password_view
from .profile import profile_view
from .name import name_view

urlpatterns = [
    path('password', password_view, name='password'),
    path('profile', profile_view, name='profile'),
    path('name', name_view, name='name'),
]