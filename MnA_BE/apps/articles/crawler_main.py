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

GNEWS_URL = "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=ko&gl=KR&ceid=KR:ko"
SECOND_LEVEL_HINTS = {"co", "com", "org", "net", "gov", "edu", "ac", "or", "go", "ne", "re", "sc"}

# S3 설정
S3_BUCKET_NAME = "swpp-12-bucket"
S3_REGION = "ap-northeast-2"

GOOGLE_HOSTS = {"news.google.com", "google.com", "www.google.com"}
STRIP_QS = {"utm_source","utm_medium","utm_campaign","gclid","fbclid"}

# S3 클라이언트 초기화
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

def is_stock_related(title: str) -> bool:
    return True

def setup_driver():
    options = Options()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--disable-gpu')
    options.add_argument('user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36')
    
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)
    
    driver.set_page_load_timeout(120)
    driver.set_script_timeout(120)
    
    return driver

def resolve_google_url_with_browser(driver, google_url: str, max_wait=5):
    try:
        print(f"    [Browser] Navigating: {google_url[:80]}...")
        driver.get(google_url)
        time.sleep(max_wait)
        
        final_url = driver.current_url
        
        if not _is_googleish(final_url):
            print(f"    [Browser] ✓ Resolved: {final_url[:70]}...")
            return final_url
        
        print(f"    [Browser] ✗ Still on Google")
        return None
        
    except Exception as e:
        print(f"    [Browser] ✗ Error: {e}")
        return None

def extract_content(driver, url: str):
    try:
        driver.get(url)
        time.sleep(2)
        
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
        print(f"    [Content] Error: {e}")
        return None

def extract_source(url: str) -> str:
    """
    URL에서 언론사명(주요 2차 도메인)을 추출.
    - news.naver.com  -> naver
    - news.mk.co.kr   -> mk
    - www.bbc.co.uk   -> bbc
    - chosun.com      -> chosun
    """
    try:
        domain = urlparse(url).netloc.lower()
        if domain.startswith("www."):
            domain = domain[4:]
        parts = [p for p in domain.split(".") if p]

        if len(parts) == 1:
            return parts[0]

        # ccTLD (마지막 레이블이 2글자: kr, uk, jp ...)
        if len(parts[-1]) == 2:
            # ex) *.co.kr / *.ac.kr / *.co.uk 등 → 실도메인은 -3번째
            if len(parts) >= 3 and parts[-2] in SECOND_LEVEL_HINTS:
                return parts[-3]
            # ex) *.kr (2레벨 도메인 바로 앞) → -2
            return parts[-2]

        # 일반 gTLD (.com, .org 등) → 2차 도메인
        return parts[-2]
    except Exception:
        return "Unknown"
    
def upload_to_s3(local_file_path, date_obj):
    """S3에 파일 업로드 (파티션 구조)"""
    try:
        year = date_obj.strftime("%Y")
        month = date_obj.strftime("%-m")  # 0 없이
        day = date_obj.strftime("%-d")    # 0 없이
        
        s3_key = f"news-articles/year={year}/month={month}/day={day}/business_top50.json"
        
        print(f"\n[S3] Uploading to s3://{S3_BUCKET_NAME}/{s3_key}...")
        s3_client.upload_file(
            local_file_path, 
            S3_BUCKET_NAME, 
            s3_key,
            ExtraArgs={'ContentType': 'application/json'}
        )
        print(f"[S3] ✓ Upload successful!")
        print(f"[S3] URL: https://{S3_BUCKET_NAME}.s3.{S3_REGION}.amazonaws.com/{s3_key}")
        return True
    except ClientError as e:
        print(f"[S3] ✗ Upload failed: {e}")
        return False
    except Exception as e:
        print(f"[S3] ✗ Error: {e}")
        return False

def main(top=50):
    # 시작 시간 기록
    start_time = time.time()
    start_datetime = datetime.now(tz.gettz("Asia/Seoul"))
    
    print("="*60)
    print("Google News Top 50 (Selenium Method)")
    print(f"Started at: {start_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    
    print("\nFetching Business Topic RSS...")
    feed = feedparser.parse(GNEWS_URL)
    print(f"RSS entries: {len(feed.entries)}")
    
    if not feed.entries:
        print("No entries found!")
        return []
    
    print("\nStarting browser...")
    driver = setup_driver()
    
    results = []
    success_count = 0
    target = top
    seen_urls = set()
    
    stats = {"filtered": 0, "failed": 0}
    
    print(f"\nProcessing up to {target} articles...")
    print("="*60)
    
    try:
        for idx, entry in enumerate(feed.entries):
            title = entry.title.strip()
            print(f"\n[{idx+1}] {title[:60]}...")
            google_url = entry.get("link", "")
            if not google_url:
                print("  ✗ No link in entry")
                stats["failed"] += 1
                continue
            
            original_url = resolve_google_url_with_browser(driver, google_url)
            
            if not original_url:
                stats["failed"] += 1
                continue
            
            original_url = normalize_url(original_url)
            
            if original_url in seen_urls:
                print("  ✗ Duplicate")
                continue
            seen_urls.add(original_url)
            
            content = extract_content(driver, original_url)
            
            if content and len(content) >= 100:
                print(f"  ✓ {len(content)} chars")
                success_count += 1
                
                fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
                
                results.append({
                    "title": title,
                    "url": original_url,
                    "source": extract_source(original_url),
                    "published_at": entry.get("published", ""),
                    "fetched_at": fetched_at.isoformat(),
                    "content": content,
                    "content_length": len(content),
                })
                
                if success_count >= target:
                    print(f"\n✓ Target {target} reached!")
                    break
            else:
                print("  ✗ Content extraction failed")
            
            time.sleep(1)
    
    finally:
        driver.quit()
        print("\nBrowser closed.")
    
    # 종료 시간 계산
    end_time = time.time()
    end_datetime = datetime.now(tz.gettz("Asia/Seoul"))
    elapsed_seconds = end_time - start_time
    elapsed_minutes = elapsed_seconds / 60
    
    # 저장
    fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
    date_folder = fetched_at.strftime("%Y%m%d")
    os.makedirs(f"articles/{date_folder}", exist_ok=True)
    out_path = f"articles/{date_folder}/business_top50.json"
    
    # 메타데이터 포함한 최종 결과
    final_output = {
        "metadata": {
            "start_time": start_datetime.isoformat(),
            "end_time": end_datetime.isoformat(),
            "elapsed_seconds": round(elapsed_seconds, 2),
            "elapsed_minutes": round(elapsed_minutes, 2),
            "target_count": target,
            "success_count": success_count,
            "failed_count": stats["failed"],
            "filtered_count": stats["filtered"],
            "total_articles": len(results)
        },
        "articles": results
    }
    
    # 로컬 저장
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(final_output, f, ensure_ascii=False, indent=2)
    
    print("\n" + "="*60)
    print("FINAL RESULT")
    print("="*60)
    print(f"Started:  {start_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Finished: {end_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Elapsed:  {elapsed_minutes:.2f} minutes ({elapsed_seconds:.2f} seconds)")
    print(f"Succeeded: {success_count}/{target}")
    print(f"Filtered: {stats['filtered']}, Failed: {stats['failed']}")
    print(f"Saved locally: {out_path}")
    
    # S3 업로드
    upload_to_s3(out_path, fetched_at)
    
    print("="*60)
    
    return final_output

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--top', type=int, default=50)
    args = parser.parse_args()
    main(args.top)