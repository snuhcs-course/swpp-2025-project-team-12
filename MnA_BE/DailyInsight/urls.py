from django.urls import path, include
from . import views

urlpatterns = [
    path('user/', include('DailyInsight.user.urls')),
    path('sample/', include('DailyInsight.sample.urls'))
]