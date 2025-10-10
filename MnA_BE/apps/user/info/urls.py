from django.urls import path
from .interests import interests_view
from .profile import profile_view
from .name import name_view

urlpatterns = [
    path('interests/', interests_view, name='interests'),
    path('profile/', profile_view, name='profile'),
    path('name/', name_view, name='name'),
]