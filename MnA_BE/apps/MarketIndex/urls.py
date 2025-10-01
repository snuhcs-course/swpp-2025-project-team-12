from django.urls import path
from apps.MarketIndex import views

app_name = 'marketindex'

urlpatterns = [
    # Get latest prices for all indices
    # GET /marketindex/stockindex/latest/
    path('stockindex/latest/', views.stockindex_latest, name='latest'),
    
    # Get historical data for a specific index (e.g., KOSPI, KOSDAQ)
    # GET /marketindex/stockindex/KOSPI/history/?days=30
    path('stockindex/<str:index_type>/history/', views.stockindex_history, name='history'),
    
    # Compare all indices over a time period
    # GET /marketindex/stockindex/compare/?days=7
    path('stockindex/compare/', views.stockindex_compare, name='compare'),
    
    # Get summary statistics for all indices
    # GET /marketindex/stockindex/summary/
    path('stockindex/summary/', views.stockindex_summary, name='summary'),
]