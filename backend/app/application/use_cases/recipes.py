import uuid
from datetime import date

from app.application.errors import ForbiddenError
from app.application.ports import FridgeRepository, ItemRepository, LLMClient


async def suggest_recipes(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    llm_client: LLMClient,
    fridge_id: uuid.UUID,
    user_id: uuid.UUID,
    prefer_expiring_first: bool,
) -> list:
    if not await fridge_repo.is_member(fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    items = await item_repo.list_items(fridge_id)
    if prefer_expiring_first:
        items = sorted(items, key=lambda item: item.expiry_date or date.max)
    names = [item.name for item in items if item.name]
    return llm_client.suggest_recipes(names, prefer_expiring_first)
