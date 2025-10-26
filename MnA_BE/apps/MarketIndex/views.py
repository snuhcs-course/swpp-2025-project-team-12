import os
from django.http import JsonResponse
from .stockindex_manager import StockindexManager
from S3.finance import FinanceS3Client
from utils.for_api import get_path_with_date

def stockindex_latest(request):
    """Get the latest closing price for each index from the JSON data."""
    manager = StockindexManager()
    latest_data = manager.get_latest()

    llm_output = FinanceS3Client().get_json(
        bucket=os.environ.get('FINANCE_BUCKET_NAME'),
        key=f"llm_output/{get_path_with_date('index_info')}"
    )

    # The manager's get_latest() already returns the data in a perfect format.
    # We just need to wrap it in our standard API response structure.
    return JsonResponse({
        'status'    : 'success',
        'data'      : latest_data,
        'llm_output': llm_output
    })


def stockindex_history(request, index_type):
    """
    Get historical data for a specific index or both from the JSON data.
    Example: 
    - /marketindex/stockindex/history/KOSPI/?days=30
    - /marketindex/stockindex/history/KOSDAQ/?days=30
    - /marketindex/stockindex/history/BOTH/?days=30
    """
    manager = StockindexManager()

    # Get number of days from query params
    try:
        days = int(request.GET.get('days', 30))
        days = min(max(1, days), 365)  # Clamp between 1 and 365
    except (ValueError, TypeError):
        days = 30

    # Handle BOTH case
    if index_type.upper() == 'BOTH':
        both_data = {}
        
        for idx_name in manager.indices.keys():
            history = manager.get_history(idx_name, days=days)
            
            # Keep original field names from the manager
            formatted_history = [{
                'date': record['date'],
                'close': record['close'],
                'change_amount': record['change_amount'],
                'change_percent': record['change_percent']
            } for record in history]
            
            both_data[idx_name] = formatted_history
        
        return JsonResponse({
            'status': 'success',
            'index': 'BOTH',
            'days': days,
            'data': both_data
        })
    
    # Handle single index case
    else:
        # Validate index type against the manager's list
        valid_indices = list(manager.indices.keys())
        if index_type not in valid_indices:
            return JsonResponse({
                'status': 'error',
                'message': f'Invalid index. Choose from: {", ".join(valid_indices + ["BOTH"])}'
            }, status=400)

        # Get historical data from the manager
        history = manager.get_history(index_type, days=days)

        # Keep original field names from the manager
        data = [{
            'date': record['date'],
            'close': record['close'],
            'change_amount': record['change_amount'],
            'change_percent': record['change_percent']
        } for record in history]

        return JsonResponse({
            'status': 'success',
            'index': index_type,
            'days': days,
            'count': len(data),
            'data': data
        })


def stockindex_summary(request):
    """Get summary statistics for all indices from the JSON data."""
    manager = StockindexManager()
    summary_data = manager.get_summary()
    
    # Also get latest data to extract change_amount
    latest_data = manager.get_latest()
    
    # Format the data to use consistent naming
    formatted_summary = {}
    for index_name, data in summary_data.items():
        formatted_summary[index_name] = {
            'latest_close': data['latest_price'],
            'latest_change_amount': latest_data[index_name]['change_amount'] if index_name in latest_data else None,
            'latest_change_percent': data['latest_change'],
            'latest_date': data['latest_date'],
            'latest_volume': data['latest_volume'],
            '30d_high': data['30d_high'],
            '30d_low': data['30d_low'],
            '30d_avg': data['30d_avg'],
            '52w_high': data['52w_high'],
            '52w_low': data['52w_low'],
            'data_points': data['data_points']
        }

    return JsonResponse({
        'status': 'success',
        'data': formatted_summary
    })