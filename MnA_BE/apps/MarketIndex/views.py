from django.http import JsonResponse
from .korean_stock_indices_manager import KoreanStockIndicesManager

def stockindex_latest(request):
    """Get the latest closing price for each index from the JSON data."""
    manager = KoreanStockIndicesManager()
    latest_data = manager.get_latest()

    # The manager's get_latest() already returns the data in a perfect format.
    # We just need to wrap it in our standard API response structure.
    return JsonResponse({
        'status': 'success',
        'data': latest_data
    })


def stockindex_history(request, index_type):
    """
    Get historical data for a specific index from the JSON data.
    Example: /marketindex/stockindex/KOSPI/history/?days=30
    """
    manager = KoreanStockIndicesManager()

    # Validate index type against the manager's list
    valid_indices = manager.indices.keys()
    if index_type not in valid_indices:
        return JsonResponse({
            'status': 'error',
            'message': f'Invalid index. Choose from: {", ".join(valid_indices)}'
        }, status=400)

    # Get number of days from query params
    try:
        days = int(request.GET.get('days', 30))
        days = min(max(1, days), 365)  # Clamp between 1 and 365
    except (ValueError, TypeError):
        days = 30

    # Get historical data from the manager
    history = manager.get_history(index_type, days=days)

    # The manager's data keys need a slight rename to match the original API output
    # ('close' -> 'price', 'change_amount' -> 'change')
    data = [{
        'date': record['date'],
        'price': record['close'],
        'change': record['change_amount'],
        'change_percent': record['change_percent']
    } for record in history]

    return JsonResponse({
        'status': 'success',
        'index': index_type,
        'days': days,
        'count': len(data),
        'data': data
    })


def stockindex_compare(request):
    """
    Compare all indices over a given period using data from the JSON files.
    Example: /marketindex/stockindex/compare/?days=7
    """
    manager = KoreanStockIndicesManager()
    
    try:
        days = int(request.GET.get('days', 7))
        days = min(max(1, days), 365)
    except (ValueError, TypeError):
        days = 7

    comparison_data = {}

    # Fetch history for each index defined in the manager
    for index_type in manager.indices.keys():
        records = manager.get_history(index_type, days=days)
        
        # Format data for the comparison response
        comparison_data[index_type] = [{
            'date': r['date'],
            'price': r['close'],
            'change_percent': r['change_percent']
        } for r in records]

    return JsonResponse({
        'status': 'success',
        'days': days,
        'data': comparison_data
    })


def stockindex_summary(request):
    """Get summary statistics for all indices from the JSON data."""
    manager = KoreanStockIndicesManager()
    summary_data = manager.get_summary() # This method does all the work!

    return JsonResponse({
        'status': 'success',
        'data': summary_data
    })