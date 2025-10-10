import boto3
import os
import pandas
import traceback
import base64
import io
from utils.debug_print import debug_print

class S3Client:
	"""
	you can use S3 service with this class.
	"""
	__client = None

	def __init__(self):
		__client = boto3.client(
		's3',
			aws_access_key_id=os.environ['IAM_ACCESS_KEY_ID'],
			aws_secret_access_key=os.environ['IAM_SECRET_KEY']
		)

	def get(self, bucket_name, key):
		"""
		get object from S3 bucket, and return it's body.
        """
		try:
			obj = self.__client.get_object(Bucket=bucket_name, Key=key)
		except:
			debug_print(traceback.format_exc())
			raise Exception(f"S3 ERROR: Couldn't get object '{key}' from bucket '{bucket_name}'.")

		return obj['Body'].read()

	def get_image_url(self, bucket_name, key):
		"""
		get image data from S3 bucket.
		"""
		try:
			obj = self.__client.get_object(Bucket=bucket_name, Key=key)
			content_type = obj['ContentType']
			bytes_data = obj['Body'].read()
			base64_data = base64.b64encode(bytes_data).decode('utf-8')

			return f"data:{content_type};base64,{base64_data}"
		except:
			debug_print(traceback.format_exc())
			raise Exception(f"S3 ERROR: Couldn't get image from bucket '{bucket_name}'.")

	def put_image(self, bucket_name, key, image_url):
		"""
		put image data to S3 bucket.
		"""
		try:
			header, base64_data = image_url.split(";base64,")
			content_type = header.split(":")[1]

			try:
				bytes_data = base64.b64decode(base64_data)
			except:
				raise Exception("S3 ERROR: Invalid base64 data.")

			self.__client.put_object(
				Bucket=bucket_name,
				Key=key,
				Body=io.BytesIO(bytes_data),
				ContentType=content_type,
				ACL='public-read'
			)
		except:
			debug_print(traceback.format_exc())
			raise Exception(f"S3 ERROR: Couldn't put bytes to bucket '{bucket_name}'.")

	def put_file(self, bucket_name, key, path_name):
		"""
		put data from file to S3 bucket.
        """
		try:
			pass
		except:
			debug_print(traceback.format_exc())
			raise Exception(f"S3 ERROR: Couldn't put object to bucket '{bucket_name}'.")

	def delete(self, bucket_name, key):
		"""
		delete object from S3 bucket.
		"""
		try:
			pass
		except:
			debug_print(traceback.format_exc())
			raise Exception(f"S3 ERROR: Couldn't delete object '{key}' from bucket '{bucket_name}'.")

	def get_dataframe(self, bucket, key) -> pandas.DataFrame:
		pass

	def put_dataframe(self, bucket, key, dataframe: pandas.DataFrame):
		pass
