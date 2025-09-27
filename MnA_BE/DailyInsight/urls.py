from django.urls import path, include
from . import views

urlpatterns = [
    path('sample1/', include('DailyInsight.sample1.urls')),
    path('sample2/', include('DailyInsight.sample2.urls'))
]