import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.entities import User
from app.db.models.user import User as UserModel


def _to_domain(model: UserModel) -> User:
    return User(
        id=model.id,
        email=model.email,
        hashed_password=model.hashed_password,
        name=model.name,
        locale=model.locale,
        created_at=model.created_at,
    )


class SqlUserRepository:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def get_by_email(self, email: str) -> User | None:
        result = await self._db.execute(select(UserModel).where(UserModel.email == email))
        model = result.scalar_one_or_none()
        return _to_domain(model) if model else None

    async def get_by_id(self, user_id: uuid.UUID) -> User | None:
        result = await self._db.execute(select(UserModel).where(UserModel.id == user_id))
        model = result.scalar_one_or_none()
        return _to_domain(model) if model else None

    async def create(self, email: str, hashed_password: str, name: str | None, locale: str | None) -> User:
        model = UserModel(email=email, hashed_password=hashed_password, name=name, locale=locale)
        self._db.add(model)
        await self._db.commit()
        await self._db.refresh(model)
        return _to_domain(model)
