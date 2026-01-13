from fastapi import APIRouter, Depends

from app.application.use_cases.notifications import generate_expiry_notifications
from app.core.config import settings
from app.infrastructure.repositories.fridges import SqlFridgeRepository
from app.infrastructure.repositories.items import SqlItemRepository
from app.infrastructure.repositories.notifications import SqlNotificationRepository
from app.interfaces.api.deps import get_fridge_repo, get_item_repo, get_notification_repo, verify_cron_secret

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.post("/cron/expiring")
async def cron_expiring_handler(
    days: int = settings.default_expiring_days,
    _: None = Depends(verify_cron_secret),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
    notification_repo: SqlNotificationRepository = Depends(get_notification_repo),
) -> dict:
    created = await generate_expiry_notifications(
        fridge_repo=fridge_repo,
        item_repo=item_repo,
        notification_repo=notification_repo,
        days=days,
    )
    return {"created": created}
