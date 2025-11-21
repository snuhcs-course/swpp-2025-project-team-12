import time, json, os, re
from datetime import datetime
from dateutil import tz
from bs4 import BeautifulSoup
from urllib.parse import urlparse
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from webdriver_manager.chrome import ChromeDriverManager


# S3 설정
S3_BUCKET_NAME = "swpp-12-bucket"
S3_REGION = "ap-northeast-2"

# S3 클라이언트 초기화
try:
    import boto3
    from botocore.exceptions import ClientError
    s3_client = boto3.client('s3', region_name=S3_REGION)
except:
    s3_client = None


SECTION_URL = "https://news.naver.com/section/101"  # 경제 섹션
TARGET_COUNT = 1000


def setup_driver():
    import os, subprocess

    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    options.add_argument("--disable-extensions")
    options.add_argument("--disable-logging")
    options.add_argument("--log-level=3")
    options.add_argument(
        "user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--lang=ko-KR")

    chrome_bin_candidates = [
        os.environ.get("CHROME_BIN"),
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        "/Applications/Chromium.app/Contents/MacOS/Chromium",
        "/opt/google/chrome/chrome",
        "/usr/bin/google-chrome",
        "/usr/bin/google-chrome-stable",
        "/usr/bin/chromium",
        "/usr/bin/chromium-browser",
    ]
    chrome_bin = next((p for p in chrome_bin_candidates if p and os.path.exists(p)), None)
    if not chrome_bin:
        raise RuntimeError("Chrome binary not found")

    if chrome_bin.endswith(".app/Contents/MacOS/Google Chrome") or chrome_bin.endswith(".app/Contents/MacOS/Chromium"):
        real = chrome_bin
    else:
        real = os.path.realpath(chrome_bin)

    options.binary_location = real

    try:
        ver = subprocess.run([real, "--version"], capture_output=True, text=True)
        print("[BROWSER]", ver.stdout.strip() or real)
    except Exception:
        print("[BROWSER] Using", real)

    driver_path = ChromeDriverManager().install()
    service = Service(driver_path)
    driver = webdriver.Chrome(service=service, options=options)
    driver.set_page_load_timeout(60)
    driver.implicitly_wait(3)
    
    caps = driver.capabilities
    print(f"[DRV] Chrome {caps.get('browserVersion')} / Driver {caps.get('chrome',{}).get('chromedriverVersion','').split(' ')[0]}")
    
    return driver


def extract_content(driver, url):
    """기사 본문 및 발행일 추출"""
    try:
        driver.get(url)
        time.sleep(1.5)

        html = driver.page_source
        soup = BeautifulSoup(html, "lxml")

        content_text = None
        published_at = None

        # 네이버 뉴스인 경우
        if "news.naver.com" in url:
            # 발행일 추출
            date_elem = soup.select_one("span.media_end_head_info_datestamp_time")
            if date_elem:
                date_text = date_elem.get("data-date-time") or date_elem.get_text(strip=True)
                try:
                    # ISO 형식으로 변환
                    from dateutil import parser
                    dt = parser.parse(date_text)
                    published_at = dt.strftime("%a, %d %b %Y %H:%M:%S GMT")
                except:
                    published_at = date_text
            
            # 본문 추출
            content_area = soup.find("article", id="dic_area")
            if not content_area:
                content_area = soup.find("div", id="articleBodyContents")
            
            if content_area:
                for tag in content_area(['script', 'style', 'em', 'strong']):
                    tag.decompose()
                
                text = content_area.get_text(" ", strip=True)
                text = re.sub(r'\s+', ' ', text).strip()
                text = re.sub(r'// flash.*', '', text)
                text = re.sub(r'무단전재.*', '', text)
                text = re.sub(r'ⓒ.*', '', text)
                
                if len(text) >= 100:
                    content_text = text
        
        # 일반 언론사 사이트
        if not content_text:
            for tag in soup(['script', 'style', 'nav', 'header', 'footer', 'aside']):
                tag.decompose()

            content_area = soup.find("article")
            if not content_area:
                content_area = soup.find("main")
            
            if content_area:
                text = content_area.get_text(" ", strip=True)
            else:
                paragraphs = soup.find_all("p")
                text = " ".join(p.get_text(" ", strip=True) for p in paragraphs)
            
            text = re.sub(r'\s+', ' ', text).strip()

            if len(text) >= 100:
                content_text = text
        
        return content_text, published_at
    except Exception as e:
        return None, None


# 네이버 뉴스 언론사 코드 매핑 (주요 언론사)
NAVER_PRESS_CODE = {
    "001": "연합뉴스", "003": "뉴시스", "005": "국민일보", "008": "머니투데이",
    "009": "매일경제", "011": "서울경제", "014": "파이낸셜뉴스", "015": "한국경제",
    "016": "헤럴드경제", "018": "이데일리", "020": "동아일보", "021": "문화일보",
    "022": "세계일보", "023": "조선일보", "025": "중앙일보", "028": "한겨레",
    "032": "경향신문", "038": "한국일보", "047": "오마이뉴스", "052": "YTN",
    "055": "SBS", "056": "MBC", "057": "MBN", "214": "MBN",
    "081": "서울신문", "082": "부산일보", "083": "매일신문", "084": "국제신문",
    "087": "강원일보", "088": "전북일보", "092": "동아일보",
    "119": "데일리안", "123": "조세일보", "138": "디지털타임스", "243": "이코노미스트",
    "277": "아시아경제", "293": "블로터", "366": "조선비즈", "374": "SBS Biz",
    "417": "머니S", "421": "뉴스1", "422": "연합인포맥스", "449": "채널A",
    "629": "더팩트", "648": "비즈워치", "654": "NSP통신",
}


def extract_source(url: str) -> str:
    """URL에서 언론사명 추출"""
    try:
        # 네이버 뉴스인 경우
        if "news.naver.com" in url:
            parts = url.split("/")
            if "article" in parts or "mnews/article" in url:
                # URL 패턴: .../article/629/0000445887 또는 .../mnews/article/629/...
                for i, part in enumerate(parts):
                    if part in ["article"] and i + 1 < len(parts):
                        press_code = parts[i + 1]
                        return NAVER_PRESS_CODE.get(press_code, f"press_{press_code}")
        
        # 일반 언론사 사이트
        host = (urlparse(url).hostname or "").lower()
        if host.startswith("www."):
            host = host[4:]
        
        # 도메인별 매핑
        domain_map = {
            "chosun": "조선일보",
            "donga": "동아일보",
            "joongang": "중앙일보",
            "hankyung": "한국경제",
            "mk": "매일경제",
            "edaily": "이데일리",
            "fnnews": "파이낸셜뉴스",
            "asiae": "아시아경제",
            "sedaily": "서울경제",
            "heraldcorp": "헤럴드경제",
            "mt": "머니투데이",
        }
        
        labels = [p for p in host.split(".") if p]
        if len(labels) >= 2:
            domain_key = labels[-2]
            return domain_map.get(domain_key, domain_key)
        
        return host
    except:
        return "Unknown"


def crawl_economy_section(driver, target_count=TARGET_COUNT):
    """네이버 경제 섹션에서 기사 URL 수집"""
    print("\n" + "="*60)
    print("📰 기사 URL 수집 시작")
    print("="*60)
    
    driver.get(SECTION_URL)
    time.sleep(3)

    seen_urls = set()
    articles = []

    click_try = 0
    max_clicks = 200
    no_new_rounds = 0

    while len(articles) < target_count and click_try < max_clicks:
        html = driver.page_source
        soup = BeautifulSoup(html, "lxml")

        # 기사 블록: li.sa_item
        blocks = soup.select("li.sa_item")

        before = len(articles)

        for li in blocks:
            a = li.select_one("a.sa_text_title")
            if not a:
                continue

            url = a.get("href")
            title = a.get_text(strip=True)

            if not url or not title:
                continue

            # 절대 URL 보정
            if url.startswith("//"):
                url = "https:" + url

            if url in seen_urls:
                continue

            seen_urls.add(url)
            articles.append({
                "title": title,
                "url": url,
                "order": len(articles) + 1,
            })

        added = len(articles) - before

        if (click_try + 1) % 10 == 0 or click_try == 0:
            print(f"[진행] 총 {len(articles)}개 (이번 루프: +{added}개)")

        if len(articles) >= target_count:
            break

        if added == 0:
            no_new_rounds += 1
        else:
            no_new_rounds = 0

        # 더보기 버튼 클릭 시도
        if no_new_rounds >= 2:
            print("새로 추가된 기사가 없어 종료")
            break

        try:
            btn = driver.find_element(
                By.CSS_SELECTOR,
                "a.section_more_inner._CONTENT_LIST_LOAD_MORE_BUTTON"
            )
            driver.execute_script("arguments[0].scrollIntoView(true);", btn)
            time.sleep(0.5)

            try:
                btn.click()
            except Exception:
                driver.execute_script("arguments[0].click();", btn)

            click_try += 1
            time.sleep(2.0)

        except Exception as e:
            print(f"[경고] 더보기 버튼 클릭 실패: {repr(e)[:60]}")
            break

    print(f"\n✓ URL 수집 완료: {len(articles)}개")
    return articles


def extract_all_contents(driver, articles, max_count=1000):
    """모든 기사의 본문 추출 (최대 개수 제한)"""
    print("\n" + "="*60)
    print(f"📝 본문 추출 시작 (목표: {max_count}개, 수집: {len(articles)}개)")
    print("="*60)
    
    results = []
    failed_count = 0
    
    for idx, article_info in enumerate(articles):
        # 목표 개수 도달 시 중단
        if len(results) >= max_count:
            print(f"\n✓ 목표 개수({max_count}개) 도달, 추출 종료")
            break
        
        if (idx + 1) % 50 == 0:
            print(f"[{idx+1}/{len(articles)}] 진행 중... (성공: {len(results)}, 실패: {failed_count})")
        
        try:
            content, published_at = extract_content(driver, article_info['url'])
            
            if content and len(content) >= 100:
                fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
                
                article_data = {
                    "title": article_info['title'],
                    "url": article_info['url'],
                    "source": extract_source(article_info['url']),
                    "section": "경제",
                    "fetched_at": fetched_at.isoformat(),
                    "content": content,
                    "content_length": len(content),
                }
                
                # published_at이 있으면 추가
                if published_at:
                    article_data["published_at"] = published_at
                
                results.append(article_data)
            else:
                failed_count += 1
        except Exception as e:
            failed_count += 1
            continue
        
        time.sleep(0.3)
    
    print(f"\n✓ 본문 추출 완료: {len(results)}개 성공 / {failed_count}개 실패")
    return results, failed_count


def upload_to_s3(local_file_path, date_obj):
    """S3에 파일 업로드 (파티션 구조: year/month/day)"""
    if not s3_client:
        print("\n⚠️ boto3가 설치되지 않아 S3 업로드를 건너뜁니다.")
        return False
    
    try:
        year = date_obj.strftime("%Y")
        month = str(int(date_obj.strftime('%m')))
        day = str(int(date_obj.strftime('%d')))
        
        s3_key = f"news-articles/year={year}/month={month}/day={day}/multi_section_top100.json"
        
        print(f"\n📤 S3 업로드 중...")
        print(f"   s3://{S3_BUCKET_NAME}/{s3_key}")
        
        s3_client.upload_file(
            local_file_path, 
            S3_BUCKET_NAME, 
            s3_key,
            ExtraArgs={'ContentType': 'application/json'}
        )
        
        print(f"✅ S3 업로드 완료!")
        return True
        
    except ClientError as e:
        print(f"❌ S3 업로드 실패: {e}")
        return False
    except Exception as e:
        print(f"❌ S3 업로드 중 에러: {e}")
        return False


def main():
    import sys
    try:
        sys.stdout.reconfigure(line_buffering=True)
    except Exception:
        pass

    start_time = time.time()
    start_datetime = datetime.now(tz.gettz("Asia/Seoul"))

    print("="*60)
    print("네이버 뉴스 경제 섹션 크롤러")
    print(f"Started at: {start_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Target: {TARGET_COUNT}개 기사")
    print("="*60)

    driver = setup_driver()

    try:
        # 1단계: 기사 URL 수집
        articles = crawl_economy_section(driver, target_count=TARGET_COUNT)
        
        # 2단계: 본문 추출
        results, failed_count = extract_all_contents(driver, articles, max_count=TARGET_COUNT)
        
    finally:
        driver.quit()
        print("\n🔒 브라우저 종료")

    # 종료 시간
    end_time = time.time()
    end_datetime = datetime.now(tz.gettz("Asia/Seoul"))
    elapsed_seconds = end_time - start_time
    elapsed_minutes = elapsed_seconds / 60

    # 저장
    fetched_at = datetime.now(tz.gettz("Asia/Seoul"))
    date_folder = fetched_at.strftime("%Y%m%d")
    os.makedirs(f"articles/{date_folder}", exist_ok=True)
    
    # 파일명: multi_section_top100.json
    out_filename = "multi_section_top100.json"
    out_path = f"articles/{date_folder}/{out_filename}"

    final_output = {
        "metadata": {
            "start_time": start_datetime.isoformat(),
            "end_time": end_datetime.isoformat(),
            "elapsed_seconds": round(elapsed_seconds, 2),
            "elapsed_minutes": round(elapsed_minutes, 2),
            "total_target": TARGET_COUNT,
            "urls_collected": len(articles),
            "contents_success": len(results),
            "contents_failed": failed_count,
            "section": "경제",
            "source": "네이버 뉴스 섹션"
        },
        "articles": results
    }

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(final_output, f, ensure_ascii=False, indent=2)

    print("\n" + "="*60)
    print("📊 최종 결과")
    print("="*60)
    print(f"시작: {start_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"종료: {end_datetime.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"소요 시간: {elapsed_minutes:.2f}분 ({elapsed_seconds:.2f}초)")
    print(f"\n📈 결과:")
    print(f"  URL 수집: {len(articles)}개")
    print(f"  본문 성공: {len(results)}개")
    print(f"  본문 실패: {failed_count}개")
    print(f"\n🎯 최종 수집: {len(results)}개")
    print(f"💾 로컬 저장: {out_path}")
    
    # S3 업로드 (파티션 구조)
    upload_to_s3(out_path, fetched_at)
    
    print("="*60)
    
    return final_output


if __name__ == "__main__":
    main()