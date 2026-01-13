from pydantic import AnyHttpUrl
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)

    app_name: str = "Refrigerator Manager API"
    api_v1_str: str = "/api/v1"

    database_url: str

    jwt_secret_key: str
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60 * 24 * 7

    cron_secret: str = ""
    cron_enabled: bool = False

    gemini_api_key: str | None = None
    llm_mode: str = "stub"

    upload_dir: str = "uploads"
    image_base_url: AnyHttpUrl | None = None

    default_expiring_days: int = 3
    invite_expires_hours: int = 24 * 7
    invite_max_uses: int = 1

    cors_allow_origins: str = ""


settings = Settings()
