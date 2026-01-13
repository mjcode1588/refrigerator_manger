from dataclasses import dataclass
from datetime import date, timedelta
import uuid

import pytest

from app.application.errors import ForbiddenError
from app.application.use_cases.items import confirm_items
from app.domain.entities import FridgeItem


@dataclass
class FakeFridgeRepo:
    members: set[tuple[uuid.UUID, uuid.UUID]]

    async def is_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID) -> bool:
        return (fridge_id, user_id) in self.members


@dataclass
class FakeItemRepo:
    created: list[FridgeItem]

    async def create_items(self, fridge_id: uuid.UUID, items: list[dict]) -> list[FridgeItem]:
        result: list[FridgeItem] = []
        for item in items:
            result.append(
                FridgeItem(
                    id=uuid.uuid4(),
                    fridge_id=fridge_id,
                    name=item["name"],
                    category=item.get("category"),
                    quantity=item.get("quantity"),
                    unit=item.get("unit"),
                    purchase_date=item.get("purchase_date"),
                    expiry_date=item.get("expiry_date"),
                    storage_location=item.get("storage_location"),
                    status=item.get("status", "fresh"),
                    notes=item.get("notes"),
                )
            )
        self.created.extend(result)
        return result


@pytest.mark.asyncio
async def test_confirm_items_requires_membership():
    fridge_repo = FakeFridgeRepo(members=set())
    item_repo = FakeItemRepo(created=[])

    with pytest.raises(ForbiddenError):
        await confirm_items(
            fridge_repo=fridge_repo,
            item_repo=item_repo,
            fridge_id=uuid.uuid4(),
            user_id=uuid.uuid4(),
            items=[{"name": "milk"}],
            expiring_days=3,
        )


@pytest.mark.asyncio
async def test_confirm_items_sets_status():
    fridge_id = uuid.uuid4()
    user_id = uuid.uuid4()
    fridge_repo = FakeFridgeRepo(members={(fridge_id, user_id)})
    item_repo = FakeItemRepo(created=[])

    items = [
        {"name": "milk", "expiry_date": date.today() + timedelta(days=1)},
        {"name": "rice", "expiry_date": date.today() + timedelta(days=10)},
    ]

    created = await confirm_items(
        fridge_repo=fridge_repo,
        item_repo=item_repo,
        fridge_id=fridge_id,
        user_id=user_id,
        items=items,
        expiring_days=3,
    )

    assert created[0].status == "expiring"
    assert created[1].status == "fresh"
