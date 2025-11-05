from django.urls import path
from .root import StyleView
from .page import StylePageView

urlpatterns = [
    path('', StyleView.as_view({
        'get': 'get',
        'post': 'post'
    }), name='style'),
    path('page/<int:page_index>', StylePageView.as_view({
        'get': 'get_page'
    }), name='style_page'),
]