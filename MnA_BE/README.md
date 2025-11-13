# Welcome M&A's Backend Server!!

### Description
public address:
ec2-3-34-197-82.ap-northeast-2.compute.amazonaws.com

## Helpful commands

### Server Managements
* activate and deactivate virtual environment
``` 
    (windows)
    .venv/scripts/activate
```
``` 
    (linux)
    source .venv/bin/activate
```
```
    deactivate
```

* python package installation
```
    pip install -r requirements.txt
```

* update python packages
```
    pip freeze > requirements.txt
```

* run server
```
    python manage.py runserver 0.0.0.0:8000
```

* DB management
```
    python manage.py makemigrations api --pythonpath="apps"
```
```
    python manage.py makemigrations user --pythonpath="apps"
```
```
    python manage.py migrate
```

### Testing
* run tests
```
    coverage run --source='.' manage.py test
```
* check coverage
```
    coverage report
```

### Crawling

* crawl articles
```
    python manage.py crawler_articles --top 50
```

## Our Stacks:
* Base Language: Python with django framework\
    <img src="https://img.shields.io/badge/python-3776AB?style=for-the-badge&logo=python&logoColor=white">
    <img src="https://img.shields.io/badge/django-092E20?style=for-the-badge&logo=django&logoColor=white">
* MySQL\
    <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">

* AWS services (EC2, lambda, RDBMS, S3 storage) \
    <img src="https://img.shields.io/badge/amazonaws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white">