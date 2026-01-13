from datetime import date, timedelta

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.db.models.fridge_item import FridgeItem
from app.db.models.fridge_member import FridgeMember
from app.db.models.notification import Notification
from app.services.items import determine_status


async def generate_expiry_notifications(db: AsyncSession, days: int | None = None) -> int:
    today = date.today()
    threshold = days if days is not None else settings.default_expiring_days
    cutoff = today + timedelta(days=threshold)

    result = await db.execute(
        select(FridgeItem).where(FridgeItem.expiry_date.is_not(None)).where(FridgeItem.expiry_date <= cutoff)
    )
    items = result.scalars().all()
    created_count = 0

    for item in items:
        status = determine_status(item.expiry_date, today, threshold)
        item.status = status

        member_result = await db.execute(
            select(FridgeMember.user_id).where(FridgeMember.fridge_id == item.fridge_id)
        )
        member_ids = [row[0] for row in member_result.all()]
        notif_type = "expired" if status == "expired" else "expiring"

        for user_id in member_ids:
            existing = await db.execute(
                select(Notification.id).where(
                    Notification.user_id == user_id,
                    Notification.item_id == item.id,
                    Notification.type == notif_type,
                )
            )
            if existing.scalar_one_or_none():
                continue
            db.add(
                Notification(
                    user_id=user_id,
                    fridge_id=item.fridge_id,
                    item_id=item.id,
                    type=notif_type,
                    status="unread",
                )
            )
            created_count += 1

    await db.commit()
    return created_count
