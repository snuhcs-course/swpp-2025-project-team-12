from django.urls import path
from .root import StyleView
from .page import StylePageView

urlpatterns = [
    path('', StyleView.as_view({
        'get': 'style',
        'post': 'style'
    }), name='style'),
    path('page/<int:page_index>', StylePageView({
        'get': 'get_page'
    }), name='style_page'),
]