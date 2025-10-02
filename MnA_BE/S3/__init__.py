import boto3
import os
import pandas

class S3Client:
	"""
	you can use S3 service with this class.
	"""
	__client = None
	__s3_resource = None

	def __init__(self):
		__client = boto3.client(
		's3',
			aws_access_key_id=os.environ['IAM_ACCESS_KEY_ID'],
			aws_secret_access_key=os.environ['IAM_SECRET_KEY']
		)
		__s3_resource = __client.resource('s3')

	def get(self, bucket_name, key):
		"""
        Gets the object.

        :param bucket_name: target Bucket name.
        :param key: The object key.

        :return: The object data in bytes.
        """
		try:
			obj = self.__s3_resource.Object(bucket_name, key)
			body = obj.get()["Body"]
		except:
			raise Exception(
				"Couldn't get object '%s' from bucket '%s'.",
				key, bucket_name)

		return body

	def put_file(self, bucket_name, key, path_name):
		"""
        Upload data to the object.


        :param bucket_name: target Bucket name.
        :param key: The object key.
        :param path_name: the file's path that we want to upload.
        """
		try:
			bucket = self.__s3_resource.Bucket(bucket_name)
			if os.path.exists(path_name):
				bucket.upload_file(path_name, key)
		except:
			raise Exception(
				"Couldn't put object '%s' to bucket '%s'.",
				key, bucket_name)

	def get_dataframe(self, bucket, key) -> pandas.DataFrame:
		pass

	def put_dataframe(self, bucket, key, dataframe: pandas.DataFrame):
		pass
