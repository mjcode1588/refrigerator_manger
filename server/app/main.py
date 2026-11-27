import logging
from pathlib import Path

from fastapi import FastAPI, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles

from app.api.routers.activities import router as activities_router
from app.api.routers.auth import router as auth_router
from app.api.routers.households import router as households_router
from app.api.routers.items import router as items_router
from app.api.routers.locations import router as locations_router
from app.api.routers.media import router as media_router
from app.api.routers.shopping_items import router as shopping_items_router
from app.core.config import settings
from app.core.errors import register_exception_handlers
from app.core.rate_limit import RateLimiter
from app.db.engine import check_db_health
from app.db.session import engine as default_engine

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    settings.validate_production_security()
    app = FastAPI(
        title=settings.app_name,
        openapi_url=f"{settings.api_v1_prefix}/openapi.json",
    )

    app.state.rate_limiter = RateLimiter()
    register_exception_handlers(app)

    if settings.cors_origins_list:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=settings.cors_origins_list,
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )

    @app.get(f"{settings.api_v1_prefix}/health", tags=["system"])
    def health_check() -> JSONResponse:
        db_engine = getattr(app.state, "db_engine", default_engine)
        db_provider = getattr(app.state, "db_provider", settings.db_provider)
        try:
            check_db_health(db_engine, db_provider)
        except Exception:
            logger.exception("DB health check failed")
            return JSONResponse(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                content={
                    "status": "unhealthy",
                    "db_provider": db_provider.value,
                },
            )
        return JSONResponse(
            status_code=status.HTTP_200_OK,
            content={
                "status": "healthy",
                "db_provider": db_provider.value,
                "maintenance": settings.maintenance_mode,
                "maintenance_message": settings.maintenance_message,
                "min_android_version_code": settings.min_android_version_code,
            },
        )

    app.include_router(auth_router, prefix=settings.api_v1_prefix)
    app.include_router(households_router, prefix=settings.api_v1_prefix)
    app.include_router(locations_router, prefix=settings.api_v1_prefix)
    app.include_router(items_router, prefix=settings.api_v1_prefix)
    app.include_router(activities_router, prefix=settings.api_v1_prefix)
    app.include_router(media_router, prefix=settings.api_v1_prefix)
    app.include_router(shopping_items_router, prefix=settings.api_v1_prefix)

    # Phase 3 웹 기능 실험용 정적 UI 마운트 (로컬 환경)
    web_proto_dir = Path(__file__).resolve().parents[2] / "web-proto"
    if settings.app_env == "local" and web_proto_dir.is_dir():
        app.mount("/web", StaticFiles(directory=str(web_proto_dir), html=True), name="web-proto")

        @app.get("/", include_in_schema=False)
        def root_redirect() -> RedirectResponse:
            return RedirectResponse(url="/web/")

    return app


app = create_app()
