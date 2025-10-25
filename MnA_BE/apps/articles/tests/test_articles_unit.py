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
