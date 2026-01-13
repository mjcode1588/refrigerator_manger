from __future__ import annotations

from datetime import date
import uuid

from app.application.errors import ForbiddenError, NotFoundError
from app.application.ports import FridgeRepository, ItemRepository, LLMClient
from app.domain.entities import ItemCandidate
from app.domain.policies import determine_status


async def ingest_candidates(
    *,
    llm_client: LLMClient,
    text: str | None,
    image_names: list[str],
) -> list[ItemCandidate]:
    candidates: list[ItemCandidate] = []
    if text:
        candidates.extend(llm_client.extract_candidates_from_text(text))
    if image_names:
        candidates.extend(llm_client.extract_candidates_from_images(image_names))
    return candidates


async def confirm_items(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    fridge_id: uuid.UUID,
    user_id: uuid.UUID,
    items: list[dict],
    expiring_days: int,
) -> list:
    if not await fridge_repo.is_member(fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    today = date.today()
    prepared: list[dict] = []
    for item in items:
        expiry_date = item.get("expiry_date")
        status = determine_status(expiry_date, today, expiring_days)
        prepared.append({**item, "status": status})
    return await item_repo.create_items(fridge_id, prepared)


async def list_items(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    fridge_id: uuid.UUID,
    user_id: uuid.UUID,
) -> list:
    if not await fridge_repo.is_member(fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    return await item_repo.list_items(fridge_id)


async def update_item(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    item_id: uuid.UUID,
    user_id: uuid.UUID,
    updates: dict,
    expiring_days: int,
) -> object:
    item = await item_repo.get_by_id(item_id)
    if not item:
        raise NotFoundError("Item not found")
    if not await fridge_repo.is_member(item.fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    if "expiry_date" in updates and "status" not in updates:
        updates["status"] = determine_status(updates.get("expiry_date"), date.today(), expiring_days)
    return await item_repo.update_item(item_id, updates)


async def delete_item(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    item_id: uuid.UUID,
    user_id: uuid.UUID,
) -> None:
    item = await item_repo.get_by_id(item_id)
    if not item:
        raise NotFoundError("Item not found")
    if not await fridge_repo.is_member(item.fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    deleted = await item_repo.delete_item(item_id)
    if not deleted:
        raise NotFoundError("Item not found")


async def list_expiring(
    *,
    fridge_repo: FridgeRepository,
    item_repo: ItemRepository,
    fridge_id: uuid.UUID,
    user_id: uuid.UUID,
    days: int,
) -> list:
    if not await fridge_repo.is_member(fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    return await item_repo.list_expiring(fridge_id, days)
