from django.core.management.base import BaseCommand
from apps.articles import crawler_main


class Command(BaseCommand):
    help = "Crawl Korean finance articles from RSS feeds and save as JSON"

    def add_arguments(self, parser):
        parser.add_argument(
            '--top',
            type=int,
            default=50,
            help='Maximum number of articles to save (default: 50)'
        )
        parser.add_argument(
            '--no-content',
            action='store_true',
            help='Skip article content extraction'
        )

    def handle(self, *args, **options):
        top = options['top']
        no_content = options['no_content']
        
        self.stdout.write(self.style.SUCCESS('=' * 60))
        self.stdout.write(self.style.SUCCESS('Starting finance article crawler...'))
        self.stdout.write(self.style.SUCCESS('=' * 60))
        
        try:
            results = crawler_main.main(top=top, no_content=no_content)
            
            self.stdout.write(self.style.SUCCESS('=' * 60))
            self.stdout.write(
                self.style.SUCCESS(
                    f'✓ Successfully crawled {len(results)} articles!'
                )
            )
            self.stdout.write(self.style.SUCCESS('=' * 60))
            
        except Exception as e:
            self.stdout.write(self.style.ERROR(f'✗ Error occurred: {str(e)}'))
            raise