from django.urls import path
from apps.MarketIndex import views

app_name = 'marketindex'

urlpatterns = [
    # Get latest prices for all indices
    # GET /marketindex/stockindex/latest/
    path('stockindex/latest/', views.stockindex_latest, name='latest'),
    
    # Get historical data for a specific index or both
    # GET /marketindex/stockindex/history/<str:index_type>/?days=30
    # where index_type can be: KOSPI, KOSDAQ, or BOTH
    path('stockindex/history/<str:index_type>/', views.stockindex_history, name='history'),
    
    # Get summary statistics for all indices
    # GET /marketindex/stockindex/summary/
    path('stockindex/summary/', views.stockindex_summary, name='summary'),
]