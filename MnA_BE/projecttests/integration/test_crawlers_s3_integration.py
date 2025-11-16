# projecttests/integration/test_crawlers_s3_integration.py
import os
import json
import datetime
import warnings
import boto3
from django.test import SimpleTestCase
from botocore.exceptions import NoCredentialsError, ProfileNotFound
from botocore.config import Config
from apps.articles.crawler_main import normalize_url

S3_BUCKET = "swpp-12-bucket"
S3_REGION = "ap-northeast-2"
S3_PREFIX = "news-articles"

class TestArticlesCrawlerIntegration(SimpleTestCase):
    def setUp(self):
        try:
            session = boto3.Session()
            creds = session.get_credentials()
            if creds is None:
                self.skipTest("No AWS credentials found by boto3 session; skipping integration test.")
            
            self.s3 = session.client(
                "s3", 
                region_name=S3_REGION,
                config=Config(retries={"max_attempts": 5, "mode": "standard"})
            )
        except (NoCredentialsError, ProfileNotFound) as e:
            self.skipTest(f"No AWS credentials/profile: {e}")

    def _latest_key(self):
        p = self.s3.get_paginator("list_objects_v2").paginate(
            Bucket=S3_BUCKET, 
            Prefix=S3_PREFIX
        )
        latest_key, latest_dt = None, None
        for page in p:
            for obj in page.get("Contents", []):
                k = obj["Key"]
                if not k.endswith("business_top50.json"):
                    continue
                try:
                    y, m, d = [int(seg.split("=")[1]) for seg in k.split("/")[1:4]]
                    dt = datetime.date(y, m, d)
                except Exception:
                    continue
                if latest_dt is None or dt > latest_dt:
                    latest_key, latest_dt = k, dt
        return latest_key

    def _get_latest_data(self):
        key = self._latest_key()
        if key is None:
            self.skipTest("No business_top50.json found in S3")
        obj = self.s3.get_object(Bucket=S3_BUCKET, Key=key)
        return json.load(obj["Body"])

    def test_file_exists_and_valid_json(self):
        key = self._latest_key()
        self.assertIsNotNone(key, "No business_top50.json found in S3")
        
        try:
            obj = self.s3.get_object(Bucket=S3_BUCKET, Key=key)
            json.load(obj["Body"])
        except json.JSONDecodeError as e:
            self.fail(f"Invalid JSON: {e}")

    def test_basic_structure(self):
        data = self._get_latest_data()
        
        self.assertIn("metadata", data)
        self.assertIn("articles", data)
        
        arts = data["articles"]
        meta = data["metadata"]
        
        self.assertIsInstance(arts, list)
        self.assertGreater(len(arts), 0, "No articles found")
        
        self.assertEqual(
            meta["total_articles"], 
            len(arts),
            f"Metadata total_articles ({meta['total_articles']}) != actual count ({len(arts)})"
        )
        
        if "success_count" in meta:
            self.assertEqual(
                len(arts), 
                meta["success_count"],
                f"Article count ({len(arts)}) != success_count ({meta['success_count']})"
            )

    def test_metadata_completeness(self):
        data = self._get_latest_data()
        meta = data["metadata"]
        
        required_meta = [
            "start_time",
            "end_time", 
            "elapsed_seconds",
            "elapsed_minutes",
            "target_count", 
            "success_count",
            "failed_count",
            "filtered_count",
            "total_articles"
        ]
        
        for field in required_meta:
            self.assertIn(field, meta, f"Missing metadata field: {field}")
        
        self.assertRegex(
            meta["start_time"], 
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}",
            "Invalid start_time format"
        )
        self.assertRegex(
            meta["end_time"], 
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}",
            "Invalid end_time format"
        )
        
        # Crawler logic: success_count + failed_count can be > target_count
        # because duplicate filtering happens after these counts
        self.assertGreaterEqual(
            meta["success_count"],
            meta["target_count"],
            f"success_count ({meta['success_count']}) should reach target ({meta['target_count']})"
        )
        
        # total_articles must match actual article count
        self.assertEqual(
            meta["total_articles"], 
            len(data["articles"]),
            "Metadata total_articles doesn't match actual count"
        )
        
        # success_count should match total_articles
        self.assertEqual(
            meta["success_count"],
            meta["total_articles"],
            f"success_count ({meta['success_count']}) != total_articles ({meta['total_articles']})"
        )
    
    def test_articles_structure_and_quality(self):
        data = self._get_latest_data()
        arts = data["articles"]
        
        required_fields = ["title", "url", "source", "content", "content_length"]
        
        for i, a in enumerate(arts):
            for field in required_fields:
                self.assertIn(field, a, f"Article [{i}]: missing '{field}'")
            
            self.assertTrue(
                a["title"].strip(), 
                f"Article [{i}]: empty title"
            )
            
            self.assertTrue(
                a["content"].strip(), 
                f"Article [{i}]: empty content"
            )
            
            self.assertTrue(
                a["url"].startswith("http"), 
                f"Article [{i}]: invalid URL format"
            )
            
            self.assertNotIn(
                "news.google.com", 
                a["url"], 
                f"Article [{i}]: unresolved Google redirect URL"
            )
            
            self.assertGreaterEqual(
                a["content_length"], 
                100, 
                f"Article [{i}]: content too short ({a['content_length']} chars)"
            )
            
            real_len = len(a["content"])
            self.assertLess(
                abs(real_len - a["content_length"]), 
                5,
                f"Article [{i}]: content_length mismatch "
                f"(actual: {real_len}, metadata: {a['content_length']})"
            )
            
            self.assertIsInstance(a["source"], str, f"Article [{i}]: source must be string")
            self.assertTrue(
                len(a["source"].strip()) > 0,
                f"Article [{i}]: empty source"
            )

    def test_no_duplicate_urls_after_normalization(self):
        data = self._get_latest_data()
        
        normalized_urls = [normalize_url(a["url"]) for a in data["articles"]]
        unique_urls = set(normalized_urls)
        
        duplicate_count = len(normalized_urls) - len(unique_urls)
        self.assertEqual(
            len(normalized_urls), 
            len(unique_urls),
            f"Found {duplicate_count} duplicate URLs after normalization"
        )

    def test_urls_normalized(self):
        data = self._get_latest_data()
        tracking_params = ["utm_source", "utm_medium", "fbclid", "gclid"]
        
        for i, a in enumerate(data["articles"]):
            url = a["url"]
            for param in tracking_params:
                self.assertNotIn(
                    param, 
                    url.lower(),
                    f"Article [{i}]: URL has tracking parameter '{param}'"
                )

    def test_sources_distribution(self):
        data = self._get_latest_data()
        
        source_counts = {}
        for a in data["articles"]:
            source = (a.get("source") or "").strip()
            source_counts[source] = source_counts.get(source, 0) + 1
        
        self.assertGreater(len(source_counts), 0, "No sources found")
        
        known_sources = {
            "naver", "hankyung", "chosun", "joins",
            "industrynews", "biz", "mk", "bbc", "yahoo", "reuters",
            "aitimes", "bloter", "byline", "cm", "dealsite", 
            "digitaltoday", "donga", "g-enews", "gametoc", "hani", 
            "inews24", "joongboo", "news", "newstopkorea", "segye", 
            "weekly", "yna", "zdnet",
        }
        
        found_sources = set(source_counts.keys())
        new_sources = found_sources - known_sources
        
        if new_sources:
            new_counts = {s: source_counts[s] for s in new_sources}
            warnings.warn(
                f"New sources detected: {sorted(new_sources)} "
                f"(counts: {new_counts})"
            )

    def test_file_size_reasonable(self):
        key = self._latest_key()
        response = self.s3.head_object(Bucket=S3_BUCKET, Key=key)
        file_size = response["ContentLength"]
        
        self.assertGreater(
            file_size, 
            50 * 1024, 
            f"File too small: {file_size} bytes"
        )
        
        self.assertLess(
            file_size, 
            10 * 1024 * 1024, 
            f"File too large: {file_size} bytes"
        )

    def test_recent_dates_exist(self):
        """S3에 데이터가 존재하고 최신성을 확인"""
        p = self.s3.get_paginator("list_objects_v2").paginate(
            Bucket=S3_BUCKET, 
            Prefix=S3_PREFIX
        )
        
        found_dates = set()
        for page in p:
            for obj in page.get("Contents", []):
                k = obj["Key"]
                if k.endswith("business_top50.json"):
                    try:
                        y, m, d = [int(seg.split("=")[1]) 
                                  for seg in k.split("/")[1:4]]
                        found_dates.add(datetime.date(y, m, d))
                    except Exception:
                        pass
        
        # 데이터 존재 확인
        self.assertGreater(
            len(found_dates), 
            0,
            "S3에 business_top50.json 데이터가 없습니다"
        )
        
        # 최신성 확인 (60일 이내)
        latest_date = max(found_dates)
        today = datetime.date.today()
        days_old = (today - latest_date).days
        
        self.assertLess(
            days_old,
            60,  # 60일 이내 데이터
            f"가장 최신 데이터: {latest_date} ({days_old}일 전). "
            f"Found dates: {sorted(found_dates)}"
        )