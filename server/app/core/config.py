from enum import StrEnum

from pydantic_settings import BaseSettings, SettingsConfigDict


class DbProvider(StrEnum):
    SQLITE = "sqlite"
    POSTGRES = "postgres"
    SUPABASE = "supabase"
    ORACLE = "oracle"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "Refrigerator Manager API"
    app_env: str = "local"
    api_v1_prefix: str = "/api/v1"
    cors_allow_origins: str = "http://localhost:3000,http://localhost:5173"

    # 다중 DB 설정
    db_provider: DbProvider = DbProvider.SQLITE
    database_url: str = "sqlite:///./local_dev.db"
    db_pool_size: int = 5
    db_max_overflow: int = 5
    db_echo: bool = False

    # Supabase Transaction Pooler(6543) 사용 시 prepared statement 비활성화
    supabase_use_transaction_pooler: bool = False

    # Oracle ADB mTLS Wallet 사용 시(선택)
    oracle_wallet_dir: str | None = None
    oracle_wallet_password: str | None = None

    # JWT 설정
    jwt_secret_key: str = "dev-insecure-secret-change-in-production"
    jwt_algorithm: str = "HS256"
    jwt_access_expire_minutes: int = 30
    jwt_refresh_expire_days: int = 30

    # 소셜 로그인 설정
    google_client_id: str | None = None
    kakao_rest_api_key: str | None = None

    # 점검 모드 및 최소 앱 버전
    maintenance_mode: bool = False
    maintenance_message: str | None = None
    min_android_version_code: int = 1

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allow_origins.split(",") if o.strip()]


settings = Settings()
