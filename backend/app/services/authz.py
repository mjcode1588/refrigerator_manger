import uuid

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.fridge_member import FridgeMember


async def ensure_fridge_member(db: AsyncSession, fridge_id: uuid.UUID, user_id: uuid.UUID) -> None:
    result = await db.execute(
        select(FridgeMember.id).where(FridgeMember.fridge_id == fridge_id, FridgeMember.user_id == user_id)
    )
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not a fridge member")
