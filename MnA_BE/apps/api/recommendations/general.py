import os
import json
from django.http import HttpRequest, JsonResponse
from rest_framework import viewsets
from rest_framework.decorators import action
from S3.finance import FinanceS3Client
from apps.api.constants import *
from decorators import default_error_handler
from utils.for_api import *

class GeneralRecommendationsView(viewsets.ViewSet):

    @action(detail=False, methods=['get'])
    @default_error_handler
    def get(self, request: HttpRequest, year=None, month=None, day=None):
        # Get pagination parameters
        try:
            limit = int(request.GET.get('limit', 10))
            offset = int(request.GET.get('offset', 0))
            # Enforce max limit
            limit = min(limit, 100)
            # Ensure positive limit (minimum 1) and non-negative offset
            limit = max(limit, 1)
            offset = max(offset, 0)
        except (ValueError, TypeError):
            limit = 10
            offset = 0
        
        # if no date provided, get the latest
        if year is None and month is None and day is None:
            source = FinanceS3Client().check_source(bucket=FINANCE_BUCKET, prefix="llm_output")
            if not source["ok"]: 
                return JsonResponse({"message": "No LLM output found"}, status=404)
            year, month, day = source["latest"].split("-")

        path = f"llm_output/{get_path_with_date('top_picks', year, month, day)}"
        try:
            llm_output = FinanceS3Client().get_json(bucket=FINANCE_BUCKET, key=path)
        except Exception as e:
            return JsonResponse({"message": "Unexpected Server Error"}, status=500)

        # Parse JSON string if needed
        if isinstance(llm_output, str):
            llm_output = json.loads(llm_output)
        
        # Extract top_picks
        top_picks = llm_output.get("top_picks", [])
        
        # Transform to frontend format
        all_items = []
        for pick in top_picks:
            all_items.append({
                "ticker": pick.get("ticker"),
                "name": pick.get("name"),
                "price": None,  # TODO: Get from price-financial-info
                "change": None,
                "change_rate": None,
                "time": "09:00",  # TODO: Get actual time
                "headline": pick.get("reason")
            })
        
        # Apply pagination
        total = len(all_items)
        paginated_items = all_items[offset:offset+limit]
        
        return JsonResponse({
            "data": paginated_items,
            "status": "success",
            "total": total,
            "limit": limit,
            "offset": offset
        }, status=200)