from django.urls import path
from .password import PasswordView
from .profile import profile_view
from .name import NameView

urlpatterns = [
    path('password', PasswordView.as_view({ 'put': 'password'}), name='password'),

    path('profile', profile_view, name='profile'),

    path('name', NameView.as_view({
        'get': 'name',
        'post': 'name'
    }), name='name'),
]