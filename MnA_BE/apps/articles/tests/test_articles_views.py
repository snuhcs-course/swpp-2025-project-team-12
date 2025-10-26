# apps/articles/tests/test_articles_views.py
from django.test import SimpleTestCase, Client
from django.urls import reverse, NoReverseMatch

class ArticlesViewTest(SimpleTestCase):
    """Black-box tests for /articles/ endpoints."""

    def setUp(self):
        self.client = Client()

    def test_url_names_exist(self):
        """모든 URL 이름(reverse 가능성) 확인"""
        for name, kwargs in [
            ("articles-list", None),
            ("articles-by-date", {"date": "2025-10-23"}),
            ("articles-detail", {"id": 1}),
        ]:
            try:
                reverse(name, kwargs=kwargs)
            except NoReverseMatch as e:
                self.fail(f"Missing or mismatched URL name: {name} ({e})")

    def test_get_root(self):
        """GET /articles/ → 200 + data list"""
        res = self.client.get(reverse("articles-list"))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res["Content-Type"].split(";")[0], "application/json")
        data = res.json()
        self.assertIn("data", data)
        self.assertIsInstance(data["data"], list)

    def test_get_by_date(self):
        """GET /articles/<date>/ → 200 + date + data list"""
        sample_date = "2025-10-23"
        res = self.client.get(reverse("articles-by-date", kwargs={"date": sample_date}))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res["Content-Type"].split(";")[0], "application/json")
        data = res.json()
        self.assertEqual(data.get("date"), sample_date)
        self.assertIn("data", data)
        self.assertIsInstance(data["data"], list)

    def test_get_detail_not_found(self):
        """GET /articles/detail/<id>/ (존재하지 않음) → 404"""
        res = self.client.get(reverse("articles-detail", kwargs={"id": 999_999}))
        self.assertEqual(res.status_code, 404)
