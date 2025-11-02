# apps/articles/tests/test_articles_unit.py
from django.test import SimpleTestCase
from apps.articles.crawler_main import normalize_url, extract_source


class TestArticlesUnit(SimpleTestCase):
    def test_normalize_url(self):
        url = "https://example.com/a?utm_source=x&fbclid=abc&id=1"
        out = normalize_url(url)
        self.assertNotIn("utm_source", out)
        self.assertNotIn("fbclid", out)
        self.assertTrue(out.startswith("https://example.com/a"))

    def test_extract_source(self):
        self.assertEqual(extract_source("https://news.naver.com/a"), "naver")
        self.assertEqual(extract_source("https://www.hankyung.com/it"), "hankyung")
    
    def test_normalize_url_with_utm_campaign(self):
        """UTM campaign 파라미터도 제거"""
        url = "https://example.com/page?utm_campaign=spring&utm_medium=email&id=5"
        out = normalize_url(url)
        self.assertNotIn("utm_campaign", out)
        self.assertNotIn("utm_medium", out)
        self.assertIn("id=5", out)
    
    def test_normalize_url_with_gclid(self):
        """gclid 파라미터 제거"""
        url = "https://example.com/article?gclid=xyz123&ref=google"
        out = normalize_url(url)
        self.assertNotIn("gclid", out)
        self.assertIn("ref=google", out)
    
    def test_extract_source_with_www(self):
        """www 프리픽스 처리"""
        self.assertEqual(extract_source("https://www.chosun.com/news"), "chosun")
        self.assertEqual(extract_source("https://www.joongang.co.kr/article"), "joongang")
    
    def test_extract_source_co_kr_domain(self):
        """co.kr 도메인 처리"""
        self.assertEqual(extract_source("https://www.mk.co.kr/news/economy"), "mk")
        self.assertEqual(extract_source("https://biz.chosun.co.kr/site/data"), "chosun")
    
    def test_extract_source_subdomain(self):
        """서브도메인 처리"""
        self.assertEqual(extract_source("https://news.naver.com/main"), "naver")
        self.assertEqual(extract_source("https://finance.naver.com/news"), "naver")