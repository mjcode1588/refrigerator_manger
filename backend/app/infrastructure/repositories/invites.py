import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.invite_code import InviteCode as InviteCodeModel
from app.domain.entities import InviteCode


def _to_domain(model: InviteCodeModel) -> InviteCode:
    return InviteCode(
        id=model.id,
        fridge_id=model.fridge_id,
        code=model.code,
        expires_at=model.expires_at,
        created_by=model.created_by,
        used_count=model.used_count,
        max_uses=model.max_uses,
        created_at=model.created_at,
    )


class SqlInviteRepository:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def create(
        self,
        fridge_id: uuid.UUID,
        code: str,
        expires_at,
        created_by: uuid.UUID,
        max_uses: int,
    ) -> InviteCode:
        model = InviteCodeModel(
            fridge_id=fridge_id,
            code=code,
            expires_at=expires_at,
            created_by=created_by,
            max_uses=max_uses,
            used_count=0,
        )
        self._db.add(model)
        await self._db.commit()
        await self._db.refresh(model)
        return _to_domain(model)

    async def get_by_code(self, code: str) -> InviteCode | None:
        result = await self._db.execute(select(InviteCodeModel).where(InviteCodeModel.code == code))
        model = result.scalar_one_or_none()
        return _to_domain(model) if model else None

    async def increment_used(self, invite_id: uuid.UUID) -> None:
        result = await self._db.execute(select(InviteCodeModel).where(InviteCodeModel.id == invite_id))
        model = result.scalar_one_or_none()
        if not model:
            return
        model.used_count += 1
        await self._db.commit()
