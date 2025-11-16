from django.core.management.base import BaseCommand
from apps.articles.crawler_main import main


class Command(BaseCommand):
    help = "Crawl Korean finance articles from Google News RSS (Business Topic)"

    def add_arguments(self, parser):
        parser.add_argument(
            '--top',
            type=int,
            default=50,
            help='Maximum number of articles to save (default: 50)'
        )

    def handle(self, *args, **options):
        self.stdout.write(self.style.SUCCESS('=' * 60))
        self.stdout.write(self.style.SUCCESS('Starting Business News Crawler (Selenium)...'))
        self.stdout.write(self.style.SUCCESS('=' * 60))
        
        try:
            main()
            
            self.stdout.write(self.style.SUCCESS('=' * 60))
            self.stdout.write(self.style.SUCCESS('✓ Crawling completed!'))
            self.stdout.write(self.style.SUCCESS('=' * 60))
            
        except Exception as e:
            self.stdout.write(self.style.ERROR(f'✗ Error occurred: {str(e)}'))
            raise