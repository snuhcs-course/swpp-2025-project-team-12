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
    """
    DB 구조와 일치하도록 수정된 Mock 추천 데이터
    DB에서 가져오는 형식: ticker, name, market, rank, news, reason, expected_direction, conviction
    """
    return {
        "items": [
            {
                "ticker": "005930",
                "name": "삼성전자",
                "market": "KOSPI",
                "rank": 1,
                "news": [
                    {"title": "메모리 반도체 업황 개선", "url": "https://example.com/101", "source": "MockNews"},
                    {"title": "환율 수혜로 실적 개선 기대", "url": "https://example.com/102", "source": "MockNews"}
                ],
                "reason": ["메모리 업황 개선", "환율 수혜"],
                "expected_direction": "up",
                "conviction": 0.82
            },
            {
                "ticker": "000660",
                "name": "SK하이닉스",
                "market": "KOSPI",
                "rank": 2,
                "news": [
                    {"title": "HBM 수요 지속 증가", "url": "https://example.com/103", "source": "MockNews"}
                ],
                "reason": ["HBM 수요 지속"],
                "expected_direction": "up",
                "conviction": 0.79
            },
            {
                "ticker": "035720",
                "name": "카카오",
                "market": "KOSDAQ",
                "rank": 3,
                "news": [
                    {"title": "AI 사업 확대", "url": "https://example.com/104", "source": "MockNews"}
                ],
                "reason": ["AI 플랫폼 성장"],
                "expected_direction": "neutral",
                "conviction": 0.65
            },
            {
                "ticker": "207940",
                "name": "삼성바이오로직스",
                "market": "KOSPI",
                "rank": 4,
                "news": [
                    {"title": "위탁생산 수주 증가", "url": "https://example.com/105", "source": "MockNews"}
                ],
                "reason": ["CDMO 시장 성장"],
                "expected_direction": "up",
                "conviction": 0.75
            },
            {
                "ticker": "035420",
                "name": "NAVER",
                "market": "KOSDAQ",
                "rank": 5,
                "news": [
                    {"title": "클라우드 사업 성장", "url": "https://example.com/106", "source": "MockNews"}
                ],
                "reason": ["클라우드 확대"],
                "expected_direction": "up",
                "conviction": 0.70
            },
        ],
        "asOf": iso_now(),
        "source": "mock"
    }