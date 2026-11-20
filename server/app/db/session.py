from collections.abc import Generator

from sqlalchemy.orm import Session, sessionmaker

from app.core.config import settings
from app.db.engine import create_db_engine

engine = create_db_engine(settings)

SessionLocal = sessionmaker(
    bind=engine,
    autocommit=False,
    autoflush=False,
    expire_on_commit=False,
)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
