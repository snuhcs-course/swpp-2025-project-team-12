from django.urls import path
from .root import style_view
from .page import get_page

urlpatterns = [
    path('', style_view, name='style'),
    path('page/<int:page_index>', get_page, name='page'),
]