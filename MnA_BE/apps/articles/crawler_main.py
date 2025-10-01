import os, json, time
from datetime import datetime
from urllib.parse import urlsplit, urlparse, urlunparse, parse_qsl, urlencode
import feedparser
from dateutil import tz
from bs4 import BeautifulSoup
import re

from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from webdriver_manager.chrome import ChromeDriverManager

GNEWS_URL = "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=ko&gl=KR&ceid=KR:ko"

GOOGLE_HOSTS = {"news.google.com", "google.com", "www.google.com"}
STRIP_QS = {"utm_source","utm_medium","utm_campaign","gclid","fbclid"}

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
    # 본문 추출
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
    """URL에서 언론사 이름 추출"""
    try:
        parsed = urlparse(url)
        domain = parsed.netloc
        if domain.startswith('www.'):
            domain = domain[4:]
        parts = domain.split('.')
        if len(parts) >= 2:
            return parts[0]
        return domain
    except:
        return "Unknown"

def main():
    print("="*60)
    print("Google News Top 50 (Selenium Method)")
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
    target = 50
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
    
    # 저장
    fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
    date_folder = fetched_at.strftime("%Y%m%d")
    os.makedirs(f"articles/{date_folder}", exist_ok=True)
    out_path = f"articles/{date_folder}/business_top50.json"
    
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print("\n" + "="*60)
    print("FINAL RESULT")
    print("="*60)
    print(f"Succeeded: {success_count}/{target}")
    print(f"Filtered: {stats['filtered']}, Failed: {stats['failed']}")
    print(f"Saved: {out_path}")
    print("="*60)
    
    return results

if __name__ == "__main__":
    main()