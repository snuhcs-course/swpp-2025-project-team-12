import os
from django.http import JsonResponse
from rest_framework import viewsets, serializers
from rest_framework.decorators import action
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi

from decorators import default_error_handler
from .stockindex_manager import StockindexManager
from utils.get_llm_overview import get_latest_overview


# ============================================================================
# Serializers
# ============================================================================

class MarketAnalysisSerializer(serializers.Serializer):
    market = serializers.CharField()
    label = serializers.CharField()
    confidence = serializers.FloatField()
    summary = serializers.CharField()

class MarketOverviewResponseSerializer(serializers.Serializer):
    asof_date = serializers.CharField()
    kospi = MarketAnalysisSerializer()
    kosdaq = MarketAnalysisSerializer()
    basic_overview = serializers.CharField()
    news_overview = serializers.CharField()


class IndexDataSerializer(serializers.Serializer):
    change_amount = serializers.FloatField()
    change_percent = serializers.FloatField()
    close = serializers.FloatField()
    date = serializers.CharField()
    high = serializers.FloatField()
    low = serializers.FloatField()
    open = serializers.FloatField()
    volume = serializers.IntegerField()

class LatestIndexResponseSerializer(serializers.Serializer):
    status = serializers.CharField()
    data = serializers.DictField()


class HistoryItemSerializer(serializers.Serializer):
    date = serializers.CharField()
    close = serializers.FloatField()
    change_amount = serializers.FloatField()
    change_percent = serializers.FloatField()

class HistorySingleResponseSerializer(serializers.Serializer):
    status = serializers.CharField()
    index = serializers.CharField()
    days = serializers.IntegerField()
    count = serializers.IntegerField()
    data = HistoryItemSerializer(many=True)

class HistoryBothResponseSerializer(serializers.Serializer):
    status = serializers.CharField()
    index = serializers.CharField()
    days = serializers.IntegerField()
    data = serializers.DictField()


class SummaryDataSerializer(serializers.Serializer):
    latest_close = serializers.FloatField()
    latest_change_amount = serializers.FloatField(allow_null=True)
    latest_change_percent = serializers.FloatField()
    latest_date = serializers.CharField()
    latest_volume = serializers.IntegerField()
    d30_high = serializers.FloatField(source='30d_high')
    d30_low = serializers.FloatField(source='30d_low')
    d30_avg = serializers.FloatField(source='30d_avg')
    w52_high = serializers.FloatField(source='52w_high')
    w52_low = serializers.FloatField(source='52w_low')
    data_points = serializers.IntegerField()

class SummaryResponseSerializer(serializers.Serializer):
    status = serializers.CharField()
    data = serializers.DictField()


# ============================================================================
# Views
# ============================================================================

class MarketLLMview(viewsets.ViewSet):
    """
    Market LLM Views - AI-generated market analysis
    """

    @swagger_auto_schema(
        operation_description="Get AI-generated market analysis for KOSPI and KOSDAQ including technical indicators and news sentiment. Access via /marketindex/overview or /marketindex/overview/{year}/{month}/{day} for historical data.",
        responses={
            200: MarketOverviewResponseSerializer(),
            500: openapi.Response(description="Server error")
        }
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_market_overview(self, request, year=None, month=None, day=None):
        try:
            import json
            llm_output = get_latest_overview("market-index-overview")
        except Exception as e:
            return JsonResponse({"message": "Unexpected Server Error"}, status=500)

        return JsonResponse(llm_output, status=200, safe=False)


class StockIndexView(viewsets.ViewSet):
    """
    Stock Index Views - KOSPI/KOSDAQ data
    """

    @swagger_auto_schema(
        operation_description="Get latest closing prices and daily changes for KOSPI and KOSDAQ indices",
        responses={200: LatestIndexResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_latest(self, request):
        manager = StockindexManager()
        latest_data = manager.get_latest()

        return JsonResponse({
            'status': 'success',
            'data': latest_data
        })

    @swagger_auto_schema(
        operation_description="Get historical daily price data for stock indices with flexible date range",
        manual_parameters=[
            openapi.Parameter('index_type', openapi.IN_PATH, description='Index name: "KOSPI", "KOSDAQ", or "BOTH"', type=openapi.TYPE_STRING, required=True),
            openapi.Parameter('days', openapi.IN_QUERY, description="Number of days to retrieve (default: 30, min: 1, max: 365)", type=openapi.TYPE_INTEGER),
        ],
        responses={
            200: openapi.Response(
                description="Historical data",
                examples={
                    "application/json": {
                        "status": "success",
                        "index": "KOSPI",
                        "days": 30,
                        "count": 30,
                        "data": [
                            {
                                "date": "2025-10-01",
                                "close": 3455.83,
                                "change_amount": 31.23,
                                "change_percent": 0.91
                            }
                        ]
                    }
                }
            ),
            400: openapi.Response(description="Invalid index type")
        }
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_history(self, request, index_type):
        manager = StockindexManager()

        try:
            days = int(request.GET.get('days', 30))
            days = min(max(1, days), 365)
        except (ValueError, TypeError):
            days = 30

        if index_type.upper() == 'BOTH':
            both_data = {}

            for idx_name in manager.indices.keys():
                history = manager.get_history(idx_name, days=days)

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

        else:
            valid_indices = list(manager.indices.keys())
            if index_type not in valid_indices:
                return JsonResponse({
                    'status': 'error',
                    'message': f'Invalid index. Choose from: {", ".join(valid_indices + ["BOTH"])}'
                }, status=400)

            history = manager.get_history(index_type, days=days)

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

    @swagger_auto_schema(
        operation_description="Get 30-day and 52-week summary statistics for both KOSPI and KOSDAQ",
        responses={200: SummaryResponseSerializer()}
    )
    @action(detail=False, methods=['get'])
    @default_error_handler
    def get_summary(self, request):
        manager = StockindexManager()
        summary_data = manager.get_summary()
        latest_data = manager.get_latest()

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
    