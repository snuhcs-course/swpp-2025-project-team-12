아래 순서대로 crawler2.py 가 실행됨.

1. ec2 인스턴스에서 contab에 의해 한국시간 월-금 7시에 실행
2. 거래일이 아니거나 현재시간이 18시 보다 작을 경우 아무것도 안하고 프로그램 종료.
3. 거래일이고 매월 첫 거래일이 아닐 경우 | prefix=price-financial-info 에 주가 및 재무 정보 dataframe으로 저장 (<3초)
4. 거래일이고 매월 첫 거래일일 경우 | 3에서 저장하는 것에 추가로 prefix=company-profile 회사별 정보 저장 (>1시간)

s3의 스펙은 다음과 같음
bucket은 슬랙 참고
market은 kospi or kosdaq

prefix = price-financial-info
s3://{bucket}/price-financial-info/year=2025/month=9/market=kospi/2025-09-29.parquet

prefix = company-profile
s3://{bucket}/company-profile/year=2025/month=9/market=kospi/2025-09-29.parquet

bucket 접속은 access key 슬랙 참고

bucket에서 일별, 티커별 정보 읽기는 read_test.ipynb 참고