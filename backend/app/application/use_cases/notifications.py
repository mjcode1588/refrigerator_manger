from datetime import date, timedelta

from app.application.ports import FridgeRepository, ItemRepository, NotificationRepository
from app.domain.policies import determine_status


async def generate_expiry_notifications(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    notification_repo: NotificationRepository,
    days: int,
) -> int:
    today = date.today()
    expiring_items = await item_repo.list_expiring_all(days)
    created = 0

    for item in expiring_items:
        status = determine_status(item.expiry_date, today, days)
        if status != item.status:
            await item_repo.update_item(item.id, {"status": status})

        members = await fridge_repo.list_members(item.fridge_id)
        notif_type = "expired" if status == "expired" else "expiring"
        for member in members:
            exists = await notification_repo.exists(member.user_id, item.id, notif_type)
            if exists:
                continue
            await notification_repo.create(member.user_id, item.fridge_id, item.id, notif_type)
            created += 1
    return created
