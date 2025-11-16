# apps/api/tests/integration/test_health.py
from django.test import TestCase, Client
import os

class ApiHealthTests(TestCase):
    """
    Health 엔드포인트 상세 테스트
    실제 S3 연결을 통한 통합 테스트
    """
    
    def setUp(self):
        """각 테스트 전에 실행"""
        self.client = Client()
        self.url = "/api/health"
    
    def test_health_endpoint_accessible(self):
        """헬스 체크 엔드포인트에 접근 가능한지 확인"""
        response = self.client.get(self.url)
        
        # 200 또는 503 둘 다 OK (S3 실패 시 503)
        self.assertIn(response.status_code, [200, 503])
    
    def test_health_returns_json(self):
        """헬스 체크가 JSON을 반환하는지 확인"""
        response = self.client.get(self.url)
        
        self.assertIn("application/json", response.get("Content-Type", ""))
        
        # JSON 파싱 가능한지
        data = response.json()
        self.assertIsInstance(data, dict)
    
    def test_health_has_required_fields(self):
        """헬스 체크 응답이 필수 필드를 포함하는지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # 필수 필드
        required_fields = ["api", "s3", "db", "asOf"]
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, data, f"Missing required field: {field}")
    
    def test_health_api_status_always_ok(self):
        """API 자체 상태는 항상 'ok'여야 함"""
        response = self.client.get(self.url)
        data = response.json()
        
        self.assertEqual(data["api"], "ok", "API status should always be 'ok'")
    
    def test_health_s3_status_structure(self):
        """S3 상태 정보가 올바른 구조인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # S3 필드는 dict여야 함
        self.assertIsInstance(data["s3"], dict, "s3 should be a dictionary")
        
        # 'ok' 필드 존재
        self.assertIn("ok", data["s3"], "s3 should have 'ok' field")
        self.assertIsInstance(data["s3"]["ok"], bool, "'ok' should be boolean")
        
        # ok가 True면 latest 정보 있어야 함
        if data["s3"]["ok"]:
            self.assertIn("latest", data["s3"], "Successful s3 check should have 'latest'")
            
            # latest 안에 companyProfile, priceFinancial
            latest = data["s3"]["latest"]
            self.assertIsInstance(latest, dict)
            self.assertIn("companyProfile", latest)
            self.assertIn("priceFinancial", latest)
    
    def test_health_db_status_structure(self):
        """DB 상태 정보가 올바른 구조인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        # DB 필드는 dict여야 함
        self.assertIsInstance(data["db"], dict, "db should be a dictionary")
        
        # 'ok' 필드 존재
        self.assertIn("ok", data["db"], "db should have 'ok' field")
        self.assertIsInstance(data["db"]["ok"], bool, "'ok' should be boolean")
        
        # DB는 항상 ok여야 함 (테스트 DB 사용 중)
        self.assertTrue(data["db"]["ok"], "DB should be ok during tests")
    
    def test_health_asof_is_iso_format(self):
        """asOf 필드가 ISO 8601 형식인지 확인"""
        response = self.client.get(self.url)
        data = response.json()
        
        asof = data.get("asOf")
        self.assertIsNotNone(asof, "asOf field should not be None")
        self.assertIsInstance(asof, str, "asOf should be string")
        
        # ISO 8601 형식 검증 (간단한 체크)
        self.assertRegex(
            asof,
            r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}',
            "asOf should be in ISO 8601 format (YYYY-MM-DDTHH:MM:SS)"
        )
    
    def test_health_with_s3_success(self):
        """
        S3 연결이 성공하면 200 OK 반환
        
        환경변수가 제대로 설정되어 있어야 함
        """
        if not os.getenv('FINANCE_BUCKET_NAME'):
            self.skipTest("FINANCE_BUCKET_NAME not set - cannot test S3 connection")
        
        response = self.client.get(self.url)
        data = response.json()
        
        # S3가 성공하면
        if data["s3"]["ok"]:
            # 200 OK여야 함
            self.assertEqual(response.status_code, 200)
            
            # latest 정보가 있어야 함
            self.assertIn("latest", data["s3"])
            latest = data["s3"]["latest"]
            
            # 타임스탬프 정보 확인
            self.assertIsInstance(latest["companyProfile"], str)
            self.assertIsInstance(latest["priceFinancial"], str)
    
    def test_health_with_s3_failure(self):
        """
        S3 연결 실패 시 적절히 처리하는지 확인
        
        실제 실패를 테스트하기는 어려우므로,
        실패 시 응답 구조가 맞는지만 확인
        """
        response = self.client.get(self.url)
        data = response.json()
        
        # S3가 실패하면
        if not data["s3"]["ok"]:
            # 503 Service Unavailable일 수 있음
            self.assertIn(response.status_code, [200, 503])
            
            # 에러 정보가 있을 수 있음
            # (하지만 필수는 아님)
    
    def test_health_idempotent(self):
        """
        헬스 체크를 여러 번 호출해도 같은 결과
        (멱등성 - idempotency)
        """
        response1 = self.client.get(self.url)
        response2 = self.client.get(self.url)
        
        self.assertEqual(response1.status_code, response2.status_code)
        
        data1 = response1.json()
        data2 = response2.json()
        
        # 주요 상태는 동일해야 함
        self.assertEqual(data1["api"], data2["api"])
        self.assertEqual(data1["s3"]["ok"], data2["s3"]["ok"])
        self.assertEqual(data1["db"]["ok"], data2["db"]["ok"])
    
    def test_health_no_authentication_required(self):
        """헬스 체크는 인증 없이 접근 가능해야 함"""
        # 인증 헤더 없이
        response = self.client.get(self.url)
        
        # 401 Unauthorized가 아니어야 함
        self.assertNotEqual(response.status_code, 401)
        
        # 접근 가능해야 함
        self.assertIn(response.status_code, [200, 503])
    
    def test_health_accepts_get_only(self):
        """헬스 체크는 GET만 허용"""
        # POST는 405 Method Not Allowed
        response_post = self.client.post(self.url)
        self.assertEqual(response_post.status_code, 405)
        
        # PUT도 405
        response_put = self.client.put(self.url)
        self.assertEqual(response_put.status_code, 405)
        
        # DELETE도 405
        response_delete = self.client.delete(self.url)
        self.assertEqual(response_delete.status_code, 405)