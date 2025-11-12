import os
from django.http import JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action

from decorators import default_error_handler
from .stockindex_manager import StockindexManager
from S3.finance import FinanceS3Client
from utils.for_api import get_path_with_date

class MarketLLMview(viewsets.ViewSet):
    """
    Market LLM Views
    ---
    Provides endpoints to retrieve LLM-generated market analysis data.
    """

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_market_overview(self, request, year = None, month = None, day = None):
        """Get the latest LLM output for market analysis from S3 JSON data."""
        bucket_name=os.environ.get('FINANCE_BUCKET_NAME')

        # if no date provided, get the latest
        if year is None and month is None and day is None:
            source = FinanceS3Client().check_source(
                bucket=bucket_name,
                prefix="llm_output/market-index-overview"
            )
            if not source["ok"]: return JsonResponse({ "message": "No LLM output found" }, status=404)
            year, month, day = source["latest"].split("-")

        path = f"llm_output/market-index-overview/year={year}/month={month}/{year}-{month}-{day}"
        try:
            llm_output = FinanceS3Client().get_json(
                bucket=bucket_name,
                key=path
            )
        except Exception as e:
            return JsonResponse({ "message": "Unexpected Server Error" }, status=500)

        return JsonResponse({ "llm_output": llm_output }, status=200)

class StockIndexView(viewsets.ViewSet):
    """
    Market Index Views
    ---
    Provides endpoints to retrieve stock index data such as latest prices,
    historical data, and summary statistics.
    """

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_latest(self, request):
        """Get the latest closing price for each index from the JSON data."""
        manager = StockindexManager()
        latest_data = manager.get_latest()

        # The manager's get_latest() already returns the data in a perfect format.
        # We just need to wrap it in our standard API response structure.
        return JsonResponse({
            'status': 'success',
            'data': latest_data
        })

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_history(self, request, index_type):
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

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_summary(self, request):
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