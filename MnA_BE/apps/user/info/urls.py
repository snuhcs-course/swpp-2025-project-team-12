from django.urls import path
from .interests import interests_view, get_recent, get_page
from .profile import profile_view
from .name import name_view

urlpatterns = [
    path('interests', interests_view, name='interests'),
    path('interests/recent', get_recent, name='get_recent'),
    path('interests/page/<int:page_index>', get_page, name='get_page'),

    path('profile', profile_view, name='profile'),
    path('name', name_view, name='name'),
]