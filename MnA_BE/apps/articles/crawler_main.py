import os, json, time
from datetime import datetime
from urllib.parse import urlsplit, urlunsplit, parse_qsl, urlencode
import feedparser
from dateutil import parser as dtparser, tz
import requests
from bs4 import BeautifulSoup

# RSS 소스 목록
RSS_SOURCES = [
    # 매일경제
    "https://www.mk.co.kr/rss/50000001/",  # 증권
    "https://www.mk.co.kr/rss/30000001/",  # 경제
    
    # 한국경제
    "https://www.hankyung.com/feed/economy",  # 경제
    "https://www.hankyung.com/feed/finance",  # 증권
    
    # 아시아경제
    "https://www.asiae.co.kr/rss/stock.htm",  # 증권
    "https://www.asiae.co.kr/rss/economy.htm",  # 경제
]

# 가중치 기반 금융 키워드
FINANCE_KEYWORDS = {
    "코스피": 5, "코스닥": 5, "주가": 5, "증시": 5, "상장": 5, "ipo": 5, 
    "배당": 5, "수익률": 5, "etf": 5, "펀드": 5, "환율": 5, "원달러": 5,
    "금리인상": 5, "금리인하": 5, "기준금리": 5, "공매도": 5, "선물": 5, "옵션": 5,
    "지수": 5, "종목": 5, "코스피200": 5, "코스닥150": 5,
    
    "공시": 5, "자사주": 5, "소각": 5, "유상증자": 5, "무상증자": 5, "액면분할": 5,
    "감자": 5, "상장폐지": 5, "거래정지": 5, "관리종목": 5, "공모가": 5, "수요예측": 5,
    "기관경쟁률": 5, "청약": 5, "상장예정": 5, "따상": 5, "합병": 5, "분할": 5,
    "배당금": 5, "배당락": 5, "주주총회": 5, "의결권": 5, "주주환원": 5,
    
    "10년물": 5, "2년물": 5, "국채금리": 5, "회사채": 5, "달러인덱스": 5,
    
    "pce": 5, "cpi": 5, "ppi": 5, "fomc": 5, "fed": 5,
    
    "실적": 3, "매출": 3, "영업이익": 3, "순이익": 3, "분기실적": 3, 
    "목표주가": 3, "급등": 3, "급락": 3, "상승": 3, "하락": 3, "반등": 3, "랠리": 3,
    "채권": 3, "국채": 3, "물가": 3, "인플레이션": 3, "디플레이션": 3,
    "gdp": 3, "성장률": 3, "무역": 3, "수출": 3, "수입": 3, "경기침체": 3, "경기회복": 3,
    "매수": 3, "매도": 3, "거래량": 3, "시가총액": 3,
    
    "국제유가": 3, "유가": 3, "휘발유": 3, "경유": 3, "석유": 3, "원유": 3, 
    "정유": 3, "에너지": 3, "기름": 3, "브렌트유": 3, "wti": 3, "두바이유": 3,
    "정유사": 3, "휘발유값": 3, "경유값": 3, "한국석유공사": 3, "오피넷": 3,
    
    "한국은행": 3, "금통위": 3, "통화정책": 3, "재정정책": 3, "qe": 3, "qt": 3,
    
    "kodex": 3, "tiger": 3, "kbstar": 3, "kindex": 3, "hanaro": 3, "arirang": 3,
    
    "금융": 2, "은행": 2, "증권": 2, "보험": 2, "금리": 2,
    "외환": 2, "투자": 2, "기업": 2, "어닝": 2, "전망": 2, "시장": 2,
    "마켓": 2, "거래": 2, "금융위": 2, "금감원": 2,
    
    "반도체": 2, "자동차": 2, "2차전지": 2, "전기차": 2, "배터리": 2,
    "철강": 2, "조선": 2, "건설": 2, "바이오": 2, "제약": 2, "it": 2, "통신": 2,
    "대주주": 2, "신용거래": 2, "차입거래": 2, "증권사": 2, "리포트": 2,
    
    "경제": 1, "산업": 1, "화학": 1, "게임": 1, "엔터": 1,
    "외국인": 1, "기관": 1, "개인": 1, "컨센서스": 1, "가이던스": 1,
    "물가상승률": 1, "환율전망": 1, "외환보유액": 1, "투자의견": 1,
    "lng": 1, "가스": 1, "경기부양": 1, "긴축": 1,
}

# 비금융 키워드
NEGATIVE_KEYWORDS = {
    "연예": -5,
}

# 국내 맥락 키워드
DOMESTIC_ANCHORS = {
    "코스피", "코스닥", "krx", "유가증권", "코넥스", "한국", "국내", "원화", 
    "원/달러", "달러/원", "환율", "한국은행", "금통위", "금융위", "금감원",
    "삼성전자", "sk하이닉스", "현대차", "기아", "포스코", "lg화학", "lg에너지솔루션", 
    "셀트리온", "네이버", "카카오", "kodex", "tiger", "kbstar"
}

# 해외 맥락 키워드
FOREIGN_ONLY = {
    "나스닥", "다우", "s&p500", "sox", "뉴욕증시", "미국증시", "뉴욕장", 
    "테슬라", "애플", "엔비디아", "마이크로소프트", "아마존"
}

# 한국 주요 뉴스 사이트별 본문 셀렉터
NEWS_SELECTORS = {
    'mk.co.kr': ['.news_cnt_detail_wrap', '.art_txt', '.news_detail_text'],
    'hankyung.com': ['.news-text', '.txt', '.article-body'],
    'fnnews.com': ['.article-body', '.news_content', '.article_txt'],
    'biz.chosun.com': ['.news-article__body', '.article-body'],
    'edaily.co.kr': ['.news_body', '.articleText', '.article_body'],
    'mt.co.kr': ['.textBody', '.view_txt', '.article_txt'],
    'sedaily.com': ['.user_sns_area', '.news_body', '.v_article'],
    'yna.co.kr': ['.story-news-article', '.article-text', '.article_body'],
    'asiae.co.kr': ['.article_txt', '.view_article'],
    'naver.com': ['#newsct_article', '#dic_area'],
    'daum.net': ['#harmonyContainer', '.news_view'],
}

def canonical_url(u: str) -> str:
    try:
        sp = urlsplit(u)
        q = [(k,v) for k,v in parse_qsl(sp.query, keep_blank_values=True) if not k.lower().startswith("utm_")]
        return urlunsplit((sp.scheme, sp.netloc, sp.path, urlencode(q, doseq=True), "")) or u
    except Exception:
        return u

def norm_title(t: str) -> str:
    return " ".join((t or "").strip().split()).lower()

def to_kst_iso(dt_str: str, fetched_at: datetime) -> str:
    if not dt_str:
        return fetched_at.isoformat()
    try:
        dt = dtparser.parse(dt_str)
        if not dt.tzinfo:
            dt = dt.replace(tzinfo=tz.UTC)
        return dt.astimezone(tz.gettz("Asia/Seoul")).isoformat()
    except Exception:
        return fetched_at.isoformat()

def exclude_by_url(url: str) -> bool:
    """URL 패턴으로 비금융 섹션 빠른 제외 (IT 섹션은 포함)"""
    u = url.lower()
    bad_patterns = ["/sports/", "/entertain", "/life/", "/travel/", "/culture/"]
    return any(pattern in u for pattern in bad_patterns)

def extract_source(entry) -> str:
    try:
        if hasattr(entry, "source") and entry.source and entry.source.get("title"):
            return entry.source.get("title")
    except Exception:
        pass
    try:
        sp = urlsplit(entry.link)
        domain = sp.netloc.lower()
        if domain.startswith("www."):
            domain = domain[4:]
            
        domain_names = {
            'mk.co.kr': '매일경제',
            'hankyung.com': '한국경제', 
            'fnnews.com': '파이낸셜뉴스',
            'biz.chosun.com': '조선비즈',
            'edaily.co.kr': '이데일리',
            'mt.co.kr': '머니투데이',
            'sedaily.com': '서울경제',
            'yna.co.kr': '연합뉴스',
            'asiae.co.kr': '아시아경제',
            'naver.com': '네이버',
            'daum.net': '다음',
        }
        return domain_names.get(domain, domain)
    except Exception:
        return "unknown"

def is_finance_article(title: str, content: str = "") -> bool:
    """가중치 기반 금융 키워드 점수 계산 (12점 이상이면 금융 기사로 인정)"""
    text = (title + " " + content).lower()
    
    positive_score = 0
    for keyword, weight in FINANCE_KEYWORDS.items():
        count = text.count(keyword)
        positive_score += count * weight
    
    negative_score = 0
    for keyword, weight in NEGATIVE_KEYWORDS.items():
        count = text.count(keyword)
        negative_score += count * weight
    
    total_score = positive_score + negative_score
    
    return total_score >= 12

def has_domestic_context(title: str, content: str = "") -> bool:
    """국내 맥락 확인 (해외 단독 기사 필터링)"""
    text = (title + " " + content).lower()
    
    # 국내 맥락 키워드 확인
    has_domestic = any(keyword.lower() in text for keyword in DOMESTIC_ANCHORS)
    # 해외 맥락 키워드만 있는지 확인
    has_foreign_only = any(keyword.lower() in text for keyword in FOREIGN_ONLY)

    return has_domestic or not has_foreign_only

def extract_section(soup) -> str:
    """메타 태그에서 섹션 정보 추출"""
    meta = soup.select_one('meta[property="article:section"]')
    if meta and meta.get('content'):
        return meta['content'].strip().lower()
    
    meta = soup.select_one('meta[name="section"]')
    if meta and meta.get('content'):
        return meta['content'].strip().lower()
    
    return ""

def clean_text(text: str) -> str:
    """텍스트 정리"""
    if not text:
        return ""
    
    import re
    
    patterns = [
        r'기자\s*=.*?$',
        r'^\s*\[.*?\]\s*',
        r'@\w+\.\w+',
        r'Copyright.*?$',
        r'저작권.*?$',
        r'무단.*?금지.*?$',
        r'Copyrights.*?$',
        r'▶.*?$',
        r'※.*?$',
    ]
    
    lines = text.split('\n')
    cleaned_lines = []
    
    for line in lines:
        line = line.strip()
        if not line or len(line) < 10:
            continue
            
        skip = False
        for pattern in patterns:
            if re.search(pattern, line, re.IGNORECASE):
                skip = True
                break
        
        if not skip:
            cleaned_lines.append(line)
    
    result = ' '.join(cleaned_lines)
    result = re.sub(r'\s+', ' ', result)
    
    return result.strip()

def extract_article_content(url: str, max_retries: int = 2) -> str:
    """기사 본문 추출"""
    for attempt in range(max_retries):
        try:
            headers = {
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
            
            response = requests.get(url, headers=headers, timeout=15)
            response.raise_for_status()
            response.encoding = response.encoding or response.apparent_encoding or "utf-8"
            
            soup = BeautifulSoup(response.text, 'html.parser')
            
            section = extract_section(soup)
            if section in {"라이프", "연예", "스포츠", "문화", "여행"}:
                return "[비금융 섹션 제외]"
            
            for tag in soup(['script', 'style', 'nav', 'header', 'footer', 'aside', 'advertisement', 'iframe']):
                tag.decompose()
            
            domain = urlsplit(url).netloc.lower()
            if domain.startswith("www."):
                domain = domain[4:]
            content_text = ""
            
            for site, selectors in NEWS_SELECTORS.items():
                if site in domain:
                    for selector in selectors:
                        elements = soup.select(selector)
                        if elements:
                            content_text = ' '.join(elem.get_text().strip() for elem in elements)
                            if len(content_text) > 300:
                                return clean_text(content_text)
            
            general_selectors = [
                'article', 'div[id*="article"]', 'div[class*="article"]', 
                'div[id*="content"]', 'div[class*="content"]',
                'div[id*="news"]', 'div[class*="news"]'
            ]
            
            for selector in general_selectors:
                elements = soup.select(selector)
                if elements:
                    content_text = ' '.join(elem.get_text().strip() for elem in elements)
                    if len(content_text) > 300:
                        return clean_text(content_text)
            
            paragraphs = soup.find_all('p')
            if paragraphs:
                content_text = ' '.join(p.get_text().strip() for p in paragraphs)
                if len(content_text) > 300:
                    return clean_text(content_text)
            
            return "[본문이 너무 짧음]"
                
        except Exception as e:
            if attempt < max_retries - 1:
                time.sleep(1)
    
    return "[본문 추출 실패]"

def load_feed(url):
    try:
        f = feedparser.parse(url)
        entries = f.entries or []
        return entries
    except Exception as e:
        print(f"  [WARNING] Failed to load {url}: {e}")
        return []

def main(top=50, no_content=False):
    """
    메인 크롤링 함수
    
    Args:
        top: 저장할 최대 기사 수
        no_content: True일 경우 본문 추출 생략
    """
    fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
    date_folder = fetched_at.strftime("%Y%m%d")
    out_dir = os.path.join("articles", date_folder)
    os.makedirs(out_dir, exist_ok=True)

    # RSS 수집
    items = []
    for i, url in enumerate(RSS_SOURCES):
        print(f"Fetching RSS {i+1}/{len(RSS_SOURCES)} - {url.split('/')[-1]}")
        entries = load_feed(url)
        items.extend(entries)
        print(f"  -> {len(entries)} entries")
    total = len(items)

    # 중복 제거
    seen = set()
    results = []
    
    for e in items:
        title = getattr(e, "title", "").strip()
        url = canonical_url(getattr(e, "link", "").strip())
        
        if not title or not url:
            continue
            
        if exclude_by_url(url):
            continue
            
        # Google News 리디렉션 제외
        if 'news.google.com/rss/articles/' in url:
            continue
            
        # 중복 체크
        key = (norm_title(title), url)
        if key in seen:
            continue
        seen.add(key)

        published_raw = getattr(e, "published", "") or getattr(e, "updated", "")
        src = extract_source(e)
        rec = {
            "title": title,
            "url": url,
            "source": src,
            "published_at": to_kst_iso(published_raw, fetched_at),
            "fetched_at": fetched_at.isoformat(),
        }
        results.append(rec)

    print(f"\n중복 제거 후: {len(results)}개 기사")

    # 본문 크롤링
    if not no_content:
        print(f"\n본문 추출 및 국내 금융 필터링 중...")
        filtered_results = []
        
        for i, article in enumerate(results):
            if len(filtered_results) >= top:
                break
                
            print(f"[{i+1}/{len(results)}] {article['title'][:60]}...")
            
            content = extract_article_content(article['url'])
            article['content'] = content
            article['content_length'] = len(content)
            
            # 본문 기반 금융 필터링 + 국내 맥락 확인
            if content not in ["[본문 추출 실패]", "[본문이 너무 짧음]", "[비금융 섹션 제외]"]:
                if is_finance_article(article['title'], content) and has_domestic_context(article['title'], content):
                    filtered_results.append(article)
                    print(f"  ✓ 국내금융기사 확정: {len(content)}자")
                else:
                    if not is_finance_article(article['title'], content):
                        print(f"  ✗ 비금융 기사로 제외")
                    else:
                        print(f"  ✗ 해외 단독 기사로 제외")
            else:
                # 본문 실패시 제목만으로 판단
                if content == "[비금융 섹션 제외]":
                    print(f"  ✗ 섹션 기준 제외: {content}")
                elif is_finance_article(article['title']) and has_domestic_context(article['title']):
                    filtered_results.append(article)
                    print(f"  ✓ 제목 기준 포함: {content}")
                else:
                    print(f"  ✗ 제외: {content}")
            
            time.sleep(0.3)
        
        results = filtered_results
    else:
        print("본문 추출 생략 - 제목만으로 필터링")
        filtered_results = []
        for article in results:
            if is_finance_article(article['title']) and has_domestic_context(article['title']):
                filtered_results.append(article)
        results = filtered_results[:top]

    # 저장
    out_path = os.path.join(out_dir, "business_top50.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    # 결과 요약
    if not no_content:
        successful_extractions = sum(1 for r in results if r.get('content', '') not in ["[본문 추출 실패]", "[본문이 너무 짧음]", "[비금융 섹션 제외]"])
        content_stats = [len(r.get('content', '')) for r in results if r.get('content', '') not in ["[본문 추출 실패]", "[본문이 너무 짧음]", "[비금융 섹션 제외]"]]
        avg_content_length = sum(content_stats) / len(content_stats) if content_stats else 0
        
        print(f"\n최종 결과:")
        print(f"  총 수집: {total}개")
        print(f"  중복 제거: {len(seen)}개")
        print(f"  국내 금융 필터링 후: {len(results)}개")
        print(f"  본문 추출 성공: {successful_extractions}개")
        print(f"  평균 본문 길이: {avg_content_length:.0f}자")

    print(f"\n[OK] 국내 금융 기사 {len(results)}개 저장 -> {out_path}")

    # 샘플 출력
    print(f"\n샘플 기사 제목:")
    for i, article in enumerate(results[:5]):
        print(f"  {i+1}. [{article['source']}] {article['title']}")
    
    return results

# Standalone 실행 지원
if __name__ == "__main__":
    import argparse
    
    ap = argparse.ArgumentParser(description="Crawl Korean finance articles from RSS feeds")
    ap.add_argument("--top", type=int, default=50, help="Maximum number of articles to save (default: 50)")
    ap.add_argument("--no-content", action="store_true", help="Skip article content extraction")
    args = ap.parse_args()
    
    main(top=args.top, no_content=args.no_content)