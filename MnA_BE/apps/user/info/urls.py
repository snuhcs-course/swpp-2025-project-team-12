from django.urls import path
from .profile import profile_view
from .name import name_view

urlpatterns = [
    path('profile', profile_view, name='profile'),
    path('name', name_view, name='name'),
]