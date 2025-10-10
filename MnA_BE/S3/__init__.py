import boto3
import os

S3 = boto3.client(
	's3',
	aws_access_key_id=os.environ['IAM_ACCESS_KEY_ID'],
	aws_secret_access_key=os.environ['IAM_SECRET_KEY'],
)