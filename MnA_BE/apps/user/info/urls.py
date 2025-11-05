from django.urls import path
from .password import PasswordView
from .profile import ProfileView
from .name import NameView

urlpatterns = [
    path('password', PasswordView.as_view({
        'put': 'put'
    }), name='password'),

    path('profile', ProfileView.as_view({
        'get': 'get',
        'post': 'post',
        'delete': 'delete'
    }), name='profile'),

    path('name', NameView.as_view({
        'get': 'get',
        'post': 'post'
    }), name='name'),
]