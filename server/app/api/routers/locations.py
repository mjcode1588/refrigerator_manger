from typing import Literal

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session

from app.api.deps import get_household_membership
from app.core.errors import AppError
from app.db.models import HouseholdMember, Item, StorageLocation
from app.db.session import get_db
from app.schemas.location import (
    CreateLocationRequest,
    LocationNodeResponse,
    UpdateLocationRequest,
)
from app.services.activity import record_activity
from app.services.locations import (
    LocationNodeInfo,
    build_location_paths,
    build_location_tree,
    collect_descendant_ids,
    would_create_cycle,
)

router = APIRouter(prefix="/households/{household_id}/locations", tags=["locations"])


def _to_node_response(node: LocationNodeInfo) -> LocationNodeResponse:
    return LocationNodeResponse(
        id=node.location.id,
        household_id=node.location.household_id,
        parent_id=node.location.parent_id,
        name=node.location.name,
        storage_type=node.location.storage_type,
        icon=node.location.icon,
        sort_order=node.location.sort_order,
        depth=node.depth,
        full_path=node.full_path,
        children=[_to_node_response(c) for c in node.children],
    )


def _get_household_locations(db: Session, household_id: int) -> list[StorageLocation]:
    stmt = (
        select(StorageLocation)
        .where(StorageLocation.household_id == household_id)
        .order_by(StorageLocation.sort_order, StorageLocation.id)
    )
    return list(db.execute(stmt).scalars().all())


@router.get("", response_model=list[LocationNodeResponse])
def list_locations(
    household_id: int,
    view_format: Literal["tree", "flat"] = Query(default="tree", alias="format"),
    _member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> list[LocationNodeResponse]:
    locations = _get_household_locations(db, household_id)
    if view_format == "tree":
        return [_to_node_response(n) for n in build_location_tree(locations)]

    paths = build_location_paths(locations)
    return [
        LocationNodeResponse(
            id=loc.id,
            household_id=loc.household_id,
            parent_id=loc.parent_id,
            name=loc.name,
            storage_type=loc.storage_type,
            icon=loc.icon,
            sort_order=loc.sort_order,
            depth=paths[loc.id][0],
            full_path=paths[loc.id][1],
            children=[],
        )
        for loc in locations
    ]


@router.post("", response_model=LocationNodeResponse, status_code=status.HTTP_201_CREATED)
def create_location(
    household_id: int,
    body: CreateLocationRequest,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> LocationNodeResponse:
    if body.parent_id is not None:
        parent = db.get(StorageLocation, body.parent_id)
        if parent is None or parent.household_id != household_id:
            raise AppError("PARENT_LOCATION_NOT_FOUND", "상위 보관 장소를 찾을 수 없습니다.", 404)

    loc = StorageLocation(
        household_id=household_id,
        parent_id=body.parent_id,
        name=body.name.strip(),
        storage_type=body.storage_type.value,
        icon=body.icon,
        sort_order=body.sort_order,
    )
    db.add(loc)
    db.flush()

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="LOCATION",
        entity_id=loc.id,
        action_type="CREATED",
        detail={"name": loc.name},
    )
    db.commit()

    locations = _get_household_locations(db, household_id)
    paths = build_location_paths(locations)
    depth, full_path = paths[loc.id]
    return LocationNodeResponse(
        id=loc.id,
        household_id=loc.household_id,
        parent_id=loc.parent_id,
        name=loc.name,
        storage_type=loc.storage_type,
        icon=loc.icon,
        sort_order=loc.sort_order,
        depth=depth,
        full_path=full_path,
        children=[],
    )


@router.patch("/{location_id}", response_model=LocationNodeResponse)
def update_location(
    household_id: int,
    location_id: int,
    body: UpdateLocationRequest,
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> LocationNodeResponse:
    locations = _get_household_locations(db, household_id)
    by_id = {loc.id: loc for loc in locations}
    loc = by_id.get(location_id)
    if loc is None:
        raise AppError("LOCATION_NOT_FOUND", "보관 장소를 찾을 수 없습니다.", 404)

    moved = False
    if body.change_parent or body.parent_id is not None:
        new_parent_id = body.parent_id
        if new_parent_id is not None and new_parent_id not in by_id:
            raise AppError("PARENT_LOCATION_NOT_FOUND", "상위 보관 장소를 찾을 수 없습니다.", 404)
        if would_create_cycle(locations, location_id, new_parent_id):
            raise AppError(
                "LOCATION_CYCLE_DETECTED",
                "자기 자신이나 하위 보관 장소를 상위 장소로 이동할 수 없습니다.",
                400,
            )
        if loc.parent_id != new_parent_id:
            loc.parent_id = new_parent_id
            moved = True

    if body.name is not None:
        loc.name = body.name.strip()
    if body.storage_type is not None:
        loc.storage_type = body.storage_type.value
    if body.icon is not None:
        loc.icon = body.icon
    if body.sort_order is not None:
        loc.sort_order = body.sort_order

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="LOCATION",
        entity_id=loc.id,
        action_type="MOVED" if moved else "UPDATED",
        detail={"name": loc.name},
    )
    db.commit()

    refreshed = _get_household_locations(db, household_id)
    paths = build_location_paths(refreshed)
    depth, full_path = paths[loc.id]
    return LocationNodeResponse(
        id=loc.id,
        household_id=loc.household_id,
        parent_id=loc.parent_id,
        name=loc.name,
        storage_type=loc.storage_type,
        icon=loc.icon,
        sort_order=loc.sort_order,
        depth=depth,
        full_path=full_path,
        children=[],
    )


@router.delete("/{location_id}")
def delete_location(
    household_id: int,
    location_id: int,
    strategy: Literal["restrict", "move", "cascade"] = Query(default="restrict"),
    move_to_location_id: int | None = Query(default=None),
    member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    locations = _get_household_locations(db, household_id)
    by_id = {loc.id: loc for loc in locations}
    target = by_id.get(location_id)
    if target is None:
        raise AppError("LOCATION_NOT_FOUND", "보관 장소를 찾을 수 없습니다.", 404)

    has_children = any(loc.parent_id == location_id for loc in locations)
    active_items_count = (
        db.execute(
            select(Item.id).where(
                Item.household_id == household_id,
                Item.location_id == location_id,
            )
        )
        .scalars()
        .first()
        is not None
    )

    if strategy == "restrict":
        if has_children or active_items_count:
            raise AppError(
                "LOCATION_NOT_EMPTY",
                "하위 보관 장소나 품목이 있어 삭제할 수 없습니다.",
                409,
            )
        db.delete(target)

    elif strategy == "move":
        if move_to_location_id is None or move_to_location_id not in by_id:
            raise AppError(
                "MOVE_TARGET_REQUIRED",
                "이동 대상 보관 장소(move_to_location_id)를 지정해야 합니다.",
                400,
            )
        descendants = collect_descendant_ids(locations, location_id)
        if move_to_location_id in descendants:
            raise AppError(
                "INVALID_MOVE_TARGET",
                "삭제할 장소 또는 하위 장소로 이동할 수 없습니다.",
                400,
            )
        db.execute(
            update(StorageLocation)
            .where(StorageLocation.parent_id == location_id)
            .values(parent_id=move_to_location_id)
        )
        db.execute(
            update(Item)
            .where(Item.location_id == location_id)
            .values(location_id=move_to_location_id)
        )
        db.delete(target)

    elif strategy == "cascade":
        descendants = collect_descendant_ids(locations, location_id)
        db.execute(delete(Item).where(Item.location_id.in_(descendants)))
        # 자식부터 순차 삭제 (SQLite/Oracle FK 호환성 보장)
        paths = build_location_paths(locations)
        ordered = sorted(descendants, key=lambda lid: paths[lid][0], reverse=True)
        for lid in ordered:
            db.execute(delete(StorageLocation).where(StorageLocation.id == lid))

    record_activity(
        db,
        household_id=household_id,
        actor_user_id=member.user_id,
        entity_type="LOCATION",
        entity_id=location_id,
        action_type="DELETED",
        detail={"name": target.name, "strategy": strategy},
    )
    db.commit()
    return {"status": "ok"}
