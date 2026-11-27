from datetime import UTC, datetime

from fastapi import APIRouter, Depends, Response, status
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.api.deps import get_household_membership
from app.core.errors import AppError
from app.db.models import HouseholdMember, ShoppingItem, User
from app.db.session import get_db
from app.schemas.shopping_item import (
    ClearPurchasedResponse,
    CreateShoppingItemRequest,
    ShoppingItemResponse,
    UpdateShoppingItemRequest,
)
from app.services.activity import record_activity

router = APIRouter(
    prefix="/households/{household_id}/shopping-items",
    tags=["shopping-items"],
)


def _get_shopping_item_or_404(
    db: Session, household_id: int, shopping_item_id: int
) -> ShoppingItem:
    item = db.get(ShoppingItem, shopping_item_id)
    if not item or item.household_id != household_id:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "SHOPPING_ITEM_NOT_FOUND",
            "장보기 품목을 찾을 수 없습니다.",
        )
    return item


def _to_shopping_item_response(
    item: ShoppingItem, nickname_map: dict[int, str]
) -> ShoppingItemResponse:
    return ShoppingItemResponse(
        id=item.id,
        household_id=item.household_id,
        name=item.name,
        category=item.category,
        quantity=float(item.quantity),
        unit=item.unit,
        memo=item.memo,
        is_purchased=item.is_purchased,
        purchased_by=item.purchased_by,
        purchased_by_nickname=(
            nickname_map.get(item.purchased_by) if item.purchased_by is not None else None
        ),
        purchased_at=item.purchased_at,
        created_by=item.created_by,
        created_at=item.created_at,
        updated_at=item.updated_at,
    )


def _load_purchaser_nicknames(
    db: Session, items: list[ShoppingItem]
) -> dict[int, str]:
    purchaser_ids = {item.purchased_by for item in items if item.purchased_by is not None}
    if not purchaser_ids:
        return {}
    rows = db.execute(
        select(User.id, User.nickname).where(User.id.in_(purchaser_ids))
    ).all()
    return {row[0]: row[1] for row in rows}


@router.get("", response_model=list[ShoppingItemResponse])
def list_shopping_items(
    household_id: int,
    membership: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> list[ShoppingItemResponse]:
    items = list(
        db.execute(
            select(ShoppingItem).where(
                ShoppingItem.household_id == membership.household_id
            )
        )
        .scalars()
        .all()
    )
    items.sort(
        key=lambda item: (
            1 if item.is_purchased else 0,
            -(
                item.purchased_at.timestamp()
                if item.is_purchased and item.purchased_at is not None
                else item.created_at.timestamp()
            ),
            -item.id,
        )
    )
    nickname_map = _load_purchaser_nicknames(db, items)
    return [_to_shopping_item_response(item, nickname_map) for item in items]


@router.post(
    "", response_model=ShoppingItemResponse, status_code=status.HTTP_201_CREATED
)
def create_shopping_item(
    household_id: int,
    payload: CreateShoppingItemRequest,
    membership: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ShoppingItemResponse:
    item = ShoppingItem(
        household_id=membership.household_id,
        name=payload.name,
        category=payload.category.value,
        quantity=payload.quantity,
        unit=payload.unit,
        memo=payload.memo,
        is_purchased=False,
        purchased_by=None,
        purchased_at=None,
        created_by=membership.user_id,
        updated_by=membership.user_id,
    )
    db.add(item)
    db.flush()

    record_activity(
        db,
        household_id=membership.household_id,
        actor_user_id=membership.user_id,
        entity_type="SHOPPING_ITEM",
        entity_id=item.id,
        action_type="CREATED",
        detail={"name": item.name, "quantity": float(item.quantity), "unit": item.unit},
    )
    db.commit()
    db.refresh(item)
    return _to_shopping_item_response(item, {})


@router.patch("/{shopping_item_id}", response_model=ShoppingItemResponse)
def update_shopping_item(
    household_id: int,
    shopping_item_id: int,
    payload: UpdateShoppingItemRequest,
    membership: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ShoppingItemResponse:
    item = _get_shopping_item_or_404(db, membership.household_id, shopping_item_id)
    was_purchased = item.is_purchased

    if payload.name is not None:
        item.name = payload.name
    if payload.category is not None:
        item.category = payload.category.value
    if payload.quantity is not None:
        item.quantity = payload.quantity
    if payload.unit is not None:
        item.unit = payload.unit
    if payload.clear_memo:
        item.memo = None
    elif payload.memo is not None:
        item.memo = payload.memo

    action_type = "UPDATED"
    if payload.is_purchased is not None:
        if payload.is_purchased and not was_purchased:
            item.is_purchased = True
            item.purchased_by = membership.user_id
            item.purchased_at = datetime.now(UTC)
            action_type = "PURCHASED"
        elif not payload.is_purchased and was_purchased:
            item.is_purchased = False
            item.purchased_by = None
            item.purchased_at = None
            action_type = "UNPURCHASED"

    item.updated_by = membership.user_id
    record_activity(
        db,
        household_id=membership.household_id,
        actor_user_id=membership.user_id,
        entity_type="SHOPPING_ITEM",
        entity_id=item.id,
        action_type=action_type,
        detail={"name": item.name, "is_purchased": item.is_purchased},
    )
    db.commit()
    db.refresh(item)
    nickname_map = _load_purchaser_nicknames(db, [item])
    return _to_shopping_item_response(item, nickname_map)


@router.delete("/{shopping_item_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_shopping_item(
    household_id: int,
    shopping_item_id: int,
    membership: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> Response:
    item = _get_shopping_item_or_404(db, membership.household_id, shopping_item_id)
    record_activity(
        db,
        household_id=membership.household_id,
        actor_user_id=membership.user_id,
        entity_type="SHOPPING_ITEM",
        entity_id=item.id,
        action_type="DELETED",
        detail={"name": item.name},
    )
    db.delete(item)
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/clear-purchased", response_model=ClearPurchasedResponse)
def clear_purchased_shopping_items(
    household_id: int,
    membership: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ClearPurchasedResponse:
    result = db.execute(
        delete(ShoppingItem).where(
            ShoppingItem.household_id == membership.household_id,
            ShoppingItem.is_purchased.is_(True),
        )
    )
    deleted_count = int(result.rowcount or 0)
    if deleted_count > 0:
        record_activity(
            db,
            household_id=membership.household_id,
            actor_user_id=membership.user_id,
            entity_type="SHOPPING_ITEM",
            entity_id=membership.household_id,
            action_type="CLEARED",
            detail={"deleted_count": deleted_count},
        )
    db.commit()
    return ClearPurchasedResponse(deleted_count=deleted_count)
