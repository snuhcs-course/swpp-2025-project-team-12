from django.urls import path
from .password import PasswordView
from .profile import ProfileView
from .name import NameView

urlpatterns = [
    path('password', PasswordView.as_view({
        'put': 'password'
    }), name='password'),

    path('profile', ProfileView.as_view({
        'get': 'profile',
        'put': 'profile',
        'post': 'profile'
    }), name='profile'),

    path('name', NameView.as_view({
        'get': 'name',
        'post': 'name'
    }), name='name'),
]