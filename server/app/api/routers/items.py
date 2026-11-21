from datetime import datetime, timedelta
from decimal import Decimal
from typing import Literal

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.api.deps import get_household_membership
from app.core.errors import AppError
from app.db.models import HouseholdMember, Item, StorageLocation
from app.db.session import get_db
from app.schemas.item import (
    KST,
    ConsumeItemRequest,
    CreateItemRequest,
    ItemCategory,
    ItemResponse,
    ItemStatus,
    PhotoUploadUrlRequest,
    PhotoUploadUrlResponse,
    UpdateItemRequest,
    compute_expiry_meta,
)
from app.services.activity import record_activity
from app.services.locations import build_location_paths, collect_descendant_ids
from app.services.media import (
    build_media_url,
    build_upload_url,
    delete_media_file,
    generate_item_image_keys,
)

router = APIRouter(prefix="/households/{household_id}/items", tags=["items"])


def _get_location_paths(
    db: Session, household_id: int
) -> tuple[list[StorageLocation], dict[int, str]]:
    locations = list(
        db.execute(
            select(StorageLocation).where(StorageLocation.household_id == household_id)
        )
        .scalars()
        .all()
    )
    paths = {lid: meta[1] for lid, meta in build_location_paths(locations).items()}
    return locations, paths


def _to_item_response(
    item: Item, location_paths: dict[int, str], alert_days: int = 3
) -> ItemResponse:
    d_day, expiry_state = compute_expiry_meta(item.expiry_date, alert_days=alert_days)
    return ItemResponse(
        id=item.id,
        household_id=item.household_id,
        location_id=item.location_id,
        location_path=location_paths.get(item.location_id, ""),
        name=item.name,
        category=item.category,
        quantity=float(item.quantity),
        initial_quantity=float(item.initial_quantity),
        unit=item.unit,
        expiry_date=item.expiry_date,
        expiry_type=item.expiry_type,
        d_day=d_day,
        expiry_state=expiry_state,
        status=item.status,
        memo=item.memo,
        detail_image_key=item.detail_image_key,
        thumb_image_key=item.thumb_image_key,
        detail_image_url=build_media_url(item.detail_image_key),
        thumb_image_url=build_media_url(item.thumb_image_key),
        updated_at=item.updated_at,
    )


def _get_item_or_404(db: Session, household_id: int, item_id: int) -> Item:
    item = db.get(Item, item_id)
    if item is None or item.household_id != household_id:
        raise AppError("ITEM_NOT_FOUND", "품목을 찾을 수 없습니다.", 404)
    return item


@router.get("", response_model=list[ItemResponse])
def list_items(
    household_id: int,
    location_id: int | None = Query(default=None),
    include_sublocations: bool = Query(default=True),
    item_status: ItemStatus = Query(default=ItemStatus.ACTIVE, alias="status"),
    category: ItemCategory | None = Query(default=None),
    q: str | None = Query(default=None),
    sort: Literal["expiry", "recent", "name"] = Query(default="expiry"),
    _member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> list[ItemResponse]:
    locations, paths = _get_location_paths(db, household_id)
    stmt = select(Item).where(
        Item.household_id == household_id,
        Item.status == item_status.value,
    )

    if location_id is not None:
        if include_sublocations:
            target_ids = collect_descendant_ids(locations, location_id)
            stmt = stmt.where(Item.location_id.in_(target_ids))
        else:
            stmt = stmt.where(Item.location_id == location_id)

    if category is not None:
        stmt = stmt.where(Item.category == category.value)

    if q and q.strip():
        pattern = f"%{q.strip()}%"
        stmt = stmt.where(or_(Item.name.ilike(pattern), Item.memo.ilike(pattern)))

    items = list(db.execute(stmt).scalars().all())
    if sort == "expiry":
        items.sort(
            key=lambda x: (x.expiry_date is None, x.expiry_date or datetime.max.date(), x.id)
        )
    elif sort == "recent":
        items.sort(key=lambda x: (x.updated_at, x.id), reverse=True)
    elif sort == "name":
        items.sort(key=lambda x: (x.name.lower(), x.id))

    return [_to_item_response(it, paths) for it in items]


@router.get("/expiring", response_model=list[ItemResponse])
def list_expiring_items(
    household_id: int,
    within_days: int = Query(default=7, ge=0, le=90),
    _member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> list[ItemResponse]:
    _, paths = _get_location_paths(db, household_id)
    cutoff = datetime.now(KST).date() + timedelta(days=within_days)
    stmt = (
        select(Item)
        .where(
            Item.household_id == household_id,
            Item.status == ItemStatus.ACTIVE.value,
            Item.expiry_date.is_not(None),
            Item.expiry_date <= cutoff,
        )
        .order_by(Item.expiry_date, Item.id)
    )
    items = list(db.execute(stmt).scalars().all())
    return [_to_item_response(it, paths, alert_days=within_days) for it in items]


@router.post("/photo-upload-url", response_model=PhotoUploadUrlResponse)
def create_photo_upload_url(
    household_id: int,
    body: PhotoUploadUrlRequest,
    _member: HouseholdMember = Depends(get_household_membership),
) -> PhotoUploadUrlResponse:
    detail_key, thumb_key = generate_item_image_keys(household_id)
    return PhotoUploadUrlResponse(
        detail_image_key=detail_key,
        thumb_image_key=thumb_key,
        detail_upload_url=build_upload_url(detail_key),
        thumb_upload_url=build_upload_url(thumb_key),
    )


@router.post("", response_model=ItemResponse, status_code=status.HTTP_201_CREATED)
def create_item(
    household_id: int,
    body: CreateItemRequest,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ItemResponse:
    loc = db.get(StorageLocation, body.location_id)
    if loc is None or loc.household_id != household_id:
        raise AppError("LOCATION_NOT_FOUND", "보관 장소를 찾을 수 없습니다.", 404)

    item = Item(
        household_id=household_id,
        location_id=body.location_id,
        name=body.name.strip(),
        category=body.category.value,
        quantity=body.quantity,
        initial_quantity=body.quantity,
        unit=body.unit.strip(),
        expiry_date=body.expiry_date,
        expiry_type=body.expiry_type.value,
        status=ItemStatus.ACTIVE.value,
        memo=body.memo,
        detail_image_key=body.detail_image_key,
        thumb_image_key=body.thumb_image_key,
        created_by=member.user_id,
        updated_by=member.user_id,
    )
    db.add(item)
    db.flush()

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="ITEM",
        entity_id=item.id,
        action_type="CREATED",
        detail={"name": item.name, "quantity": float(item.quantity), "unit": item.unit},
    )
    db.commit()
    db.refresh(item)

    _, paths = _get_location_paths(db, household_id)
    return _to_item_response(item, paths)


@router.get("/{item_id}", response_model=ItemResponse)
def get_item(
    household_id: int,
    item_id: int,
    _member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ItemResponse:
    item = _get_item_or_404(db, household_id, item_id)
    _, paths = _get_location_paths(db, household_id)
    return _to_item_response(item, paths)


@router.patch("/{item_id}", response_model=ItemResponse)
def update_item(
    household_id: int,
    item_id: int,
    body: UpdateItemRequest,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ItemResponse:
    item = _get_item_or_404(db, household_id, item_id)
    moved = False

    if body.location_id is not None and body.location_id != item.location_id:
        loc = db.get(StorageLocation, body.location_id)
        if loc is None or loc.household_id != household_id:
            raise AppError("LOCATION_NOT_FOUND", "이동할 보관 장소를 찾을 수 없습니다.", 404)
        item.location_id = body.location_id
        moved = True

    if body.name is not None:
        item.name = body.name.strip()
    if body.category is not None:
        item.category = body.category.value
    if body.quantity is not None:
        item.quantity = body.quantity
        if body.quantity > item.initial_quantity:
            item.initial_quantity = body.quantity
    if body.unit is not None:
        item.unit = body.unit.strip()
    if body.clear_expiry_date:
        item.expiry_date = None
    elif body.expiry_date is not None:
        item.expiry_date = body.expiry_date
    if body.expiry_type is not None:
        item.expiry_type = body.expiry_type.value
    if body.memo is not None or "memo" in body.model_fields_set:
        item.memo = body.memo
    if body.clear_images:
        delete_media_file(item.detail_image_key)
        delete_media_file(item.thumb_image_key)
        item.detail_image_key = None
        item.thumb_image_key = None
    else:
        if body.detail_image_key is not None:
            item.detail_image_key = body.detail_image_key
        if body.thumb_image_key is not None:
            item.thumb_image_key = body.thumb_image_key
    item.updated_by = member.user_id

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="ITEM",
        entity_id=item.id,
        action_type="MOVED" if moved else "UPDATED",
        detail={"name": item.name, "quantity": float(item.quantity), "unit": item.unit},
    )
    db.commit()
    db.refresh(item)

    _, paths = _get_location_paths(db, household_id)
    return _to_item_response(item, paths)


@router.post("/{item_id}/consume", response_model=ItemResponse)
def consume_item(
    household_id: int,
    item_id: int,
    body: ConsumeItemRequest | None = None,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ItemResponse:
    item = _get_item_or_404(db, household_id, item_id)
    amount = body.amount if body is not None else None
    remaining_quantity = body.remaining_quantity if body is not None else None
    note = body.note if body is not None else None
    before_qty = Decimal(item.quantity)

    if remaining_quantity is not None:
        if remaining_quantity > before_qty:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail={
                    "code": "INVALID_REMAINING_QUANTITY",
                    "message": "남은 수량은 현재 보유 수량보다 클 수 없습니다.",
                },
            )
        item.quantity = remaining_quantity.quantize(Decimal("0.01"))
        consumed_amount = (before_qty - item.quantity).quantize(Decimal("0.01"))
        if item.quantity <= Decimal("0.00"):
            item.quantity = Decimal("0.00")
            item.status = ItemStatus.CONSUMED.value
    elif amount is None or amount >= before_qty:
        item.quantity = Decimal("0.00")
        item.status = ItemStatus.CONSUMED.value
        consumed_amount = before_qty
    else:
        item.quantity = (before_qty - amount).quantize(Decimal("0.01"))
        consumed_amount = amount.quantize(Decimal("0.01"))

    if note is not None:
        item.memo = note

    item.updated_by = member.user_id
    detail_payload: dict[str, object] = {
        "name": item.name,
        "consumed": float(consumed_amount),
        "remaining": float(item.quantity),
        "unit": item.unit,
    }
    if note is not None:
        detail_payload["note"] = note

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="ITEM",
        entity_id=item.id,
        action_type="CONSUMED",
        detail=detail_payload,
    )
    db.commit()
    db.refresh(item)

    _, paths = _get_location_paths(db, household_id)
    return _to_item_response(item, paths)


@router.post("/{item_id}/discard", response_model=ItemResponse)
def discard_item(
    household_id: int,
    item_id: int,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ItemResponse:
    item = _get_item_or_404(db, household_id, item_id)
    item.status = ItemStatus.DISCARDED.value
    item.updated_by = member.user_id

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="ITEM",
        entity_id=item.id,
        action_type="DISCARDED",
        detail={"name": item.name, "quantity": float(item.quantity), "unit": item.unit},
    )
    db.commit()
    db.refresh(item)

    _, paths = _get_location_paths(db, household_id)
    return _to_item_response(item, paths)


@router.delete("/{item_id}")
def delete_item(
    household_id: int,
    item_id: int,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    item = _get_item_or_404(db, household_id, item_id)
    name = item.name
    db.delete(item)
    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="ITEM",
        entity_id=item_id,
        action_type="DELETED",
        detail={"name": name},
    )
    db.commit()
    return {"status": "ok"}
