from django.urls import path
from .views import StockIndexView, MarketLLMview

app_name = 'marketindex'

urlpatterns = [
    # Get latest prices for all indices
    path('llm_summary', MarketLLMview.as_view({ 'get': 'market_llm_output' }), name='llm_summary_latest'),
    path('llm_summary/<str:year>/<str:month>/<str:day>', MarketLLMview.as_view({ 'get': 'market_llm_output' }), name='llm_summary'),
    # GET /marketindex/stockindex/latest/
    path('stockindex/latest/', StockIndexView.as_view({ 'get': 'stockindex_latest' }), name='latest'),
    
    # Get historical data for a specific index or both
    # GET /marketindex/stockindex/history/<str:index_type>/?days=30
    # where index_type can be: KOSPI, KOSDAQ, or BOTH
    path('stockindex/history/<str:index_type>/', StockIndexView.as_view({ 'get': 'stockindex_history' }), name='history'),
    
    # Get summary statistics for all indices
    # GET /marketindex/stockindex/summary/
    path('stockindex/summary/', StockIndexView.as_view({ 'get': 'stockindex_summary' }), name='summary'),
]