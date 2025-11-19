from drf_yasg.views import get_schema_view
from rest_framework import permissions
from drf_yasg import openapi

schema_view_v1 = get_schema_view(
    openapi.Info(
        title="API for Daily Insight!",
        default_version='v1',
        description="Stock recommendation API with AI-generated insights",
        contact=openapi.Contact(email="ldh041203@snu.ac.kr"),
        license=openapi.License(name="Team MnA"),
    ),
    public=True,
    permission_classes=(permissions.AllowAny,),
)