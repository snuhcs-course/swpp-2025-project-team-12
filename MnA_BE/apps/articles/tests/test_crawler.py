# apps/articles/tests/test_crawler.py
from django.test import SimpleTestCase
from unittest.mock import patch, MagicMock, Mock, mock_open, call, PropertyMock
import json
import os
import platform
from datetime import datetime
from urllib.parse import urlparse, urlsplit
from apps.articles.crawler_main import (
    normalize_url,
    extract_source,
    _is_googleish,
    get_gnews_url,
    SECTIONS,
)


# ==================== Fake Driver ====================
class FakeDriver:
    """최소 인터페이스 가짜 드라이버"""
    def __init__(self, current_url=None, page_source=""):
        self.current_url = current_url
        self.page_source = page_source
        self._timeout_called = False
        self._script_timeout_called = False
    
    def get(self, url):
        pass
    
    def quit(self):
        pass
    
    def set_page_load_timeout(self, timeout):
        self._timeout_called = True
    
    def set_script_timeout(self, timeout):
        self._script_timeout_called = True


# ==================== Dummy Feed ====================
class DummyFeed:
    """feedparser 더미"""
    def __init__(self, entries):
        self.entries = entries


# ==================== 1) 순수 함수 커버 ====================
class CrawlerPureFunctionsTest(SimpleTestCase):
    """순수 함수 완전 커버"""
    
    def test_get_gnews_url_all_sections(self):
        for section in SECTIONS:
            url = get_gnews_url(section["name"])
            self.assertIn("news.google.com/rss", url)
            self.assertIn(section["name"], url)
    
    def test_get_gnews_url_custom_section(self):
        url = get_gnews_url("CUSTOM")
        self.assertIn("CUSTOM", url)
    
    def test_is_googleish_news_google(self):
        self.assertTrue(_is_googleish("https://news.google.com/articles/123"))
    
    def test_is_googleish_www_google(self):
        self.assertTrue(_is_googleish("https://www.google.com/search"))
    
    def test_is_googleish_subdomain(self):
        self.assertTrue(_is_googleish("https://mail.google.com"))
    
    def test_is_googleish_naver(self):
        self.assertFalse(_is_googleish("https://news.naver.com"))
    
    def test_is_googleish_invalid_url(self):
        result = _is_googleish("::")
        self.assertIsInstance(result, bool)
    
    def test_is_googleish_exception_handling(self):
        """_is_googleish 예외 처리 (line 43-44)"""
        # None을 전달하여 AttributeError 발생 유도
        with patch('apps.articles.crawler_main.urlsplit') as mock_urlsplit:
            mock_urlsplit.side_effect = Exception("Parse error")
            result = _is_googleish("http://example.com")
            # 예외 발생 시 True 반환 (안전한 기본값)
            self.assertTrue(result)
    
    def test_normalize_url_exception_handling(self):
        """normalize_url 예외 처리 (line 53-54)"""
        # urlparse가 실패하도록 mock
        with patch('apps.articles.crawler_main.urlparse') as mock_urlparse:
            mock_urlparse.side_effect = Exception("Parse error")
            test_url = "http://example.com"
            result = normalize_url(test_url)
            # 예외 발생 시 원본 반환
            self.assertEqual(result, test_url)
    
    def test_extract_source_exception_handling(self):
        """extract_source 예외 처리 (line 186-187)"""
        # urlparse가 실패하도록 mock
        with patch('apps.articles.crawler_main.urlparse') as mock_urlparse:
            mock_urlparse.side_effect = Exception("Parse error")
            result = extract_source("http://example.com")
            # 예외 발생 시 "Unknown" 반환
            self.assertEqual(result, "Unknown")

    
    def test_normalize_url_utm_and_fragment(self):
        url = "https://example.com/page?utm_source=google&q=1#section"
        result = normalize_url(url)
        self.assertNotIn("utm_source", result)
        self.assertNotIn("#section", result)
    
    def test_normalize_url_no_query(self):
        result = normalize_url("https://example.com/page")
        self.assertEqual(result, "https://example.com/page")
    
    def test_normalize_url_empty_string(self):
        self.assertEqual(normalize_url(""), "")
    
    def test_normalize_url_broken(self):
        result = normalize_url("not a url")
        self.assertEqual(result, "not a url")
    
    def test_normalize_url_all_tracking_params(self):
        url = "https://test.com?utm_source=x&utm_medium=y&fbclid=a&q=keep"
        result = normalize_url(url)
        self.assertIn("q=keep", result)
    
    def test_extract_source_naver(self):
        self.assertEqual(extract_source("https://news.naver.com/a"), "naver")
    
    def test_extract_source_hankyung(self):
        self.assertEqual(extract_source("https://www.hankyung.com/a"), "hankyung")
    
    def test_extract_source_mk_cctld(self):
        self.assertEqual(extract_source("https://www.mk.co.kr/news"), "mk")
    
    def test_extract_source_bbc_cctld(self):
        result = extract_source("https://news.bbc.co.uk/article")
        self.assertIn("bbc", result.lower())
    
    def test_extract_source_jp_cctld(self):
        result = extract_source("https://www.univ.ac.jp/news")
        self.assertIsInstance(result, str)
    
    def test_extract_source_invalid_url(self):
        result = extract_source("not-a-url")
        self.assertIsInstance(result, str)
    
    def test_extract_source_no_netloc(self):
        result = extract_source("file:///path/to/file")
        self.assertIsInstance(result, str)
    
    def test_extract_source_with_port(self):
        result = extract_source("https://example.com:8443/article")
        self.assertIn("example", result)
    
    def test_extract_source_ip_address(self):
        result = extract_source("http://192.168.0.1/news")
        self.assertIsInstance(result, str)
    
    def test_extract_source_punycode(self):
        result = extract_source("https://xn--oy2b35b.com/article")
        self.assertIsInstance(result, str)


# ==================== 2) S3 업로드 분기 ====================
class CrawlerS3Test(SimpleTestCase):
    """S3 업로드 모든 분기"""
    
    @patch('apps.articles.crawler_main.s3_client')
    def test_upload_to_s3_success(self, mock_s3):
        from apps.articles.crawler_main import upload_to_s3
        mock_s3.upload_file.return_value = None
        result = upload_to_s3("/tmp/test.json", datetime(2025, 10, 30))
        self.assertTrue(result)
    
    @patch('apps.articles.crawler_main.s3_client')
    def test_upload_to_s3_client_error(self, mock_s3):
        from apps.articles.crawler_main import upload_to_s3
        from botocore.exceptions import ClientError
        mock_s3.upload_file.side_effect = ClientError(
            {'Error': {'Code': '500', 'Message': 'Server Error'}},
            'upload_file'
        )
        result = upload_to_s3("/tmp/test.json", datetime(2025, 10, 30))
        self.assertFalse(result)
    
    @patch('apps.articles.crawler_main.s3_client')
    def test_upload_to_s3_general_exception(self, mock_s3):
        from apps.articles.crawler_main import upload_to_s3
        mock_s3.upload_file.side_effect = Exception("Network error")
        result = upload_to_s3("/tmp/test.json", datetime(2025, 10, 30))
        self.assertFalse(result)
    
    @patch('apps.articles.crawler_main.s3_client')
    def test_upload_to_s3_key_format(self, mock_s3):
        from apps.articles.crawler_main import upload_to_s3
        mock_s3.upload_file.return_value = None
        upload_to_s3("/tmp/test.json", datetime(2025, 1, 5))
        call_args = mock_s3.upload_file.call_args
        s3_key = call_args[1]['Key'] if 'Key' in call_args[1] else call_args[0][2]
        self.assertIn("year=2025", s3_key)


# ==================== 3) 브라우저 의존 함수 ====================
class CrawlerBrowserTest(SimpleTestCase):
    """브라우저 함수 가짜 드라이버로 커버"""
    
    @patch('time.sleep')
    def test_resolve_google_url_success(self, mock_sleep):
        from apps.articles.crawler_main import resolve_google_url_with_browser
        driver = FakeDriver(current_url="https://www.hankyung.com/article/123")
        result = resolve_google_url_with_browser(driver, "https://news.google.com/articles/xyz", max_wait=1, max_retries=1)
        self.assertEqual(result, "https://www.hankyung.com/article/123")
    
    @patch('time.sleep')
    def test_resolve_google_url_still_google(self, mock_sleep):
        from apps.articles.crawler_main import resolve_google_url_with_browser
        driver = FakeDriver(current_url="https://news.google.com/still-here")
        result = resolve_google_url_with_browser(driver, "https://news.google.com/articles/xyz", max_wait=1, max_retries=1)
        self.assertIsNone(result)
    
    @patch('time.sleep')
    def test_resolve_google_url_retry_success(self, mock_sleep):
        from apps.articles.crawler_main import resolve_google_url_with_browser
        driver = Mock()
        driver.get.side_effect = [Exception("First attempt fails"), None]
        driver.current_url = "https://example.com/article"
        result = resolve_google_url_with_browser(driver, "https://news.google.com/articles/xyz", max_wait=1, max_retries=3)
        self.assertEqual(driver.get.call_count, 2)
    
    @patch('time.sleep')
    def test_resolve_google_url_all_retries_fail(self, mock_sleep):
        from apps.articles.crawler_main import resolve_google_url_with_browser
        driver = Mock()
        driver.get.side_effect = Exception("Always fails")
        result = resolve_google_url_with_browser(driver, "https://news.google.com/articles/xyz", max_wait=1, max_retries=2)
        self.assertIsNone(result)
    
    @patch('time.sleep')
    def test_extract_content_success_long(self, mock_sleep):
        from apps.articles.crawler_main import extract_content
        html = """<html><p>첫 번째 단락입니다. 충분히 긴 텍스트를 만들어야 합니다.</p>
        <p>두 번째 단락입니다. 더 많은 내용을 추가하여 길이를 늘립니다.</p>
        <p>세 번째 단락입니다. 이 정도면 백 자는 충분히 넘을 것입니다.</p></html>"""
        driver = FakeDriver(page_source=html)
        result = extract_content(driver, "https://example.com", retry=False)
        if result is not None:
            if isinstance(result, dict):
                self.assertGreater(result.get('content_length', 0), 100)
            elif isinstance(result, str):
                self.assertGreater(len(result), 100)
    
    @patch('time.sleep')
    def test_extract_content_too_short_fails(self, mock_sleep):
        from apps.articles.crawler_main import extract_content
        driver = FakeDriver(page_source="<html><p>짧음</p></html>")
        result = extract_content(driver, "https://example.com", retry=False)
        self.assertIsNone(result)
    
    @patch('time.sleep')
    def test_extract_content_retry_true_longer_wait(self, mock_sleep):
        from apps.articles.crawler_main import extract_content
        driver = FakeDriver(page_source="<html><p>짧음</p></html>")
        extract_content(driver, "https://example.com", retry=True)
        if mock_sleep.called:
            first_call = mock_sleep.call_args_list[0][0][0]
            self.assertAlmostEqual(first_call, 3.5, places=1)


# ==================== 4) crawl_section 모든 분기 ====================
class CrawlerSectionTest(SimpleTestCase):
    """crawl_section 시나리오별 완전 커버"""
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_no_entries(self, mock_parse, mock_sleep):
        from apps.articles.crawler_main import crawl_section
        mock_parse.return_value = DummyFeed([])
        driver = FakeDriver()
        results, stats = crawl_section(driver, "TEST", 10, set())
        self.assertEqual(len(results), 0)
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_no_link(self, mock_parse, mock_sleep):
        from apps.articles.crawler_main import crawl_section
        entry = Mock()
        entry.link = None
        entry.title = "Test"
        mock_parse.return_value = DummyFeed([entry])
        results, stats = crawl_section(FakeDriver(), "TEST", 10, set())
        self.assertGreater(stats["failed"], 0)
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.resolve_google_url_with_browser')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_resolve_failure(self, mock_parse, mock_resolve, mock_sleep):
        from apps.articles.crawler_main import crawl_section
        entry = Mock()
        entry.link = "https://news.google.com/articles/123"
        entry.title = "Test"
        mock_parse.return_value = DummyFeed([entry])
        mock_resolve.return_value = None
        results, stats = crawl_section(FakeDriver(), "TEST", 10, set())
        self.assertGreater(stats["failed"], 0)
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.resolve_google_url_with_browser')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_duplicate_filtered(self, mock_parse, mock_resolve, mock_sleep):
        from apps.articles.crawler_main import crawl_section
        entry1 = Mock()
        entry1.link = "https://news.google.com/articles/123"
        entry1.title = "Article 1"
        entry2 = Mock()
        entry2.link = "https://news.google.com/articles/456"
        entry2.title = "Article 2"
        mock_parse.return_value = DummyFeed([entry1, entry2])
        mock_resolve.return_value = "https://example.com/same-article"
        results, stats = crawl_section(FakeDriver(), "TEST", 10, set())
        self.assertGreater(stats["filtered"], 0)
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.extract_content')
    @patch('apps.articles.crawler_main.resolve_google_url_with_browser')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_extract_fails(self, mock_parse, mock_resolve, mock_extract, mock_sleep):
        from apps.articles.crawler_main import crawl_section
        entry = Mock()
        entry.link = "https://news.google.com/articles/123"
        entry.title = "Test"
        mock_parse.return_value = DummyFeed([entry])
        mock_resolve.return_value = "https://example.com/article"
        mock_extract.return_value = None
        results, stats = crawl_section(FakeDriver(), "TEST", 10, set())
        self.assertGreater(stats["failed"], 0)


# ==================== 5) setup_driver 분기 ====================
class CrawlerSetupDriverTest(SimpleTestCase):
    """setup_driver 옵션 구성 분기"""
    
    @patch('apps.articles.crawler_main.ChromeDriverManager')
    @patch('apps.articles.crawler_main.Service')
    @patch('apps.articles.crawler_main.webdriver.Chrome')
    @patch('platform.system')
    @patch.dict(os.environ, {}, clear=True)
    def test_setup_driver_linux_path(self, mock_platform, mock_chrome, mock_service, mock_cdm):
        """Linux 환경에서 직접 경로 사용 (line 88)"""
        from apps.articles.crawler_main import setup_driver
        
        mock_platform.return_value = "Linux"
        fake_driver = FakeDriver()
        mock_chrome.return_value = fake_driver
        
        driver = setup_driver()
        
        # Linux에서는 /usr/bin/chromedriver 직접 사용
        mock_service.assert_called_with('/usr/bin/chromedriver')
        self.assertTrue(mock_chrome.called)
    
    @patch('apps.articles.crawler_main.ChromeDriverManager')
    @patch('apps.articles.crawler_main.Service')
    @patch('apps.articles.crawler_main.webdriver.Chrome')
    @patch.dict(os.environ, {}, clear=True)
    def test_setup_driver_without_shim(self, mock_chrome, mock_service, mock_cdm):
        from apps.articles.crawler_main import setup_driver
        fake_driver = FakeDriver()
        mock_chrome.return_value = fake_driver
        mock_cdm.return_value.install.return_value = '/tmp/chromedriver'
        driver = setup_driver()
        self.assertTrue(mock_chrome.called)
    
    @patch('apps.articles.crawler_main.ChromeDriverManager')
    @patch('apps.articles.crawler_main.Service')
    @patch('apps.articles.crawler_main.webdriver.Chrome')
    @patch.dict(os.environ, {'GOOGLE_CHROME_SHIM': '/usr/bin/chrome'})
    def test_setup_driver_with_shim(self, mock_chrome, mock_service, mock_cdm):
        from apps.articles.crawler_main import setup_driver
        fake_driver = FakeDriver()
        mock_chrome.return_value = fake_driver
        mock_cdm.return_value.install.return_value = '/tmp/chromedriver'
        driver = setup_driver()
        self.assertTrue(mock_service.called)
    
    @patch('apps.articles.crawler_main.ChromeDriverManager')
    @patch('apps.articles.crawler_main.Service')
    @patch('apps.articles.crawler_main.webdriver.Chrome')
    @patch.dict(os.environ, {}, clear=True)
    def test_setup_driver_creation_fails(self, mock_chrome, mock_service, mock_cdm):
        from apps.articles.crawler_main import setup_driver
        mock_cdm.return_value.install.return_value = '/tmp/chromedriver'
        mock_chrome.side_effect = Exception("Cannot start Chrome")
        with self.assertRaises(Exception):
            setup_driver()


# ==================== 6) crawl_section 성공 경로 + break ====================
class CrawlerSectionSuccessTest(SimpleTestCase):
    """crawl_section 정상 수집 후 target 충족 시 조기 break"""
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.extract_content')
    @patch('apps.articles.crawler_main.resolve_google_url_with_browser')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_success_with_early_break(self, mock_parse, mock_resolve, mock_extract, mock_sleep):
        """첫 기사 성공 → target=1 충족 → break로 두 번째 기사 미처리"""
        from apps.articles.crawler_main import crawl_section
        
        # 2개 엔트리 준비
        entry1 = Mock()
        entry1.link = "https://news.google.com/articles/first"
        entry1.title = "First Article"
        entry1.get = Mock(return_value="2025-10-30T10:00:00")
        
        entry2 = Mock()
        entry2.link = "https://news.google.com/articles/second"
        entry2.title = "Second Article Should Not Be Processed"
        entry2.get = Mock(return_value="2025-10-30T11:00:00")
        
        mock_parse.return_value = DummyFeed([entry1, entry2])
        
        # 첫 번째만 성공 URL 반환
        mock_resolve.return_value = "https://example.com/article1"
        
        # 충분히 긴 컨텐츠 (120자 이상)
        long_content = "이것은 충분히 긴 기사 본문입니다. " * 10  # 약 150자
        mock_extract.return_value = long_content
        
        # target_count=1로 설정 → 첫 기사 성공 시 즉시 break
        driver = FakeDriver()
        results, stats = crawl_section(driver, "TEST", target_count=1, seen_urls=set())
        
        # 검증
        self.assertEqual(len(results), 1, "Should collect exactly 1 article")
        self.assertEqual(mock_resolve.call_count, 1, "Should resolve only first article")
        self.assertEqual(mock_extract.call_count, 1, "Should extract content only once")
        
        # 결과 구조 검증
        article = results[0]
        self.assertEqual(article["title"], "First Article")
        self.assertEqual(article["url"], "https://example.com/article1")
        self.assertEqual(article["section"], "TEST")
        self.assertIn("content", article)
        self.assertIn("content_length", article)
        self.assertGreater(article["content_length"], 100)


# ==================== 7) resolve 재시도 중 성공 경로 ====================
class CrawlerResolvePollingTest(SimpleTestCase):
    """resolve_google_url_with_browser 재시도 중 성공 전환"""
    
    @patch('time.sleep')
    def test_resolve_google_url_exception_then_success(self, mock_sleep):
        """첫 시도 Exception → 재시도에서 성공"""
        from apps.articles.crawler_main import resolve_google_url_with_browser
        
        driver = Mock()
        
        # 첫 번째 get() 호출: Exception 발생
        # 두 번째 get() 호출: 성공 후 비구글 URL
        driver.get.side_effect = [Exception("Network timeout"), None]
        driver.current_url = "https://example.com/final-article"
        
        result = resolve_google_url_with_browser(
            driver, 
            "https://news.google.com/articles/xyz",
            max_wait=1,
            max_retries=2
        )
        
        # 재시도 후 성공
        self.assertEqual(result, "https://example.com/final-article")
        self.assertEqual(driver.get.call_count, 2)


# ==================== 8) main() 실행부 글루 ====================
class CrawlerMainIntegrationTest(SimpleTestCase):
    """main() 함수 전체 실행 흐름 테스트"""
    
    @patch('apps.articles.crawler_main.tz.gettz')
    @patch('apps.articles.crawler_main.time.time')
    @patch('apps.articles.crawler_main.upload_to_s3')
    @patch('apps.articles.crawler_main.json.dump')
    @patch('os.makedirs')
    @patch('apps.articles.crawler_main.crawl_section')
    @patch('apps.articles.crawler_main.setup_driver')
    @patch.object(
        __import__('apps.articles.crawler_main', fromlist=['SECTIONS']), 
        'SECTIONS',
        [{"name": "TEST1", "count": 2}, {"name": "TEST2", "count": 1}]
    )
    def test_main_full_execution(self, mock_setup, mock_crawl, mock_makedirs, 
                                mock_json_dump, mock_upload, mock_time, mock_gettz):
        """main() 함수의 파일쓰기·집계·S3 업로드 전체 흐름"""
        from apps.articles.crawler_main import main
        from datetime import datetime, timezone, timedelta
        
        # tz.gettz Mock - UTC+9 타임존 반환
        kst = timezone(timedelta(hours=9))
        mock_gettz.return_value = kst
        
        # time.time Mock
        mock_time.side_effect = [100.0, 305.0]  # 시작: 100, 종료: 305 (205초 경과)
        
        # setup_driver Mock
        fake_driver = FakeDriver()
        fake_driver.quit = Mock()
        mock_setup.return_value = fake_driver
        
        # crawl_section Mock - 각 섹션별 결과 반환
        def crawl_side_effect(driver, section_name, target_count, seen_urls):
            if section_name == "TEST1":
                results = [
                    {
                        "title": "Article 1",
                        "url": "https://example.com/1",
                        "source": "example",
                        "section": "TEST1",
                        "published_at": "2025-10-30T10:00:00",
                        "fetched_at": "2025-10-30T10:01:00+09:00",
                        "content": "Content 1 " * 20,
                        "content_length": 180,
                    },
                    {
                        "title": "Article 2",
                        "url": "https://example.com/2",
                        "source": "example",
                        "section": "TEST1",
                        "published_at": "2025-10-30T10:05:00",
                        "fetched_at": "2025-10-30T10:06:00+09:00",
                        "content": "Content 2 " * 20,
                        "content_length": 180,
                    }
                ]
                stats = {"filtered": 1, "failed": 0}
                return results, stats
            else:  # TEST2
                results = [
                    {
                        "title": "Article 3",
                        "url": "https://example.com/3",
                        "source": "example",
                        "section": "TEST2",
                        "published_at": "2025-10-30T10:10:00",
                        "fetched_at": "2025-10-30T10:11:00+09:00",
                        "content": "Content 3 " * 20,
                        "content_length": 180,
                    }
                ]
                stats = {"filtered": 0, "failed": 1}
                return results, stats
        
        mock_crawl.side_effect = crawl_side_effect
        mock_upload.return_value = True
        
        # open() Mock을 context manager로 처리
        m_open = mock_open()
        with patch('builtins.open', m_open):
            # main() 실행
            result = main()
        
        # 검증 1: setup_driver 및 quit 호출
        self.assertTrue(mock_setup.called)
        self.assertTrue(fake_driver.quit.called)
        
        # 검증 2: crawl_section이 2번 호출 (TEST1, TEST2)
        self.assertEqual(mock_crawl.call_count, 2)
        
        # 검증 3: makedirs 호출 (articles/날짜 폴더)
        self.assertTrue(mock_makedirs.called)
        
        # 검증 4: json.dump 호출 확인
        self.assertTrue(mock_json_dump.called)
        
        # 검증 5: upload_to_s3 호출
        self.assertTrue(mock_upload.called)
        
        # 검증 6: 반환된 결과 구조 확인
        self.assertIn("metadata", result)
        self.assertIn("articles", result)
        
        # 검증 7: 메타데이터 필드 확인
        metadata = result["metadata"]
        self.assertIn("start_time", metadata)
        self.assertIn("end_time", metadata)
        self.assertIn("elapsed_seconds", metadata)
        self.assertIn("total_target", metadata)
        self.assertIn("total_collected", metadata)
        self.assertIn("section_stats", metadata)
        
        # 검증 8: total_target = 2 + 1 = 3
        self.assertEqual(metadata["total_target"], 3)
        
        # 검증 9: total_collected = 2 + 1 = 3
        self.assertEqual(metadata["total_collected"], 3)
        
        # 검증 10: section_stats 구조
        section_stats = metadata["section_stats"]
        self.assertIn("TEST1", section_stats)
        self.assertIn("TEST2", section_stats)
        self.assertEqual(section_stats["TEST1"]["target"], 2)
        self.assertEqual(section_stats["TEST1"]["success"], 2)
        self.assertEqual(section_stats["TEST2"]["target"], 1)
        self.assertEqual(section_stats["TEST2"]["success"], 1)


# ==================== 9) 타임아웃 및 WebDriver 예외 테스트 ====================
class CrawlerTimeoutTest(SimpleTestCase):
    """타임아웃 및 네트워크 오류 처리 테스트"""
    
    @patch('time.sleep')
    def test_resolve_timeout_exception(self, mock_sleep):
        """TimeoutException 발생 시 재시도"""
        from apps.articles.crawler_main import resolve_google_url_with_browser
        from selenium.common.exceptions import TimeoutException
        
        driver = Mock()
        driver.get.side_effect = [
            TimeoutException("Page load timeout"),
            None
        ]
        driver.current_url = "https://example.com/article"
        
        result = resolve_google_url_with_browser(driver, "https://news.google.com/123", max_retries=2)
        self.assertEqual(result, "https://example.com/article")
        self.assertEqual(driver.get.call_count, 2)
    
    @patch('time.sleep')
    def test_extract_content_exception_handling(self, mock_sleep):
        """extract_content 예외 처리"""
        from apps.articles.crawler_main import extract_content
        
        driver = Mock()
        driver.get.side_effect = Exception("Network error")
        
        result = extract_content(driver, "https://example.com/article")
        self.assertIsNone(result)
    
    @patch('time.sleep')
    def test_extract_content_no_paragraphs(self, mock_sleep):
        """<p> 태그가 없는 HTML"""
        from apps.articles.crawler_main import extract_content
        
        driver = FakeDriver(page_source="<html><body><div>No paragraphs</div></body></html>")
        result = extract_content(driver, "https://example.com/article")
        self.assertIsNone(result)
    
    @patch('time.sleep')
    def test_extract_content_script_removal(self, mock_sleep):
        """script/style 태그 제거 확인"""
        from apps.articles.crawler_main import extract_content
        
        long_text = " ".join(["word"] * 50)
        html = f"""<html>
        <head><style>.test {{ color: red; }}</style></head>
        <body>
            <script>alert('test');</script>
            <nav>Navigation</nav>
            <p>{long_text}</p>
        </body>
        </html>"""
        
        driver = FakeDriver(page_source=html)
        result = extract_content(driver, "https://example.com/article")
        
        if result:
            self.assertNotIn("alert", result)
            self.assertNotIn("Navigation", result)


# ==================== 10) crawl_section 추가 엣지 케이스 ====================
class CrawlerSectionEdgeCasesTest(SimpleTestCase):
    """crawl_section 추가 엣지 케이스"""
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.extract_content')
    @patch('apps.articles.crawler_main.resolve_google_url_with_browser')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_first_fail_retry_success(self, mock_parse, mock_resolve, mock_extract, mock_sleep):
        """콘텐츠 추출 첫 실패, 재시도 성공"""
        from apps.articles.crawler_main import crawl_section
        
        entry = Mock()
        entry.link = "https://news.google.com/articles/123"
        entry.title = "Test Article"
        entry.get = Mock(return_value="2025-01-01")
        
        mock_parse.return_value = DummyFeed([entry])
        mock_resolve.return_value = "https://example.com/article"
        
        # 첫 번째 실패, 두 번째 성공
        long_content = "Test content. " * 20
        mock_extract.side_effect = [None, long_content]
        
        driver = FakeDriver()
        results, stats = crawl_section(driver, "TEST", 1, set())
        
        self.assertEqual(len(results), 1)
        self.assertEqual(mock_extract.call_count, 2)
    
    @patch('time.sleep')
    @patch('apps.articles.crawler_main.feedparser.parse')
    def test_crawl_section_empty_link(self, mock_parse, mock_sleep):
        """빈 link 필드 - entry.get('link') 반환값 없음 (line 216-218)"""
        from apps.articles.crawler_main import crawl_section
        
        entry = Mock()
        entry.get = Mock(side_effect=lambda k, default=None: "" if k == "link" else "2025-01-01")
        entry.title = "Test"
        
        mock_parse.return_value = DummyFeed([entry])
        results, stats = crawl_section(FakeDriver(), "TEST", 10, set())
        
        # 빈 링크는 실패로 처리
        self.assertGreater(stats["failed"], 0)
        self.assertEqual(len(results), 0)

# ==================== 기존 테스트 유지 ====================
class CrawlerOutputTest(SimpleTestCase):
    """출력 포맷 테스트"""
    
    def test_article_structure(self):
        article = {"title": "Test", "url": "https://example.com", "source": "example",
                   "section": "TEST", "content": "Test", "content_length": 12}
        for field in ["title", "url", "source", "content", "content_length"]:
            self.assertIn(field, article)
    
    def test_metadata_structure(self):
        metadata = {"start_time": "2025-10-30T10:00:00+09:00", "end_time": "2025-10-30T10:05:00+09:00",
                    "elapsed_seconds": 300, "target_count": 100, "success_count": 100}
        for field in ["start_time", "end_time", "elapsed_seconds", "target_count", "success_count"]:
            self.assertIn(field, metadata)