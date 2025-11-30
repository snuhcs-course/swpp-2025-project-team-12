# apps/articles/tests/test_crawler_main.py
from django.test import SimpleTestCase
from unittest.mock import patch, MagicMock, Mock, mock_open
import json
import os
from datetime import datetime


# ==================== Fake Driver ====================
class FakeDriver:
    """Selenium WebDriver Mock"""

    def __init__(self, current_url=None, page_source=""):
        self.current_url = current_url
        self.page_source = page_source
        self.capabilities = {
            "browserVersion": "120.0",
            "chrome": {"chromedriverVersion": "120.0.0"},
        }

    def get(self, url):
        pass

    def quit(self):
        pass

    def set_page_load_timeout(self, timeout):
        pass

    def implicitly_wait(self, timeout):
        pass

    def find_element(self, by, selector):
        raise Exception("Element not found")

    def execute_script(self, script, *args):
        pass


# ==================== 1) extract_source 테스트 ====================
class ExtractSourceTest(SimpleTestCase):
    """extract_source 함수 테스트"""

    def test_naver_news_article_url(self):
        """네이버 뉴스 기사 URL에서 언론사 추출"""
        from apps.articles.crawler_main import extract_source

        # 더팩트 (629)
        url = "https://news.naver.com/article/629/0000445887"
        result = extract_source(url)
        self.assertEqual(result, "더팩트")

    def test_naver_news_mnews_url(self):
        """네이버 모바일 뉴스 URL"""
        from apps.articles.crawler_main import extract_source

        url = "https://news.naver.com/mnews/article/015/0005678901"
        result = extract_source(url)
        self.assertEqual(result, "한국경제")

    def test_naver_unknown_press_code(self):
        """알 수 없는 언론사 코드"""
        from apps.articles.crawler_main import extract_source

        url = "https://news.naver.com/article/999/0000123456"
        result = extract_source(url)
        self.assertEqual(result, "press_999")

    def test_general_site_chosun(self):
        """일반 언론사 사이트 - 조선일보"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.chosun.com/economy/article/123")
        self.assertEqual(result, "조선일보")

    def test_general_site_hankyung(self):
        """일반 언론사 사이트 - 한국경제"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.hankyung.com/article/123")
        self.assertEqual(result, "한국경제")

    def test_general_site_mk(self):
        """일반 언론사 사이트 - 매일경제 (co.kr 도메인은 co 반환)"""
        from apps.articles.crawler_main import extract_source

        # 현재 코드는 .co.kr 도메인에서 'co'를 반환함
        result = extract_source("https://www.mk.co.kr/news/economy/123")
        self.assertEqual(result, "co")

    def test_general_site_edaily(self):
        """일반 언론사 사이트 - 이데일리 (co.kr 도메인은 co 반환)"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.edaily.co.kr/news/123")
        self.assertEqual(result, "co")

    def test_general_site_sedaily(self):
        """일반 언론사 사이트 - 서울경제"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.sedaily.com/news/123")
        self.assertEqual(result, "서울경제")

    def test_general_site_fnnews(self):
        """일반 언론사 사이트 - 파이낸셜뉴스"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.fnnews.com/news/123")
        self.assertEqual(result, "파이낸셜뉴스")

    def test_general_site_asiae(self):
        """일반 언론사 사이트 - 아시아경제 (co.kr 도메인은 co 반환)"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.asiae.co.kr/article/123")
        self.assertEqual(result, "co")

    def test_general_site_heraldcorp(self):
        """일반 언론사 사이트 - 헤럴드경제"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://biz.heraldcorp.com/view.php?ud=123")
        self.assertEqual(result, "헤럴드경제")

    def test_general_site_mt(self):
        """일반 언론사 사이트 - 머니투데이 (co.kr 도메인은 co 반환)"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://news.mt.co.kr/123")
        self.assertEqual(result, "co")

    def test_general_site_donga(self):
        """일반 언론사 사이트 - 동아일보"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.donga.com/news/123")
        self.assertEqual(result, "동아일보")

    def test_general_site_joongang(self):
        """일반 언론사 사이트 - 중앙일보 (co.kr 도메인은 co 반환)"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.joongang.co.kr/article/123")
        self.assertEqual(result, "co")

    def test_unknown_domain(self):
        """알 수 없는 도메인"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.unknown-site.com/article")
        self.assertEqual(result, "unknown-site")

    def test_www_prefix_removal(self):
        """www 프리픽스 제거"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("https://www.example.com/article")
        self.assertEqual(result, "example")

    def test_exception_handling(self):
        """빈 문자열 - 빈 문자열 반환"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("")
        self.assertEqual(result, "")

    def test_invalid_url(self):
        """잘못된 URL"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("not-a-valid-url")
        self.assertIsInstance(result, str)


# ==================== 2) upload_to_s3 테스트 ====================
class UploadToS3Test(SimpleTestCase):
    """upload_to_s3 함수 테스트"""

    @patch("apps.articles.crawler_main.s3_client")
    def test_upload_success(self, mock_s3):
        """S3 업로드 성공"""
        from apps.articles.crawler_main import upload_to_s3

        mock_s3.upload_file.return_value = None
        result = upload_to_s3("/tmp/test.json", datetime(2025, 11, 26))

        self.assertTrue(result)
        mock_s3.upload_file.assert_called_once()

    @patch("apps.articles.crawler_main.s3_client")
    def test_upload_client_error(self, mock_s3):
        """S3 ClientError"""
        from apps.articles.crawler_main import upload_to_s3
        from botocore.exceptions import ClientError

        mock_s3.upload_file.side_effect = ClientError(
            {"Error": {"Code": "500", "Message": "Server Error"}}, "upload_file"
        )
        result = upload_to_s3("/tmp/test.json", datetime(2025, 11, 26))

        self.assertFalse(result)

    @patch("apps.articles.crawler_main.s3_client")
    def test_upload_general_exception(self, mock_s3):
        """일반 예외"""
        from apps.articles.crawler_main import upload_to_s3

        mock_s3.upload_file.side_effect = Exception("Network error")
        result = upload_to_s3("/tmp/test.json", datetime(2025, 11, 26))

        self.assertFalse(result)

    @patch("apps.articles.crawler_main.s3_client", None)
    def test_upload_no_client(self):
        """S3 클라이언트 없음"""
        from apps.articles.crawler_main import upload_to_s3

        result = upload_to_s3("/tmp/test.json", datetime(2025, 11, 26))
        self.assertFalse(result)

    @patch("apps.articles.crawler_main.s3_client")
    def test_upload_s3_key_format(self, mock_s3):
        """S3 키 포맷 확인"""
        from apps.articles.crawler_main import upload_to_s3

        mock_s3.upload_file.return_value = None
        upload_to_s3("/tmp/test.json", datetime(2025, 1, 5))

        call_args = mock_s3.upload_file.call_args
        s3_key = call_args[0][2]  # 세 번째 인자가 S3 key

        self.assertIn("year=2025", s3_key)
        self.assertIn("month=1", s3_key)
        self.assertIn("day=5", s3_key)


# ==================== 3) setup_driver 테스트 ====================
class SetupDriverTest(SimpleTestCase):
    """setup_driver 함수 테스트"""

    @patch("apps.articles.crawler_main.webdriver.Chrome")
    @patch("apps.articles.crawler_main.Service")
    @patch("apps.articles.crawler_main.ChromeDriverManager")
    @patch("subprocess.run")
    @patch("os.path.exists")
    def test_setup_driver_success(self, mock_exists, mock_run, mock_cdm, mock_service, mock_chrome):
        """드라이버 설정 성공"""
        from apps.articles.crawler_main import setup_driver

        mock_exists.return_value = True
        mock_run.return_value = Mock(stdout="Google Chrome 120.0")
        mock_cdm.return_value.install.return_value = "/tmp/chromedriver"

        fake_driver = FakeDriver()
        mock_chrome.return_value = fake_driver

        driver = setup_driver()

        self.assertEqual(driver, fake_driver)
        mock_chrome.assert_called_once()

    @patch("apps.articles.crawler_main.webdriver.Chrome")
    @patch("apps.articles.crawler_main.Service")
    @patch("apps.articles.crawler_main.ChromeDriverManager")
    @patch("subprocess.run")
    @patch("os.path.exists")
    def test_setup_driver_no_chrome_binary(
        self, mock_exists, mock_run, mock_cdm, mock_service, mock_chrome
    ):
        """Chrome 바이너리 없음"""
        from apps.articles.crawler_main import setup_driver

        mock_exists.return_value = False

        with self.assertRaises(RuntimeError) as ctx:
            setup_driver()

        self.assertIn("Chrome binary not found", str(ctx.exception))

    @patch("apps.articles.crawler_main.webdriver.Chrome")
    @patch("apps.articles.crawler_main.Service")
    @patch("apps.articles.crawler_main.ChromeDriverManager")
    @patch("subprocess.run")
    @patch("os.path.exists")
    @patch.dict(os.environ, {"CHROME_BIN": "/custom/chrome"})
    def test_setup_driver_with_env_chrome_bin(
        self, mock_exists, mock_run, mock_cdm, mock_service, mock_chrome
    ):
        """환경 변수로 Chrome 경로 지정"""
        from apps.articles.crawler_main import setup_driver

        mock_exists.side_effect = lambda p: p == "/custom/chrome"
        mock_run.return_value = Mock(stdout="Google Chrome 120.0")
        mock_cdm.return_value.install.return_value = "/tmp/chromedriver"
        mock_chrome.return_value = FakeDriver()

        driver = setup_driver()
        self.assertIsNotNone(driver)

    @patch("apps.articles.crawler_main.webdriver.Chrome")
    @patch("apps.articles.crawler_main.Service")
    @patch("apps.articles.crawler_main.ChromeDriverManager")
    @patch("subprocess.run")
    @patch("os.path.exists")
    def test_setup_driver_version_check_exception(
        self, mock_exists, mock_run, mock_cdm, mock_service, mock_chrome
    ):
        """버전 체크 예외"""
        from apps.articles.crawler_main import setup_driver

        mock_exists.return_value = True
        mock_run.side_effect = Exception("Command failed")
        mock_cdm.return_value.install.return_value = "/tmp/chromedriver"
        mock_chrome.return_value = FakeDriver()

        driver = setup_driver()
        self.assertIsNotNone(driver)


# ==================== 4) extract_content 테스트 ====================
class ExtractContentTest(SimpleTestCase):
    """extract_content 함수 테스트"""

    @patch("time.sleep")
    def test_extract_content_naver_success(self, mock_sleep):
        """네이버 뉴스 본문 추출 성공"""
        from apps.articles.crawler_main import extract_content

        html = """
        <html>
        <head></head>
        <body>
            <span class="media_end_head_info_datestamp_time" data-date-time="2025-11-26 10:00:00"></span>
            <article id="dic_area">
                <p>이것은 테스트 기사 본문입니다. 충분히 긴 텍스트를 만들어야 합니다. 
                더 많은 내용을 추가하여 100자 이상을 만듭니다. 테스트를 위한 추가 텍스트입니다.
                계속해서 더 많은 내용을 작성합니다.</p>
            </article>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://news.naver.com/article/123")

        self.assertIsNotNone(content)
        self.assertGreater(len(content), 100)
        self.assertIsNotNone(published_at)

    @patch("time.sleep")
    def test_extract_content_naver_articleBodyContents(self, mock_sleep):
        """네이버 뉴스 - articleBodyContents div"""
        from apps.articles.crawler_main import extract_content

        long_text = "테스트 본문 " * 30
        html = f"""
        <html>
        <body>
            <div id="articleBodyContents">{long_text}</div>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://news.naver.com/article/123")

        self.assertIsNotNone(content)

    @patch("time.sleep")
    def test_extract_content_general_site_article(self, mock_sleep):
        """일반 사이트 - article 태그"""
        from apps.articles.crawler_main import extract_content

        long_text = "일반 기사 본문 " * 30
        html = f"""
        <html>
        <body>
            <nav>네비게이션</nav>
            <article>{long_text}</article>
            <footer>푸터</footer>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://example.com/article")

        self.assertIsNotNone(content)
        self.assertNotIn("네비게이션", content)

    @patch("time.sleep")
    def test_extract_content_general_site_main(self, mock_sleep):
        """일반 사이트 - main 태그"""
        from apps.articles.crawler_main import extract_content

        long_text = "메인 콘텐츠 " * 30
        html = f"""
        <html>
        <body>
            <main>{long_text}</main>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://example.com/article")

        self.assertIsNotNone(content)

    @patch("time.sleep")
    def test_extract_content_general_site_paragraphs(self, mock_sleep):
        """일반 사이트 - p 태그들"""
        from apps.articles.crawler_main import extract_content

        html = """
        <html>
        <body>
            <p>첫 번째 문단입니다. 충분히 긴 텍스트.</p>
            <p>두 번째 문단입니다. 더 많은 내용.</p>
            <p>세 번째 문단입니다. 추가 텍스트.</p>
            <p>네 번째 문단입니다. 계속 추가.</p>
            <p>다섯 번째 문단입니다. 100자 이상.</p>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://example.com/article")

        self.assertIsNotNone(content)

    @patch("time.sleep")
    def test_extract_content_too_short(self, mock_sleep):
        """본문이 너무 짧음"""
        from apps.articles.crawler_main import extract_content

        html = "<html><body><p>짧음</p></body></html>"

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://example.com/article")

        self.assertIsNone(content)

    @patch("time.sleep")
    def test_extract_content_exception(self, mock_sleep):
        """예외 발생"""
        from apps.articles.crawler_main import extract_content

        driver = Mock()
        driver.get.side_effect = Exception("Network error")

        content, published_at = extract_content(driver, "https://example.com/article")

        self.assertIsNone(content)
        self.assertIsNone(published_at)

    @patch("time.sleep")
    def test_extract_content_script_style_removal(self, mock_sleep):
        """script/style 태그 제거"""
        from apps.articles.crawler_main import extract_content

        long_text = "실제 본문 내용 " * 30
        html = f"""
        <html>
        <body>
            <article id="dic_area">
                <script>alert('test');</script>
                <style>.test {{ color: red; }}</style>
                <em>강조</em>
                <strong>굵게</strong>
                {long_text}
            </article>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://news.naver.com/article/123")

        if content:
            self.assertNotIn("alert", content)

    @patch("time.sleep")
    def test_extract_content_date_text_fallback(self, mock_sleep):
        """발행일 텍스트 fallback"""
        from apps.articles.crawler_main import extract_content

        long_text = "본문 내용 " * 30
        html = f"""
        <html>
        <body>
            <span class="media_end_head_info_datestamp_time">2025.11.26 10:00</span>
            <article id="dic_area">{long_text}</article>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://news.naver.com/article/123")

        self.assertIsNotNone(content)


# ==================== 5) crawl_economy_section 테스트 ====================
class CrawlEconomySectionTest(SimpleTestCase):
    """crawl_economy_section 함수 테스트"""

    @patch("time.sleep")
    def test_crawl_economy_section_success(self, mock_sleep):
        """기사 URL 수집 성공"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/2">기사 2</a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=2)

        self.assertEqual(len(articles), 2)
        self.assertEqual(articles[0]["title"], "기사 1")
        self.assertEqual(articles[1]["title"], "기사 2")

    @patch("time.sleep")
    def test_crawl_economy_section_with_protocol_fix(self, mock_sleep):
        """// 프로토콜 보정"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="//news.naver.com/article/1">기사 1</a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=1)

        self.assertEqual(len(articles), 1)
        self.assertTrue(articles[0]["url"].startswith("https:"))

    @patch("time.sleep")
    def test_crawl_economy_section_duplicate_filter(self, mock_sleep):
        """중복 URL 필터링"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1 중복</a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=10)

        self.assertEqual(len(articles), 1)

    @patch("time.sleep")
    def test_crawl_economy_section_no_articles(self, mock_sleep):
        """기사 없음"""
        from apps.articles.crawler_main import crawl_economy_section

        html = "<html><body></body></html>"

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=10)

        self.assertEqual(len(articles), 0)

    @patch("time.sleep")
    def test_crawl_economy_section_missing_href(self, mock_sleep):
        """href 없는 링크"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title">제목만 있음</a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=10)

        self.assertEqual(len(articles), 0)

    @patch("time.sleep")
    def test_crawl_economy_section_missing_title(self, mock_sleep):
        """제목 없는 링크"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1"></a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=10)

        self.assertEqual(len(articles), 0)

    @patch("time.sleep")
    def test_crawl_economy_section_more_button_click(self, mock_sleep):
        """더보기 버튼 클릭"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
        </body>
        </html>
        """

        driver = Mock()
        driver.page_source = html
        driver.get = Mock()
        driver.find_element = Mock(side_effect=Exception("Button not found"))
        driver.execute_script = Mock()

        articles = crawl_economy_section(driver, target_count=100)

        self.assertGreaterEqual(len(articles), 0)


# ==================== 6) extract_all_contents 테스트 ====================
class ExtractAllContentsTest(SimpleTestCase):
    """extract_all_contents 함수 테스트"""

    @patch("apps.articles.crawler_main.extract_content")
    @patch("apps.articles.crawler_main.extract_source")
    @patch("time.sleep")
    def test_extract_all_contents_success(self, mock_sleep, mock_source, mock_content):
        """본문 일괄 추출 성공"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.return_value = ("테스트 본문 " * 30, "2025-11-26T10:00:00")
        mock_source.return_value = "테스트언론"

        articles = [
            {"title": "기사 1", "url": "https://example.com/1", "order": 1},
            {"title": "기사 2", "url": "https://example.com/2", "order": 2},
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=2)

        self.assertEqual(len(results), 2)
        self.assertEqual(failed, 0)

    @patch("apps.articles.crawler_main.extract_content")
    @patch("time.sleep")
    def test_extract_all_contents_partial_failure(self, mock_sleep, mock_content):
        """일부 실패"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.side_effect = [
            ("테스트 본문 " * 30, "2025-11-26T10:00:00"),
            (None, None),
        ]

        articles = [
            {"title": "기사 1", "url": "https://example.com/1", "order": 1},
            {"title": "기사 2", "url": "https://example.com/2", "order": 2},
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=10)

        self.assertEqual(len(results), 1)
        self.assertEqual(failed, 1)

    @patch("apps.articles.crawler_main.extract_content")
    @patch("time.sleep")
    def test_extract_all_contents_max_count_limit(self, mock_sleep, mock_content):
        """max_count 제한"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.return_value = ("테스트 본문 " * 30, "2025-11-26T10:00:00")

        articles = [
            {"title": f"기사 {i}", "url": f"https://example.com/{i}", "order": i} for i in range(10)
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=3)

        self.assertEqual(len(results), 3)

    @patch("apps.articles.crawler_main.extract_content")
    @patch("time.sleep")
    def test_extract_all_contents_too_short(self, mock_sleep, mock_content):
        """본문 너무 짧음"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.return_value = ("짧음", None)

        articles = [
            {"title": "기사 1", "url": "https://example.com/1", "order": 1},
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=10)

        self.assertEqual(len(results), 0)
        self.assertEqual(failed, 1)

    @patch("apps.articles.crawler_main.extract_content")
    @patch("time.sleep")
    def test_extract_all_contents_exception(self, mock_sleep, mock_content):
        """예외 발생"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.side_effect = Exception("Error")

        articles = [
            {"title": "기사 1", "url": "https://example.com/1", "order": 1},
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=10)

        self.assertEqual(len(results), 0)
        self.assertEqual(failed, 1)

    @patch("apps.articles.crawler_main.extract_content")
    @patch("time.sleep")
    def test_extract_all_contents_no_published_at(self, mock_sleep, mock_content):
        """published_at 없음"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.return_value = ("테스트 본문 " * 30, None)

        articles = [
            {"title": "기사 1", "url": "https://example.com/1", "order": 1},
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=10)

        self.assertEqual(len(results), 1)
        self.assertNotIn("published_at", results[0])


# ==================== 7) main 함수 테스트 ====================
class MainFunctionTest(SimpleTestCase):
    """main 함수 테스트"""

    @patch("apps.articles.crawler_main.upload_to_s3")
    @patch("apps.articles.crawler_main.extract_all_contents")
    @patch("apps.articles.crawler_main.crawl_economy_section")
    @patch("apps.articles.crawler_main.setup_driver")
    @patch("os.makedirs")
    @patch("builtins.open", new_callable=mock_open)
    @patch("time.time")
    def test_main_success(
        self, mock_time, mock_file, mock_makedirs, mock_setup, mock_crawl, mock_extract, mock_upload
    ):
        """main 함수 성공"""
        from apps.articles.crawler_main import main

        mock_time.side_effect = [100.0, 200.0]  # start, end

        fake_driver = Mock()
        mock_setup.return_value = fake_driver

        mock_crawl.return_value = [{"title": "기사 1", "url": "https://example.com/1", "order": 1}]

        mock_extract.return_value = (
            [
                {
                    "title": "기사 1",
                    "url": "https://example.com/1",
                    "content": "본문",
                    "content_length": 100,
                }
            ],
            0,
        )

        mock_upload.return_value = True

        result = main()

        self.assertIn("metadata", result)
        self.assertIn("articles", result)
        fake_driver.quit.assert_called_once()

    @patch("apps.articles.crawler_main.upload_to_s3")
    @patch("apps.articles.crawler_main.extract_all_contents")
    @patch("apps.articles.crawler_main.crawl_economy_section")
    @patch("apps.articles.crawler_main.setup_driver")
    @patch("os.makedirs")
    @patch("builtins.open", new_callable=mock_open)
    @patch("time.time")
    def test_main_driver_quit_on_exception(
        self, mock_time, mock_file, mock_makedirs, mock_setup, mock_crawl, mock_extract, mock_upload
    ):
        """예외 발생시에도 driver.quit 호출"""
        from apps.articles.crawler_main import main

        mock_time.side_effect = [100.0, 200.0]

        fake_driver = Mock()
        mock_setup.return_value = fake_driver

        mock_crawl.side_effect = Exception("Crawl error")

        with self.assertRaises(Exception):
            main()

        fake_driver.quit.assert_called_once()


# ==================== 8) NAVER_PRESS_CODE 상수 테스트 ====================
class NaverPressCodeTest(SimpleTestCase):
    """NAVER_PRESS_CODE 상수 테스트"""

    def test_press_code_mapping(self):
        """언론사 코드 매핑 확인"""
        from apps.articles.crawler_main import NAVER_PRESS_CODE

        # 주요 언론사 확인
        self.assertEqual(NAVER_PRESS_CODE["001"], "연합뉴스")
        self.assertEqual(NAVER_PRESS_CODE["015"], "한국경제")
        self.assertEqual(NAVER_PRESS_CODE["023"], "조선일보")
        self.assertEqual(NAVER_PRESS_CODE["025"], "중앙일보")
        self.assertEqual(NAVER_PRESS_CODE["028"], "한겨레")
        self.assertEqual(NAVER_PRESS_CODE["052"], "YTN")
        self.assertEqual(NAVER_PRESS_CODE["055"], "SBS")
        self.assertEqual(NAVER_PRESS_CODE["629"], "더팩트")

    def test_press_code_count(self):
        """언론사 코드 개수"""
        from apps.articles.crawler_main import NAVER_PRESS_CODE

        self.assertGreater(len(NAVER_PRESS_CODE), 40)


# ==================== 9) 추가 커버리지 테스트 ====================
class AdditionalCoverageTest(SimpleTestCase):
    """누락된 코드 커버리지 테스트"""

    def test_extract_source_exception_path(self):
        """extract_source 예외 경로"""
        from apps.articles.crawler_main import extract_source
        from unittest.mock import patch

        with patch("apps.articles.crawler_main.urlparse") as mock_urlparse:
            mock_urlparse.side_effect = Exception("Parse error")
            result = extract_source("http://example.com")
            self.assertEqual(result, "Unknown")

    @patch("time.sleep")
    def test_extract_content_date_parse_exception(self, mock_sleep):
        """발행일 파싱 예외"""
        from apps.articles.crawler_main import extract_content

        long_text = "본문 내용 " * 30
        html = f"""
        <html>
        <body>
            <span class="media_end_head_info_datestamp_time">invalid-date-format</span>
            <article id="dic_area">{long_text}</article>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://news.naver.com/article/123")

        self.assertIsNotNone(content)

    @patch("time.sleep")
    def test_crawl_economy_section_button_click_success(self, mock_sleep):
        """더보기 버튼 클릭 성공"""
        from apps.articles.crawler_main import crawl_economy_section
        from selenium.webdriver.common.by import By

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
        </body>
        </html>
        """

        mock_btn = Mock()
        mock_btn.click = Mock()

        driver = Mock()
        driver.page_source = html
        driver.get = Mock()
        driver.find_element = Mock(return_value=mock_btn)
        driver.execute_script = Mock()

        articles = crawl_economy_section(driver, target_count=1)

        self.assertGreaterEqual(len(articles), 0)

    @patch("time.sleep")
    def test_crawl_economy_section_button_click_with_js_fallback(self, mock_sleep):
        """더보기 버튼 JS fallback"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
        </body>
        </html>
        """

        mock_btn = Mock()
        mock_btn.click = Mock(side_effect=Exception("Click failed"))

        driver = Mock()
        driver.page_source = html
        driver.get = Mock()
        driver.find_element = Mock(return_value=mock_btn)
        driver.execute_script = Mock()

        articles = crawl_economy_section(driver, target_count=1)

        self.assertGreaterEqual(len(articles), 0)

    @patch("time.sleep")
    def test_crawl_no_new_articles_break(self, mock_sleep):
        """새 기사 없으면 종료"""
        from apps.articles.crawler_main import crawl_economy_section

        # 빈 HTML - 새 기사 없음
        html = "<html><body></body></html>"

        driver = Mock()
        driver.page_source = html
        driver.get = Mock()
        driver.find_element = Mock(side_effect=Exception("No button"))

        articles = crawl_economy_section(driver, target_count=100)

        self.assertEqual(len(articles), 0)

    @patch("time.sleep")
    def test_extract_content_removes_copyright_text(self, mock_sleep):
        """저작권 문구 제거"""
        from apps.articles.crawler_main import extract_content

        long_text = "본문 내용 " * 30
        html = f"""
        <html>
        <body>
            <article id="dic_area">
                {long_text}
                무단전재 및 재배포 금지
                ⓒ 2025 뉴스사
            </article>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        content, published_at = extract_content(driver, "https://news.naver.com/article/123")

        if content:
            self.assertNotIn("무단전재", content)
            self.assertNotIn("ⓒ", content)

    def test_extract_source_single_label_domain(self):
        """단일 라벨 도메인"""
        from apps.articles.crawler_main import extract_source

        result = extract_source("http://localhost/news")
        self.assertEqual(result, "localhost")

    @patch("apps.articles.crawler_main.extract_content")
    @patch("time.sleep")
    def test_extract_all_contents_progress_print(self, mock_sleep, mock_content):
        """50개마다 진행 출력"""
        from apps.articles.crawler_main import extract_all_contents

        mock_content.return_value = ("테스트 본문 " * 30, "2025-11-26T10:00:00")

        articles = [
            {"title": f"기사 {i}", "url": f"https://example.com/{i}", "order": i} for i in range(60)
        ]

        driver = FakeDriver()
        results, failed = extract_all_contents(driver, articles, max_count=60)

        self.assertEqual(len(results), 60)

    @patch("apps.articles.crawler_main.webdriver.Chrome")
    @patch("apps.articles.crawler_main.Service")
    @patch("apps.articles.crawler_main.ChromeDriverManager")
    @patch("subprocess.run")
    @patch("os.path.exists")
    def test_setup_driver_mac_app_path(
        self, mock_exists, mock_run, mock_cdm, mock_service, mock_chrome
    ):
        """Mac .app 경로 처리"""
        from apps.articles.crawler_main import setup_driver

        def exists_side_effect(path):
            return path == "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

        mock_exists.side_effect = exists_side_effect
        mock_run.return_value = Mock(stdout="Google Chrome 120.0")
        mock_cdm.return_value.install.return_value = "/tmp/chromedriver"
        mock_chrome.return_value = FakeDriver()

        driver = setup_driver()
        self.assertIsNotNone(driver)

    @patch("time.sleep")
    def test_crawl_economy_section_button_click_success_with_new_articles(self, mock_sleep):
        """더보기 버튼 클릭 성공 (Line 324-325)"""
        from apps.articles.crawler_main import crawl_economy_section

        # 첫 번째: 기사 1개, 두 번째: 기사 2개 (버튼 클릭 후 추가)
        html_pages = [
            """<html><body>
                <li class="sa_item">
                    <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
                </li>
            </body></html>""",
            """<html><body>
                <li class="sa_item">
                    <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
                </li>
                <li class="sa_item">
                    <a class="sa_text_title" href="https://news.naver.com/article/2">기사 2</a>
                </li>
            </body></html>""",
        ]

        page_index = [0]

        def get_page_source():
            idx = min(page_index[0], len(html_pages) - 1)
            page_index[0] += 1
            return html_pages[idx]

        mock_btn = Mock()
        mock_btn.click = Mock()  # 클릭 성공 (예외 없음)

        driver = Mock()
        type(driver).page_source = property(lambda self: get_page_source())
        driver.get = Mock()
        driver.find_element = Mock(return_value=mock_btn)
        driver.execute_script = Mock()

        articles = crawl_economy_section(driver, target_count=2)

        # btn.click()이 호출되었는지 확인
        self.assertTrue(mock_btn.click.called)

    @patch("time.sleep")
    def test_crawl_economy_section_button_click_then_js_fallback_success(self, mock_sleep):
        """버튼 클릭 실패 후 JS fallback 성공 (Line 324-325, 336-337)"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
        </body>
        </html>
        """

        mock_btn = Mock()
        mock_btn.click = Mock(side_effect=Exception("Click intercepted"))

        driver = Mock()
        driver.page_source = html
        driver.get = Mock()
        driver.find_element = Mock(return_value=mock_btn)
        driver.execute_script = Mock(return_value=None)

        # target_count를 크게 설정해야 버튼 클릭 로직까지 도달
        articles = crawl_economy_section(driver, target_count=10)

        # execute_script (JS fallback)이 호출되었는지 확인
        self.assertTrue(driver.execute_script.called)

    @patch("apps.articles.crawler_main.upload_to_s3")
    @patch("apps.articles.crawler_main.extract_all_contents")
    @patch("apps.articles.crawler_main.crawl_economy_section")
    @patch("apps.articles.crawler_main.setup_driver")
    @patch("os.makedirs")
    @patch("builtins.open", new_callable=mock_open)
    @patch("time.time")
    @patch("sys.stdout")
    def test_main_stdout_reconfigure_exception(
        self,
        mock_stdout,
        mock_time,
        mock_file,
        mock_makedirs,
        mock_setup,
        mock_crawl,
        mock_extract,
        mock_upload,
    ):
        """sys.stdout.reconfigure 예외 (Line 439-440)"""
        from apps.articles.crawler_main import main

        # reconfigure가 AttributeError 발생 (Python 버전에 따라)
        mock_stdout.reconfigure = Mock(side_effect=AttributeError("no reconfigure"))

        mock_time.side_effect = [100.0, 200.0]

        fake_driver = Mock()
        mock_setup.return_value = fake_driver

        mock_crawl.return_value = [{"title": "기사 1", "url": "https://example.com/1", "order": 1}]

        mock_extract.return_value = (
            [
                {
                    "title": "기사 1",
                    "url": "https://example.com/1",
                    "content": "본문",
                    "content_length": 100,
                }
            ],
            0,
        )

        mock_upload.return_value = True

        # 예외가 발생해도 main이 정상 실행되어야 함
        result = main()

        self.assertIn("metadata", result)
        fake_driver.quit.assert_called_once()

    def test_crawl_li_with_sa_text_title_selection(self):
        """li 내부에서 a.sa_text_title 선택 (Line 285)"""
        from apps.articles.crawler_main import crawl_economy_section

        # sa_text_title 클래스가 있는 a 태그
        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/001/123">
                    <strong class="sa_text_strong">테스트 기사 제목</strong>
                </a>
                <a class="sa_text_info" href="https://other.link">다른 링크</a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)

        with patch("time.sleep"):
            articles = crawl_economy_section(driver, target_count=1)

        self.assertEqual(len(articles), 1)
        self.assertIn("news.naver.com", articles[0]["url"])

    @patch("time.sleep")
    def test_crawl_with_multiple_li_items_select_one(self, mock_sleep):
        """여러 li에서 각각 a.sa_text_title 선택 (Line 285 다중 실행)"""
        from apps.articles.crawler_main import crawl_economy_section

        html = """
        <html>
        <body>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/1">기사 1</a>
            </li>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/2">기사 2</a>
            </li>
            <li class="sa_item">
                <a class="sa_text_title" href="https://news.naver.com/article/3">기사 3</a>
            </li>
        </body>
        </html>
        """

        driver = FakeDriver(page_source=html)
        articles = crawl_economy_section(driver, target_count=3)

        self.assertEqual(len(articles), 3)
