import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.notification import Notification as NotificationModel
from app.domain.entities import Notification


def _to_domain(model: NotificationModel) -> Notification:
    return Notification(
        id=model.id,
        user_id=model.user_id,
        fridge_id=model.fridge_id,
        item_id=model.item_id,
        type=model.type,
        status=model.status,
        created_at=model.created_at,
    )


class SqlNotificationRepository:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def exists(self, user_id: uuid.UUID, item_id: uuid.UUID | None, notif_type: str) -> bool:
        result = await self._db.execute(
            select(NotificationModel.id).where(
                NotificationModel.user_id == user_id,
                NotificationModel.item_id == item_id,
                NotificationModel.type == notif_type,
            )
        )
        return result.scalar_one_or_none() is not None

    async def create(
        self,
        user_id: uuid.UUID,
        fridge_id: uuid.UUID,
        item_id: uuid.UUID | None,
        notif_type: str,
    ) -> Notification:
        model = NotificationModel(
            user_id=user_id,
            fridge_id=fridge_id,
            item_id=item_id,
            type=notif_type,
            status="unread",
        )
        self._db.add(model)
        await self._db.commit()
        await self._db.refresh(model)
        return _to_domain(model)
