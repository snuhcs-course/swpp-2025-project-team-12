# apps/articles/tests/test_articles_views.py
from django.test import SimpleTestCase, Client
from django.urls import reverse, NoReverseMatch
from unittest.mock import patch
import json
import io


class ArticlesViewTest(SimpleTestCase):
    """Black-box tests for /articles/ endpoints."""

    def setUp(self):
        self.client = Client()
        
        # Mock S3 response data
        self.mock_s3_data = {
            "metadata": {
                "total_collected": 3,
                "start_time": "2025-10-23T10:00:00+09:00"
            },
            "articles": [
                {
                    "title": "Test Business Article",
                    "url": "https://example.com/business/1",
                    "source": "example",
                    "section": "BUSINESS",
                    "content": "This is test business content. " * 20,
                    "fetched_at": "2025-10-23T10:00:00+09:00"
                },
                {
                    "title": "Test Tech Article",
                    "url": "https://example.com/tech/1",
                    "source": "techsite",
                    "section": "TECHNOLOGY",
                    "content": "This is test technology content. " * 20,
                    "fetched_at": "2025-10-23T10:15:00+09:00"
                },
                {
                    "title": "Test Health Article",
                    "url": "https://example.com/health/1",
                    "source": "healthnews",
                    "section": "HEALTH",
                    "content": "This is test health content. " * 20,
                    "fetched_at": "2025-10-23T10:30:00+09:00"
                }
            ]
        }

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

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_root(self, mock_exists, mock_s3):
        """GET /articles/ → 200 + data list"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        res = self.client.get(reverse("articles-list"))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res["Content-Type"].split(";")[0], "application/json")
        data = res.json()
        self.assertIn("data", data)
        self.assertIsInstance(data["data"], list)
        if data["data"]:
            self.assertEqual(data["data"][0]["id"], 0)

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_by_date(self, mock_exists, mock_s3):
        """GET /articles/<date>/ → 200 + date + data list"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        sample_date = "2025-10-23"
        res = self.client.get(reverse("articles-by-date", kwargs={"date": sample_date}))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res["Content-Type"].split(";")[0], "application/json")
        data = res.json()
        self.assertEqual(data.get("date"), sample_date)
        self.assertIn("data", data)
        self.assertIsInstance(data["data"], list)

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_by_date_invalid_format(self, mock_exists, mock_s3):
        """GET /articles/<date>/ with invalid date format → 400"""
        mock_exists.return_value = False
        invalid_date = "invalid-date"
        res = self.client.get(reverse("articles-by-date", kwargs={"date": invalid_date}))
        self.assertEqual(res.status_code, 400)

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_detail_success(self, mock_exists, mock_s3):
        """GET /articles/detail/<id>/ (존재함) → 200 + article data"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        res = self.client.get(reverse("articles-detail", kwargs={"id": 0}))
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertEqual(data["id"], 0)
        self.assertEqual(data["title"], "Test Business Article")
        self.assertIn("date", data)
        self.assertIn("content", data)

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_detail_not_found(self, mock_exists, mock_s3):
        """GET /articles/detail/<id>/ (존재하지 않음) → 404"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        res = self.client.get(reverse("articles-detail", kwargs={"id": 999}))
        self.assertEqual(res.status_code, 404)
        data = res.json()
        self.assertIn("message", data)

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_detail_with_date_param(self, mock_exists, mock_s3):
        """GET /articles/detail/<id>/?date=YYYY-MM-DD → 200"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        res = self.client.get(
            reverse("articles-detail", kwargs={"id": 1}) + "?date=2025-10-23"
        )
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertEqual(data["id"], 1)
        self.assertEqual(data["date"], "2025-10-23")

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_root_empty_articles(self, mock_exists, mock_s3):
        """GET /articles/ with empty articles → 200 + empty list"""
        mock_exists.return_value = False
        
        empty_data = {
            "metadata": {"total_collected": 0},
            "articles": []
        }
        mock_body = io.BytesIO(json.dumps(empty_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        res = self.client.get(reverse("articles-list"))
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertIn("data", data)
        self.assertEqual(len(data["data"]), 0)

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_detail_boundary_last_article(self, mock_exists, mock_s3):
        """GET /articles/detail/<id>/ with last valid index → 200"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        res = self.client.get(reverse("articles-detail", kwargs={"id": 2}))
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertEqual(data["id"], 2)
        self.assertEqual(data["title"], "Test Health Article")

    @patch('apps.articles.services.s3')
    @patch('os.path.exists')
    def test_get_by_date_with_different_date(self, mock_exists, mock_s3):
        """GET /articles/<date>/ with different date → 200"""
        mock_exists.return_value = False
        
        mock_body = io.BytesIO(json.dumps(self.mock_s3_data).encode('utf-8'))
        mock_response = {'Body': mock_body}
        mock_s3.get_object.return_value = mock_response
        
        different_date = "2025-01-01"
        res = self.client.get(reverse("articles-by-date", kwargs={"date": different_date}))
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertEqual(data.get("date"), different_date)
        self.assertIn("data", data)