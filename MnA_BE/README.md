# Welcome M&A's Backend Server!!

### Description
TODO

### Helpful commands
* activate and deactivate virtual environment
```
    .venv/scripts/activate
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
    python manage.py runserver
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