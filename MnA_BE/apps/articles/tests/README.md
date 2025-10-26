# Articles Crawler Tests

## Structure
- `test_articles_unit.py`: unit tests for `crawler_main.py` internal functions
- `test_articles_views.py`: endpoint tests for `/articles/`
- `projecttests/integration/test_crawlers_s3_integration.py`: S3 integration

## Run
```bash
# Local unit/view tests
python manage.py test apps.articles.tests -v 2

# Integration (requires AWS_PROFILE)
export AWS_PROFILE=swpp12-ro
python manage.py test projecttests.integration -v 2
```