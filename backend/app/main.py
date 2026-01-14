from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.interfaces.api.routers.auth import router as auth_router
from app.interfaces.api.routers.fridges import router as fridges_router
from app.interfaces.api.routers.items import router as items_router
from app.interfaces.api.routers.notifications import router as notifications_router
from app.interfaces.api.routers.recipes import router as recipes_router


def create_app() -> FastAPI:
    app = FastAPI(title=settings.app_name, openapi_url=f"{settings.api_v1_str}/openapi.json")

    if settings.cors_allow_origins:
        origins = [origin.strip() for origin in settings.cors_allow_origins.split(",") if origin.strip()]
        app.add_middleware(
            CORSMiddleware,
            allow_origins=origins,
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )

    # Health check endpoint for Docker
    @app.get("/health")
    async def health_check():
        return {"status": "healthy"}

    app.include_router(auth_router, prefix=settings.api_v1_str)
    app.include_router(fridges_router, prefix=settings.api_v1_str)
    app.include_router(items_router, prefix=settings.api_v1_str)
    app.include_router(recipes_router, prefix=settings.api_v1_str)
    app.include_router(notifications_router, prefix=settings.api_v1_str)

    return app


app = create_app()
