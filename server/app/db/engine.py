from typing import Any

from sqlalchemy import Engine, create_engine, text
from sqlalchemy.pool import StaticPool

from app.core.config import DbProvider, Settings


def build_engine_kwargs(cfg: Settings) -> dict[str, Any]:
    """DB_PROVIDER에 따라 SQLAlchemy create_engine 옵션을 동적으로 구성합니다."""
    kwargs: dict[str, Any] = {
        "echo": cfg.db_echo,
        "future": True,
    }
    connect_args: dict[str, Any] = {}

    if cfg.db_provider == DbProvider.SQLITE:
        connect_args["check_same_thread"] = False
        if cfg.database_url in ("sqlite://", "sqlite:///:memory:"):
            kwargs["poolclass"] = StaticPool
    else:
        kwargs["pool_pre_ping"] = True
        kwargs["pool_size"] = cfg.db_pool_size
        kwargs["max_overflow"] = cfg.db_max_overflow

        if cfg.db_provider == DbProvider.ORACLE:
            if cfg.oracle_wallet_dir:
                connect_args["config_dir"] = cfg.oracle_wallet_dir
                connect_args["wallet_location"] = cfg.oracle_wallet_dir
            if cfg.oracle_wallet_password:
                connect_args["wallet_password"] = cfg.oracle_wallet_password

        elif cfg.db_provider == DbProvider.SUPABASE and cfg.supabase_use_transaction_pooler:
            # Supavisor Transaction Pooler(6543) 사용 시 prepared statement 충돌 방지
            connect_args["prepare_threshold"] = None

    if connect_args:
        kwargs["connect_args"] = connect_args

    return kwargs


def create_db_engine(cfg: Settings) -> Engine:
    kwargs = build_engine_kwargs(cfg)
    return create_engine(cfg.database_url, **kwargs)


def get_healthcheck_sql(provider: DbProvider) -> str:
    if provider == DbProvider.ORACLE:
        return "SELECT 1 FROM DUAL"
    return "SELECT 1"


def check_db_health(engine: Engine, provider: DbProvider) -> bool:
    sql = get_healthcheck_sql(provider)
    with engine.connect() as conn:
        conn.execute(text(sql))
    return True
