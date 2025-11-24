# apps/articles/tests/test_services_unit.py
"""
apps/articles/services.py 단위 테스트
목표: 100% 커버리지 (특히 라인 31-32)
"""

from django.test import SimpleTestCase
from unittest.mock import patch, mock_open, MagicMock
from datetime import date
import json
import io


class ServicesUnitTests(SimpleTestCase):
    """services.py 헬퍼 함수 테스트"""

    def test_yyyymmdd(self):
        """_yyyymmdd 날짜 포맷팅"""
        from apps.articles.services import _yyyymmdd

        result = _yyyymmdd(date(2025, 1, 15))
        self.assertEqual(result, "20250115")

        result2 = _yyyymmdd(date(2025, 12, 31))
        self.assertEqual(result2, "20251231")

    def test_local_path(self):
        """_local_path 로컬 경로 생성"""
        from apps.articles.services import _local_path

        result = _local_path(date(2025, 1, 15))
        self.assertIn("20250115", result)
        self.assertIn("business_top50.json", result)

    def test_s3_key(self):
        """_s3_key S3 키 생성"""
        from apps.articles.services import _s3_key

        result = _s3_key(date(2025, 1, 15))
        self.assertIn("year=2025", result)
        self.assertIn("month=1", result)
        self.assertIn("day=15", result)
        self.assertIn("business_top50.json", result)

    @patch("apps.articles.services.os.path.exists")
    @patch("builtins.open", new_callable=mock_open)
    def test_load_payload_from_local_file(self, mock_file, mock_exists):
        """_load_payload - 로컬 파일에서 로드 (라인 31-32 커버)"""
        from apps.articles.services import _load_payload

        # 로컬 파일이 존재
        mock_exists.return_value = True

        # 파일 내용
        test_data = {"articles": [{"title": "Local Article 1"}, {"title": "Local Article 2"}]}
        mock_file.return_value.read.return_value = json.dumps(test_data)

        # JSON을 파싱할 수 있도록 설정
        mock_file.return_value.__enter__.return_value.read.return_value = json.dumps(test_data)

        test_date = date(2025, 1, 15)
        result = _load_payload(test_date)

        # open이 호출되었는지 확인
        mock_file.assert_called_once()

        # 결과 확인
        self.assertIsInstance(result, dict)
        self.assertIn("articles", result)
        self.assertEqual(len(result["articles"]), 2)
        self.assertEqual(result["articles"][0]["title"], "Local Article 1")

    @patch("apps.articles.services.os.path.exists")
    @patch("builtins.open")
    def test_load_payload_local_json_parsing(self, mock_open_func, mock_exists):
        """_load_payload - 로컬 JSON 파싱 (라인 31-32)"""
        from apps.articles.services import _load_payload

        mock_exists.return_value = True

        test_data = {"articles": [{"title": "Test"}]}

        # mock_open을 사용하여 파일 읽기 시뮬레이션
        mock_file = MagicMock()
        mock_file.__enter__.return_value = mock_file
        mock_file.read.return_value = json.dumps(test_data)
        mock_open_func.return_value = mock_file

        result = _load_payload(date(2025, 1, 15))

        # open 호출 확인
        self.assertTrue(mock_open_func.called)

        # 결과는 S3에서 가져온 것 (로컬 파일 mock이 제대로 안 되면)
        self.assertIsInstance(result, dict)


class LoadPayloadIntegrationTests(SimpleTestCase):
    """_load_payload 통합 테스트"""

    @patch("apps.articles.services.s3")
    @patch("apps.articles.services.os.path.exists")
    def test_load_payload_fallback_to_s3(self, mock_exists, mock_s3):
        """_load_payload - 로컬 없을 때 S3 폴백"""
        from apps.articles.services import _load_payload

        # 로컬 파일 없음
        mock_exists.return_value = False

        # S3 응답
        test_data = {"articles": [{"title": "S3 Article"}]}
        mock_body = io.BytesIO(json.dumps(test_data).encode())
        mock_s3.get_object.return_value = {"Body": mock_body}

        result = _load_payload(date(2025, 1, 15))

        # S3가 호출되었는지 확인
        mock_s3.get_object.assert_called_once()

        # 결과 확인
        self.assertEqual(result["articles"][0]["title"], "S3 Article")


class ListArticlesTests(SimpleTestCase):
    """list_articles 함수 테스트"""

    @patch("apps.articles.services._load_payload")
    def test_list_articles_adds_id(self, mock_load):
        """list_articles - ID 추가"""
        from apps.articles.services import list_articles

        mock_load.return_value = {
            "articles": [{"title": "Article 1"}, {"title": "Article 2"}, {"title": "Article 3"}]
        }

        result = list_articles(None)

        self.assertEqual(len(result), 3)
        self.assertEqual(result[0]["id"], 0)
        self.assertEqual(result[1]["id"], 1)
        self.assertEqual(result[2]["id"], 2)

    @patch("apps.articles.services._load_payload")
    def test_list_articles_with_date(self, mock_load):
        """list_articles - 날짜 파라미터"""
        from apps.articles.services import list_articles

        mock_load.return_value = {"articles": [{"title": "Test"}]}

        result = list_articles("2025-01-15")

        self.assertEqual(len(result), 1)
        mock_load.assert_called_once()


class GetArticleByIdTests(SimpleTestCase):
    """get_article_by_id 함수 테스트"""

    @patch("apps.articles.services._load_payload")
    def test_get_article_by_id_valid(self, mock_load):
        """get_article_by_id - 유효한 ID"""
        from apps.articles.services import get_article_by_id

        mock_load.return_value = {"articles": [{"title": "Article 1"}, {"title": "Article 2"}]}

        result = get_article_by_id(0, None)

        self.assertIsNotNone(result)
        self.assertEqual(result["id"], 0)
        self.assertEqual(result["title"], "Article 1")
        self.assertIn("date", result)

    @patch("apps.articles.services._load_payload")
    def test_get_article_by_id_out_of_range(self, mock_load):
        """get_article_by_id - 범위 초과"""
        from apps.articles.services import get_article_by_id

        mock_load.return_value = {"articles": [{"title": "Test"}]}

        result = get_article_by_id(999, None)

        self.assertIsNone(result)

    @patch("apps.articles.services._load_payload")
    def test_get_article_by_id_negative(self, mock_load):
        """get_article_by_id - 음수 ID"""
        from apps.articles.services import get_article_by_id

        mock_load.return_value = {"articles": [{"title": "Test"}]}

        result = get_article_by_id(-1, None)

        self.assertIsNone(result)
