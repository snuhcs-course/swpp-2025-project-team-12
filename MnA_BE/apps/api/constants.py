import os

########################################
### Settings
########################################
FINANCE_BUCKET    = os.getenv("FINANCE_BUCKET_NAME")
S3_PREFIX_COMPANY = os.getenv("S3_PREFIX_COMPANY_PROFILE", "company-profile/")
S3_PREFIX_PRICE   = os.getenv("S3_PREFIX_PRICE_FIN",      "price-financial-info/")
S3_PREFIX_INDICES = os.getenv("S3_PREFIX_INDICES",        "stock-index/")
S3_PREFIX_ARTICLE = os.getenv("S3_PREFIX_ARTICLES",       "articles/")

ARTICLES_SOURCE   = os.getenv("ARTICLES_SOURCE", "mock")   # mock | s3
INDICES_SOURCE    = os.getenv("INDICES_SOURCE",  "mock")   # mock | s3