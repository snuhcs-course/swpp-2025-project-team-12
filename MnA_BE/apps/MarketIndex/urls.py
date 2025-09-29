from django.urls import path
from apps.MarketIndex import views

app_name = 'marketindex'

urlpatterns = [
    # Stock Index endpoints
    path('stockindex/', views.stockindex_list, name='stockindex-list'),
    path('stockindex/latest/', views.stockindex_latest, name='stockindex-latest'),
    path('stockindex/fetch/', views.stockindex_fetch, name='stockindex-fetch'),
]