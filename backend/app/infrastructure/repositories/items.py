import uuid
from datetime import date, timedelta

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.fridge_item import FridgeItem as FridgeItemModel
from app.domain.entities import FridgeItem


def _to_domain(model: FridgeItemModel) -> FridgeItem:
    return FridgeItem(
        id=model.id,
        fridge_id=model.fridge_id,
        name=model.name,
        category=model.category,
        quantity=model.quantity,
        unit=model.unit,
        purchase_date=model.purchase_date,
        expiry_date=model.expiry_date,
        storage_location=model.storage_location,
        status=model.status,
        notes=model.notes,
        created_at=model.created_at,
        updated_at=model.updated_at,
    )


class SqlItemRepository:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def create_items(self, fridge_id: uuid.UUID, items: list[dict]) -> list[FridgeItem]:
        models: list[FridgeItemModel] = []
        for item in items:
            model = FridgeItemModel(fridge_id=fridge_id, **item)
            self._db.add(model)
            models.append(model)
        await self._db.commit()
        for model in models:
            await self._db.refresh(model)
        return [_to_domain(model) for model in models]

    async def list_items(self, fridge_id: uuid.UUID) -> list[FridgeItem]:
        result = await self._db.execute(select(FridgeItemModel).where(FridgeItemModel.fridge_id == fridge_id))
        return [_to_domain(model) for model in result.scalars().all()]

    async def get_by_id(self, item_id: uuid.UUID) -> FridgeItem | None:
        result = await self._db.execute(select(FridgeItemModel).where(FridgeItemModel.id == item_id))
        model = result.scalar_one_or_none()
        return _to_domain(model) if model else None

    async def update_item(self, item_id: uuid.UUID, updates: dict) -> FridgeItem | None:
        result = await self._db.execute(select(FridgeItemModel).where(FridgeItemModel.id == item_id))
        model = result.scalar_one_or_none()
        if not model:
            return None
        for key, value in updates.items():
            setattr(model, key, value)
        await self._db.commit()
        await self._db.refresh(model)
        return _to_domain(model)

    async def delete_item(self, item_id: uuid.UUID) -> bool:
        result = await self._db.execute(select(FridgeItemModel).where(FridgeItemModel.id == item_id))
        model = result.scalar_one_or_none()
        if not model:
            return False
        await self._db.delete(model)
        await self._db.commit()
        return True

    async def list_expiring(self, fridge_id: uuid.UUID, days: int) -> list[FridgeItem]:
        cutoff = date.today() + timedelta(days=days)
        result = await self._db.execute(
            select(FridgeItemModel)
            .where(FridgeItemModel.fridge_id == fridge_id)
            .where(FridgeItemModel.expiry_date.is_not(None))
            .where(FridgeItemModel.expiry_date <= cutoff)
        )
        return [_to_domain(model) for model in result.scalars().all()]

    async def list_expiring_all(self, days: int) -> list[FridgeItem]:
        cutoff = date.today() + timedelta(days=days)
        result = await self._db.execute(
            select(FridgeItemModel)
            .where(FridgeItemModel.expiry_date.is_not(None))
            .where(FridgeItemModel.expiry_date <= cutoff)
        )
        return [_to_domain(model) for model in result.scalars().all()]
