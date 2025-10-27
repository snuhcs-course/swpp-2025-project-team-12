import os, json, time, argparse
from datetime import datetime
from urllib.parse import urlsplit, urlparse, urlunparse, parse_qsl, urlencode
import feedparser
from dateutil import tz
from bs4 import BeautifulSoup
import re
import boto3
from botocore.exceptions import ClientError

from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from webdriver_manager.chrome import ChromeDriverManager

# 섹션별 크롤링 설정 (총 100개)
SECTIONS = [
    {"name": "BUSINESS", "count": 50},
    {"name": "TECHNOLOGY", "count": 10},
    {"name": "HEALTH", "count": 10},
    {"name": "WORLD", "count": 10},
    {"name": "NATION", "count": 10},
    {"name": "SCIENCE", "count": 10},
]

def get_gnews_url(section):
    """섹션별 Google News RSS URL 생성"""
    return f"https://news.google.com/rss/headlines/section/topic/{section}?hl=ko&gl=KR&ceid=KR:ko"

# S3 설정
S3_BUCKET_NAME = "swpp-12-bucket"
S3_REGION = "ap-northeast-2"

GOOGLE_HOSTS = {"news.google.com", "google.com", "www.google.com"}
STRIP_QS = {"utm_source","utm_medium","utm_campaign","gclid","fbclid"}

# S3 클라이언트 초기화 (환경변수에서 자격 증명 자동 로드)
s3_client = boto3.client('s3', region_name=S3_REGION)

def _is_googleish(u: str) -> bool:
    try:
        h = urlsplit(u).netloc.lower()
    except:
        return True
    return any(h == g or h.endswith("." + g) for g in GOOGLE_HOSTS)

def normalize_url(u: str) -> str:
    try:
        p = urlparse(u)
        p = p._replace(fragment="")
        qs = [(k,v) for k,v in parse_qsl(p.query) if k not in STRIP_QS]
        return urlunparse(p._replace(query=urlencode(qs)))
    except:
        return u

def setup_driver():
    options = Options()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--disable-gpu')
    options.add_argument('user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36')
    
    # 🔥 속도 개선 1: 이미지 로딩 차단
    prefs = {
        'profile.default_content_setting_values': {
            'images': 2,  # 이미지 차단
        },
        'profile.managed_default_content_settings': {
            'images': 2
        }
    }
    options.add_experimental_option('prefs', prefs)
    
    # 🔥 속도 개선 2: 불필요한 기능 비활성화
    options.add_argument('--disable-extensions')
    options.add_argument('--disable-infobars')
    options.add_argument('--disable-logging')
    options.add_argument('--log-level=3')
    options.add_argument('--blink-settings=imagesEnabled=false')
    
    # ChromeDriver 설정 (환경 자동 감지)
    import platform
    system = platform.system()
    
    if system == "Linux":
        # GCP 서버 (Ubuntu): 직접 경로 사용 (빠름)
        service = Service('/usr/bin/chromedriver')
    else:
        # 로컬 (Mac/Windows): ChromeDriverManager 사용
        service = Service(ChromeDriverManager().install())
    
    driver = webdriver.Chrome(service=service, options=options)
    
    # 타임아웃 설정 (서버 환경 고려)
    driver.set_page_load_timeout(30)
    driver.set_script_timeout(30)
    
    return driver

def resolve_google_url_with_browser(driver, google_url: str, max_wait=3, max_retries=2):
    """Google News URL을 실제 기사 URL로 리디렉션 (재시도 포함)"""
    for attempt in range(max_retries):
        try:
            driver.get(google_url)
            time.sleep(max_wait)
            
            final_url = driver.current_url
            
            if not _is_googleish(final_url):
                return final_url
            
            return None
            
        except Exception as e:
            if attempt < max_retries - 1:
                print(f"    [Browser] ⟳ Retry {attempt+1}/{max_retries-1}...")
                time.sleep(2)
                continue
            print(f"    [Browser] ✗ Failed after {max_retries} attempts")
            return None

def extract_content(driver, url: str, retry=False):
    """기사 본문 추출 (재시도 시 더 긴 대기)"""
    try:
        driver.get(url)
        
        # 재시도 시 더 오래 대기 (JS 렌더링 완료 보장)
        wait_time = 3.5 if retry else 1.5
        time.sleep(wait_time)
        
        html = driver.page_source
        soup = BeautifulSoup(html, "lxml")
        
        for tag in soup(['script', 'style', 'nav', 'header', 'footer']):
            tag.decompose()
        
        paragraphs = soup.find_all("p")
        text = " ".join(p.get_text(" ", strip=True) for p in paragraphs)
        text = re.sub(r'\s+', ' ', text).strip()
        
        if len(text) >= 100:
            return text
        
        return None
        
    except Exception as e:
        print(f"    [Content] Error: {str(e)[:50]}...")
        return None

# ccTLD 및 다중 레벨 도메인 처리를 위한 상수
_CC_TLDS = {
    # 대표적 ccTLD (필요 시 추가 가능)
    "kr","uk","jp","au","nz","za","br","mx","id","sg","th","tw","in","cn","tr","sa","ae","ru","de","fr","it","es"
}
# ccTLD 앞에 붙는 2차 레벨(조직/지역 구분자)
_MULTI_TLD_SECOND = {
    "co","or","go","ne","re","pe","ac","hs","ms","es","kg","sc",
    "gov","edu","net","org",  # 일부 국가에서 쓰는 변형
    # 한국 지역(예: seoul.kr 등) — 필요 시 확장
    "seoul","busan","daegu","incheon","gwangju","daejeon","ulsan","jeju",
    "gyeonggi","gangwon","chungbuk","chungnam","jeonbuk","jeonnam","gyeongbuk","gyeongnam",
}

def extract_source(url: str) -> str:
    """
    호스트명에서 2차 도메인(SLD)을 반환.
    예) news.naver.com -> naver
        www.hankyung.com -> hankyung
        www.mk.co.kr -> mk
    """
    try:
        host = (urlparse(url).hostname or "").lower()
        if host.startswith("www."):
            host = host[4:]
        labels = [p for p in host.split(".") if p]
        if len(labels) < 2:
            return host  # localhost 등

        # ccTLD + 조직/지역 세컨드 레벨(e.g., *.co.kr, *.ac.jp 등) 처리
        if len(labels) >= 3 and labels[-1] in _CC_TLDS and labels[-2] in _MULTI_TLD_SECOND:
            return labels[-3]

        # 일반적 케이스 (*.com, *.net, *.io, *.kr 등)
        return labels[-2]
    except:
        return "Unknown"

def crawl_section(driver, section_name, target_count, seen_urls):
    """특정 섹션 크롤링"""
    print(f"\n{'='*60}")
    print(f"📰 Section: {section_name} (Target: {target_count})")
    print(f"{'='*60}")
    
    rss_url = get_gnews_url(section_name)
    feed = feedparser.parse(rss_url)
    print(f"RSS entries found: {len(feed.entries)}")
    
    if not feed.entries:
        print(f"⚠️ No entries found for {section_name}")
        return [], {"filtered": 0, "failed": 0}
    
    results = []
    success_count = 0
    stats = {"filtered": 0, "failed": 0}
    
    for idx, entry in enumerate(feed.entries):
        if success_count >= target_count:
            break
            
        title = entry.title.strip()
        print(f"\n[{section_name} {idx+1}] {title[:50]}...")
        
        google_url = entry.get("link", "")
        if not google_url:
            print("  ✗ No link")
            stats["failed"] += 1
            continue
        
        original_url = resolve_google_url_with_browser(driver, google_url)
        
        if not original_url:
            print("  ✗ URL resolve failed")
            stats["failed"] += 1
            continue
        
        original_url = normalize_url(original_url)
        
        if original_url in seen_urls:
            print("  ✗ Duplicate")
            stats["filtered"] += 1
            continue
        seen_urls.add(original_url)
        
        content = extract_content(driver, original_url)
        
        # 실패 시 한 번 더 시도 (더 긴 대기 시간)
        if not content:
            print("  ⟳ Retrying with longer wait...")
            content = extract_content(driver, original_url, retry=True)
        
        if content and len(content) >= 100:
            print(f"  ✓ {len(content)} chars")
            success_count += 1
            
            fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
            
            results.append({
                "title": title,
                "url": original_url,
                "source": extract_source(original_url),
                "section": section_name,
                "published_at": entry.get("published", ""),
                "fetched_at": fetched_at.isoformat(),
                "content": content,
                "content_length": len(content),
            })
        else:
            print("  ✗ Content extraction failed")
            stats["failed"] += 1
        
        time.sleep(0.5)
    
    print(f"\n✓ {section_name}: {success_count}/{target_count} collected")
    return results, stats

def upload_to_s3(local_file_path, date_obj):
    """S3에 파일 업로드 (파티션 구조)"""
    try:
        year = date_obj.strftime("%Y")
        month = str(int(date_obj.strftime('%m')))
        day = str(int(date_obj.strftime('%d')))
        
        s3_key = f"news-articles/year={year}/month={month}/day={day}/multi_section_top100.json"
        
        print(f"\n[S3] Uploading to s3://{S3_BUCKET_NAME}/{s3_key}...")
        s3_client.upload_file(
            local_file_path, 
            S3_BUCKET_NAME, 
            s3_key,
            ExtraArgs={'ContentType': 'application/json'}
        )
        print(f"[S3] ✓ Upload successful!")
        return True
    except ClientError as e:
        print(f"[S3] ✗ Upload failed: {e}")
        return False
    except Exception as e:
        print(f"[S3] ✗ Error: {e}")
        return False

def main():
    # 시작 시간 기록
    start_time = time.time()
    start_datetime = datetime.now(tz.gettz("Asia/Seoul"))
    
    print("="*60)
    print("Google News Multi-Section Crawler")
    print(f"Started at: {start_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    print("\n📋 Crawling Plan:")
    for section in SECTIONS:
        print(f"  - {section['name']}: {section['count']} articles")
    print(f"\n🎯 Total Target: {sum(s['count'] for s in SECTIONS)} articles")
    
    print("\n🌐 Starting browser...")
    driver = setup_driver()
    
    all_results = []
    seen_urls = set()
    section_stats = {}
    
    try:
        for section_config in SECTIONS:
            section_name = section_config["name"]
            target_count = section_config["count"]
            
            results, stats = crawl_section(driver, section_name, target_count, seen_urls)
            all_results.extend(results)
            section_stats[section_name] = {
                "target": target_count,
                "success": len(results),
                "filtered": stats["filtered"],
                "failed": stats["failed"]
            }
    
    finally:
        driver.quit()
        print("\n🔒 Browser closed.")
    
    # 종료 시간 계산
    end_time = time.time()
    end_datetime = datetime.now(tz.gettz("Asia/Seoul"))
    elapsed_seconds = end_time - start_time
    elapsed_minutes = elapsed_seconds / 60
    
    # 저장
    fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
    date_folder = fetched_at.strftime("%Y%m%d")
    os.makedirs(f"articles/{date_folder}", exist_ok=True)
    out_path = f"articles/{date_folder}/multi_section_top100.json"
    
    # 메타데이터 포함한 최종 결과
    final_output = {
        "metadata": {
            "start_time": start_datetime.isoformat(),
            "end_time": end_datetime.isoformat(),
            "elapsed_seconds": round(elapsed_seconds, 2),
            "elapsed_minutes": round(elapsed_minutes, 2),
            "total_target": sum(s['count'] for s in SECTIONS),
            "total_collected": len(all_results),
            "section_stats": section_stats
        },
        "articles": all_results
    }
    
    # 로컬 저장
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(final_output, f, ensure_ascii=False, indent=2)
    
    print("\n" + "="*60)
    print("📊 FINAL RESULT")
    print("="*60)
    print(f"Started:  {start_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Finished: {end_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Elapsed:  {elapsed_minutes:.2f} minutes ({elapsed_seconds:.2f} seconds)")
    print(f"\n📈 Section Breakdown:")
    for section_name, stats in section_stats.items():
        print(f"  {section_name:12} {stats['success']:3}/{stats['target']:3}  "
              f"(filtered: {stats['filtered']}, failed: {stats['failed']})")
    print(f"\n🎯 Total: {len(all_results)}/{sum(s['count'] for s in SECTIONS)} articles collected")
    print(f"💾 Saved locally: {out_path}")
    
    # S3 업로드
    upload_to_s3(out_path, fetched_at)
    
    print("="*60)
    
    return final_output

if __name__ == "__main__":
    main()