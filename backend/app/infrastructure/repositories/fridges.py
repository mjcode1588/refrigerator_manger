import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.fridge import Fridge as FridgeModel
from app.db.models.fridge_member import FridgeMember as FridgeMemberModel
from app.domain.entities import Fridge, FridgeMember


def _to_domain(model: FridgeModel) -> Fridge:
    return Fridge(
        id=model.id,
        name=model.name,
        owner_user_id=model.owner_user_id,
        created_at=model.created_at,
    )


class SqlFridgeRepository:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def get_by_id(self, fridge_id: uuid.UUID) -> Fridge | None:
        result = await self._db.execute(select(FridgeModel).where(FridgeModel.id == fridge_id))
        model = result.scalar_one_or_none()
        return _to_domain(model) if model else None

    async def create(self, name: str, owner_user_id: uuid.UUID) -> Fridge:
        model = FridgeModel(name=name, owner_user_id=owner_user_id)
        self._db.add(model)
        await self._db.commit()
        await self._db.refresh(model)
        member = FridgeMemberModel(fridge_id=model.id, user_id=owner_user_id, role="owner")
        self._db.add(member)
        await self._db.commit()
        return _to_domain(model)

    async def list_members(self, fridge_id: uuid.UUID) -> list[FridgeMember]:
        result = await self._db.execute(
            select(FridgeMemberModel).where(FridgeMemberModel.fridge_id == fridge_id)
        )
        members = result.scalars().all()
        return [
            FridgeMember(
                id=member.id,
                fridge_id=member.fridge_id,
                user_id=member.user_id,
                role=member.role,
                created_at=member.created_at,
            )
            for member in members
        ]

    async def add_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID, role: str) -> None:
        member = FridgeMemberModel(fridge_id=fridge_id, user_id=user_id, role=role)
        self._db.add(member)
        await self._db.commit()

    async def is_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID) -> bool:
        result = await self._db.execute(
            select(FridgeMemberModel.id).where(
                FridgeMemberModel.fridge_id == fridge_id,
                FridgeMemberModel.user_id == user_id,
            )
        )
        return result.scalar_one_or_none() is not None
