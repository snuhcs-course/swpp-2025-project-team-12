# apps/common/mock_data.py
from datetime import datetime, timezone

def iso_now():
    return datetime.now(timezone.utc).isoformat()

MOCK_INDICES = {
    "kospi":  {"value": 2560.12, "changePct": -0.41},
    "kosdaq": {"value": 825.33,  "changePct":  0.22},
    "asOf": iso_now(),
    "source": "mock",
}

MOCK_ARTICLES = {
    "items": [
        {"title":"메모리 업황 회복", "url":"https://example.com/1", "publisher":"MockNews", "publishedAt": iso_now()},
        {"title":"원화 약세, 수출기업 수혜", "url":"https://example.com/2", "publisher":"MockNews", "publishedAt": iso_now()},
    ],
    "asOf": iso_now(),
    "source":"mock"
}

def mock_recommendations():
    return {
        "items":[
            {"symbol":"005930","title":"삼성전자","score":0.82,"reasons":["메모리 업황 개선","환율 수혜"],"evidenceIds":[101,102]},
            {"symbol":"000660","title":"SK하이닉스","score":0.79,"reasons":["HBM 수요 지속"],"evidenceIds":[103]},
        ],
        "asOf": iso_now(),
        "source":"mock"
    }
