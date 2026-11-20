from collections.abc import Generator

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import Engine
from sqlalchemy.orm import Session, sessionmaker

from app.core.config import DbProvider, Settings
from app.db.base import Base
from app.db.engine import create_db_engine
from app.db.models import (  # noqa: F401
    ActivityLog,
    Household,
    HouseholdMember,
    InviteCode,
    Item,
    RefreshToken,
    SocialAccount,
    StorageLocation,
    User,
)
from app.db.session import get_db
from app.main import create_app


@pytest.fixture()
def sqlite_engine() -> Generator[Engine, None, None]:
    cfg = Settings(
        db_provider=DbProvider.SQLITE,
        database_url="sqlite:///:memory:",
    )
    engine = create_db_engine(cfg)
    Base.metadata.create_all(bind=engine)
    yield engine
    Base.metadata.drop_all(bind=engine)
    engine.dispose()


@pytest.fixture()
def db_session(sqlite_engine: Engine) -> Generator[Session, None, None]:
    testing_session_local = sessionmaker(
        bind=sqlite_engine,
        autocommit=False,
        autoflush=False,
        expire_on_commit=False,
    )
    session = testing_session_local()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture()
def client(sqlite_engine: Engine) -> TestClient:
    testing_session_local = sessionmaker(
        bind=sqlite_engine,
        autocommit=False,
        autoflush=False,
        expire_on_commit=False,
    )
    app = create_app()
    app.state.db_engine = sqlite_engine
    app.state.db_provider = DbProvider.SQLITE

    def override_get_db() -> Generator[Session, None, None]:
        session = testing_session_local()
        try:
            yield session
        finally:
            session.close()

    app.dependency_overrides[get_db] = override_get_db
    return TestClient(app)
