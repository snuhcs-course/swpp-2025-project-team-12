from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from apps.MarketIndex.models import StockIndex
from datetime import datetime, timedelta
from django.utils import timezone
import json

def stockindex_list(request):
    """Get stock index data with filtering"""
    
    # Get query parameters
    index_type = request.GET.get('type', 'all')  # all, KOSPI, KOSDAQ, SP500
    days = int(request.GET.get('days', 7))  # How many days back
    limit = int(request.GET.get('limit', 100))  # Max records
    
    # Calculate date range
    end_date = timezone.now()
    start_date = end_date - timedelta(days=days)
    
    # Build query
    if index_type == 'all':
        stocks = StockIndex.objects.filter(
            timestamp__gte=start_date
        ).order_by('-timestamp')[:limit]
    else:
        stocks = StockIndex.objects.filter(
            index_type=index_type.upper(),
            timestamp__gte=start_date
        ).order_by('-timestamp')[:limit]
    
    # Format data
    data = []
    for stock in stocks:
        data.append({
            'type': stock.index_type,
            'price': float(stock.close_price),
            'change_percent': float(stock.change_percent),
            'timestamp': stock.timestamp.isoformat()
        })
    
    return JsonResponse({
        'status': 'success',
        'count': len(data),
        'data': data
    })


def stockindex_latest(request):
    """Get only the most recent price for each index"""
    
    latest = {}
    
    for stock_type in ['KOSPI', 'KOSDAQ', 'SP500']:
        stock = StockIndex.objects.filter(
            index_type=stock_type
        ).order_by('-timestamp').first()
        
        if stock:
            latest[stock_type] = {
                'price': float(stock.close_price),
                'change_percent': float(stock.change_percent),
                'timestamp': stock.timestamp.isoformat()
            }
        else:
            latest[stock_type] = None
    
    return JsonResponse({
        'status': 'success',
        'data': latest
    })


@csrf_exempt
def stockindex_fetch(request):
    """Manually trigger stock fetching via API"""
    
    if request.method != 'POST':
        return JsonResponse({'status': 'error', 'message': 'Only POST allowed'}, status=405)
    
    try:
        from django.core.management import call_command
        from io import StringIO
        
        # Capture command output
        out = StringIO()
        call_command('fetch_stocks', stdout=out)
        output = out.getvalue()
        
        return JsonResponse({
            'status': 'success',
            'message': 'Stock fetch completed',
            'output': output
        })
    except Exception as e:
        return JsonResponse({
            'status': 'error',
            'message': str(e)
        }, status=500)