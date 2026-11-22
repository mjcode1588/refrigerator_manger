from enum import StrEnum

from pydantic_settings import BaseSettings, SettingsConfigDict

WEAK_JWT_SECRETS = {
    "dev-insecure-secret-change-in-production",
    "change-this-to-a-random-64-byte-hex-secret",
    "secret",
    "changeme",
}


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

    # 소셜 로그인 및 체험 로그인 설정
    allow_dev_login: bool = True
    google_client_id: str | None = None
    kakao_rest_api_key: str | None = None

    # 점검 모드 및 최소 앱 버전
    maintenance_mode: bool = False
    maintenance_message: str | None = None
    min_android_version_code: int = 1

    # 품목 이미지 스토리지 설정 (로컬 /data/media 또는 OCI Object Storage)
    media_storage_dir: str = "./data/media"
    media_public_base_url: str = ""
    media_max_detail_bytes: int = 500 * 1024
    media_max_thumb_bytes: int = 100 * 1024

    # Rate Limiting (IP 기준 슬라이딩 윈도우, 단일 프로세스 메모리)
    rate_limit_enabled: bool = True
    rate_limit_auth_per_minute: int = 20
    rate_limit_join_per_minute: int = 10

    @property
    def is_production(self) -> bool:
        return self.app_env.strip().lower() != "local"

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allow_origins.split(",") if o.strip()]

    def validate_production_security(self) -> None:
        """운영 환경(APP_ENV != local)에서 취약한 설정이면 기동을 차단한다 (Fail-Fast)."""
        if not self.is_production:
            return
        problems: list[str] = []
        secret = self.jwt_secret_key
        if (
            secret in WEAK_JWT_SECRETS
            or secret.startswith(("dev-insecure", "change-this"))
            or len(secret) < 32
        ):
            problems.append("JWT_SECRET_KEY가 기본값이거나 너무 짧습니다(32자 이상 필요).")
        if self.allow_dev_login:
            problems.append("운영 환경에서는 ALLOW_DEV_LOGIN=false 여야 합니다.")
        if problems:
            raise RuntimeError("운영 환경 보안 설정 오류: " + " ".join(problems))


settings = Settings()
