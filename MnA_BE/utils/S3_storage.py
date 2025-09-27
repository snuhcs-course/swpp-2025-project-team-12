import boto3

S3 = boto3.client(
	's3',
	aws_access_key_id="ACCESS_KEY_ID",
	aws_secret_access_key="SECRET_KEY",
)