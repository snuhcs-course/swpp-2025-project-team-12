from S3.finance import FinanceS3Client
from django.http import JsonResponse
from apps.api.constants import FINANCE_BUCKET

def get_latest_overview(sector: str):
    source = FinanceS3Client().check_source(
        bucket=FINANCE_BUCKET,
        prefix=f"llm_output/{sector}"
    )
    if not source["ok"]: return JsonResponse({"message": "No LLM output found"}, status=404)
    year, month, day = source["latest"].split("-")

    llm_output = FinanceS3Client().get_json(
        bucket=FINANCE_BUCKET,
        key=f"llm_output/{sector}/year={year}/month={month}/{year}-{month}-{day}"
    )

    return llm_output