# apps/api/tests/integration/test_company_profiles.py
from django.test import TestCase, Client
from unittest.mock import patch, Mock


class ApiCompanyProfilesTests(TestCase):
    """
    기업 프로필 엔드포인트 상세 테스트
    실제 S3 데이터 또는 degraded 모드 검증
    """

    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/company-profiles"

    def test_company_profiles_endpoint_accessible(self):
        """기업 프로필 엔드포인트에 접근 가능한지 확인"""
        response = self.client.get(self.url)

        # 항상 200 OK (degraded 가능)
        self.assertEqual(response.status_code, 200)

    def test_company_profiles_returns_json(self):
        """기업 프로필이 JSON을 반환하는지 확인"""
        response = self.client.get(self.url)

        self.assertIn("application/json", response.get("Content-Type", ""))

        data = response.json()
        self.assertIsInstance(data, dict)

    def test_company_profiles_has_pagination_fields(self):
        """기업 프로필 응답이 페이지네이션 필드를 포함하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # 필수 페이지네이션 필드
        pagination_fields = ["total", "limit", "offset"]
        for field in pagination_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing pagination field: {field}")
                self.assertIsInstance(data[field], int, f"{field} should be integer")

    def test_company_profiles_default_pagination(self):
        """기본 페이지네이션 값이 적용되는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # 기본값: limit=10, offset=0
        self.assertEqual(data["limit"], 10, "Default limit should be 10")
        self.assertEqual(data["offset"], 0, "Default offset should be 0")

    def test_company_profiles_custom_limit(self):
        """사용자 지정 limit가 적용되는지 확인"""
        response = self.client.get(self.url, {"limit": 5})
        data = response.json()

        self.assertEqual(data["limit"], 5, "Custom limit should be applied")

        # items가 있으면 limit 이하여야 함
        if "items" in data and not data.get("degraded"):
            self.assertLessEqual(len(data["items"]), 5, "Items count should not exceed limit")

    def test_company_profiles_custom_offset(self):
        """사용자 지정 offset이 적용되는지 확인"""
        response = self.client.get(self.url, {"offset": 10})
        data = response.json()

        self.assertEqual(data["offset"], 10, "Custom offset should be applied")

    def test_company_profiles_pagination_works(self):
        """페이지네이션이 실제로 작동하는지 확인"""
        # 첫 페이지
        response1 = self.client.get(self.url, {"limit": 3, "offset": 0})
        data1 = response1.json()

        # 두 번째 페이지
        response2 = self.client.get(self.url, {"limit": 3, "offset": 3})
        data2 = response2.json()

        # degraded가 아니고 데이터가 충분하면
        if not data1.get("degraded") and data1["total"] > 3:
            items1 = data1.get("items", [])
            items2 = data2.get("items", [])

            # 두 페이지의 아이템이 달라야 함
            if items1 and items2:
                tickers1 = [item["ticker"] for item in items1]
                tickers2 = [item["ticker"] for item in items2]

                # 겹치는 ticker가 없어야 함
                self.assertEqual(
                    len(set(tickers1) & set(tickers2)),
                    0,
                    "Different pages should have different items",
                )

    def test_company_profiles_items_structure(self):
        """items 배열의 구조가 올바른지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # degraded가 아니면 items 있어야 함
        if not data.get("degraded"):
            self.assertIn("items", data, "Non-degraded response should have items")
            self.assertIsInstance(data["items"], list, "items should be list")

            # items가 있으면 각 item 검증
            if data["items"]:
                item = data["items"][0]

                # ticker 필드 필수
                self.assertIn("ticker", item, "Item should have ticker")
                self.assertIsInstance(item["ticker"], str)

                # explanation 필드 필수
                self.assertIn("explanation", item, "Item should have explanation")

    def test_company_profiles_symbol_query(self):
        """특정 심볼 검색이 작동하는지 확인"""
        # 유명한 삼성전자로 테스트
        response = self.client.get(self.url, {"symbol": "005930"})
        data = response.json()

        self.assertEqual(response.status_code, 200)

        # degraded가 아니면
        if not data.get("degraded"):
            # 심볼을 찾았으면 total=1
            if data["total"] == 1:
                self.assertEqual(len(data["items"]), 1)
                self.assertEqual(data["items"][0]["ticker"], "005930")
            # 못 찾았으면 total=0
            elif data["total"] == 0:
                self.assertEqual(len(data["items"]), 0)

    def test_company_profiles_nonexistent_symbol(self):
        """존재하지 않는 심볼 검색 시 빈 결과 반환"""
        response = self.client.get(self.url, {"symbol": "INVALID999"})
        data = response.json()

        self.assertEqual(response.status_code, 200)

        # degraded가 아니면 빈 결과
        if not data.get("degraded"):
            self.assertEqual(data["total"], 0)
            self.assertEqual(len(data.get("items", [])), 0)

    def test_company_profiles_has_source_field(self):
        """응답에 source 필드가 있는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # source 필드가 있으면
        if "source" in data:
            self.assertIn(data["source"], ["s3", "mock", "memory"])

    def test_company_profiles_has_asof_field(self):
        """응답에 asOf 필드가 있는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # asOf 필드가 있으면 타임스탬프 형식
        if "asOf" in data:
            asof = data["asOf"]
            self.assertIsInstance(asof, str)
            self.assertRegex(asof, r"\d{4}-\d{2}-\d{2}", "asOf should contain date")

    def test_company_profiles_degraded_mode(self):
        """degraded 모드가 적절히 처리되는지 확인"""
        response = self.client.get(self.url)
        data = response.json()

        # degraded이면 error 메시지 있어야 함
        if data.get("degraded"):
            self.assertIn("error", data, "Degraded response should have error message")
            self.assertIsInstance(data["error"], str)

    def test_company_profiles_total_consistency(self):
        """total 값이 실제 데이터와 일치하는지 확인"""
        response = self.client.get(self.url, {"limit": 100})  # 큰 limit
        data = response.json()

        # degraded가 아니면
        if not data.get("degraded") and "items" in data:
            # offset=0이고 limit이 total보다 크면
            # items 개수 = total (또는 더 적음)
            if data["offset"] == 0 and data["limit"] >= data["total"]:
                self.assertEqual(
                    len(data["items"]),
                    data["total"],
                    "Items count should equal total when fetching all",
                )

    def test_company_profiles_max_limit(self):
        """최대 limit이 적용되는지 확인 (100 제한)"""
        response = self.client.get(self.url, {"limit": 1000})
        data = response.json()

        # limit은 100을 초과하지 않아야 함
        self.assertLessEqual(data["limit"], 100, "Limit should not exceed maximum (100)")

    def test_company_profiles_negative_offset(self):
        """음수 offset 처리"""
        response = self.client.get(self.url, {"offset": -1})
        data = response.json()

        # 음수는 0으로 처리되거나 에러
        self.assertIn(response.status_code, [200, 400])

    def test_company_profiles_no_authentication_required(self):
        """기업 프로필은 인증 없이 접근 가능"""
        response = self.client.get(self.url)

        self.assertNotEqual(response.status_code, 401)
        self.assertEqual(response.status_code, 200)

    def test_company_profiles_accepts_get_only(self):
        """기업 프로필은 GET만 허용"""
        # POST는 405
        response_post = self.client.post(self.url)
        self.assertEqual(response_post.status_code, 405)

        # PUT도 405
        response_put = self.client.put(self.url)
        self.assertEqual(response_put.status_code, 405)

        # DELETE도 405
        response_delete = self.client.delete(self.url)
        self.assertEqual(response_delete.status_code, 405)

    def test_company_profiles_market_filter(self):
        """시장 필터(kospi/kosdaq)가 있는지 확인 (선택적)"""
        # kosdaq 필터
        response = self.client.get(self.url, {"market": "kosdaq"})

        # 필터가 구현되어 있으면 200, 아니면 무시
        self.assertEqual(response.status_code, 200)

    def test_company_profiles_response_time(self):
        """기업 프로필 응답이 합리적인 시간 내에 오는지"""
        import time

        start = time.time()
        response = self.client.get(self.url)
        elapsed = time.time() - start

        # 10초 이내 응답 (S3 호출 가능)
        self.assertLess(elapsed, 10.0, "Response should be within 10 seconds")
        self.assertEqual(response.status_code, 200)

    def test_company_profiles_with_symbol_found(self):
        """특정 심볼 검색 - 찾았을 때 (lines 152-166)"""
        # 실제 존재하는 심볼 (삼성전자)
        response = self.client.get(self.url, {"symbol": "005930"})
        data = response.json()

        self.assertEqual(response.status_code, 200)

        # S3 데이터가 있으면
        if not data.get("degraded") and data.get("total", 0) > 0:
            self.assertEqual(data["total"], 1)
            self.assertEqual(len(data["items"]), 1)
            self.assertEqual(data["items"][0]["ticker"], "005930")

    def test_company_profiles_with_symbol_not_found(self):
        """특정 심볼 검색 - 못 찾았을 때 (lines 167-175)"""
        # 존재하지 않는 심볼
        response = self.client.get(self.url, {"symbol": "INVALID999"})
        data = response.json()

        self.assertEqual(response.status_code, 200)

        # S3 데이터가 있으면 빈 결과
        if not data.get("degraded"):
            self.assertEqual(data["total"], 0)
            self.assertEqual(len(data["items"]), 0)

    @patch("apps.api.views.ApiConfig")
    def test_company_profiles_symbol_in_index(self, mock_config):
        """특정 심볼이 인덱스에 있을 때 (lines 185-190)"""
        import pandas as pd

        # Mock DataFrame with symbol in index
        mock_df = pd.DataFrame({"explanation": ["Test company explanation"]}, index=["TEST001"])

        mock_config.profile_df = mock_df
        mock_config.instant_df = None

        response = self.client.get(self.url, {"symbol": "TEST001"})
        data = response.json()

        self.assertEqual(response.status_code, 200)
        self.assertEqual(data["total"], 1)
        self.assertEqual(len(data["items"]), 1)
        self.assertEqual(data["items"][0]["ticker"], "TEST001")
