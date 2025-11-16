from S3.finance import FinanceBucket
from django.http import JsonResponse

def get_latest_overview(sector: str):
    s3 = FinanceBucket()
    source = s3.check_source(prefix=f"llm_output/{sector}")
    if not source["ok"]: return JsonResponse({"message": "No LLM output found"}, status=404)
    year, month, day = source["latest"].split("-")

    llm_output = s3.get_json(
        key=f"llm_output/{sector}/year={year}/month={month}/{year}-{month}-{day}"
    )

    return llm_output