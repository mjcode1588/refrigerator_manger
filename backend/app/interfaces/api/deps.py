from typing import AsyncGenerator
import uuid

from fastapi import Depends, Header, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.db.session import AsyncSessionLocal
from app.domain.entities import User
from app.infrastructure.llm.client import build_llm_client
from app.infrastructure.repositories.fridges import SqlFridgeRepository
from app.infrastructure.repositories.invites import SqlInviteRepository
from app.infrastructure.repositories.items import SqlItemRepository
from app.infrastructure.repositories.notifications import SqlNotificationRepository
from app.infrastructure.repositories.users import SqlUserRepository
from app.infrastructure.storage.image_store import LocalImageStorage

oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{settings.api_v1_str}/auth/login")


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        yield session


def get_user_repo(db: AsyncSession = Depends(get_db)) -> SqlUserRepository:
    return SqlUserRepository(db)


def get_fridge_repo(db: AsyncSession = Depends(get_db)) -> SqlFridgeRepository:
    return SqlFridgeRepository(db)


def get_invite_repo(db: AsyncSession = Depends(get_db)) -> SqlInviteRepository:
    return SqlInviteRepository(db)


def get_item_repo(db: AsyncSession = Depends(get_db)) -> SqlItemRepository:
    return SqlItemRepository(db)


def get_notification_repo(db: AsyncSession = Depends(get_db)) -> SqlNotificationRepository:
    return SqlNotificationRepository(db)


def get_llm_client():
    return build_llm_client()


def get_image_storage() -> LocalImageStorage:
    return LocalImageStorage()


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    user_repo: SqlUserRepository = Depends(get_user_repo),
) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, settings.jwt_secret_key, algorithms=[settings.jwt_algorithm])
        user_id: str | None = payload.get("sub")
    except JWTError as exc:
        raise credentials_exception from exc
    if not user_id:
        raise credentials_exception
    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError as exc:
        raise credentials_exception from exc
    user = await user_repo.get_by_id(user_uuid)
    if not user:
        raise credentials_exception
    return user


def verify_cron_secret(x_cron_secret: str | None = Header(default=None)) -> None:
    if not settings.cron_secret:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Cron secret not configured")
    if x_cron_secret != settings.cron_secret:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid cron secret")
