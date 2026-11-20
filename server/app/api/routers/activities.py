import json

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_household_membership
from app.db.models import ActivityLog, HouseholdMember, User
from app.db.session import get_db
from app.schemas.activity import ActivityListResponse, ActivityLogResponse

router = APIRouter(prefix="/households/{household_id}/activities", tags=["activities"])


@router.get("", response_model=ActivityListResponse)
def list_activities(
    household_id: int,
    cursor: int | None = Query(default=None),
    limit: int = Query(default=20, ge=1, le=100),
    _member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> ActivityListResponse:
    stmt = (
        select(ActivityLog, User.nickname)
        .join(User, User.id == ActivityLog.actor_user_id)
        .where(ActivityLog.household_id == household_id)
    )
    if cursor is not None:
        stmt = stmt.where(ActivityLog.id < cursor)
    stmt = stmt.order_by(ActivityLog.id.desc()).limit(limit + 1)

    rows = db.execute(stmt).all()
    has_next = len(rows) > limit
    sliced = rows[:limit]
    next_cursor = sliced[-1][0].id if has_next and sliced else None

    return ActivityListResponse(
        items=[
            ActivityLogResponse(
                id=log.id,
                household_id=log.household_id,
                actor_user_id=log.actor_user_id,
                actor_nickname=nickname,
                entity_type=log.entity_type,
                entity_id=log.entity_id,
                action_type=log.action_type,
                detail=json.loads(log.detail_json) if log.detail_json else None,
                created_at=log.created_at,
            )
            for log, nickname in sliced
        ],
        next_cursor=next_cursor,
    )
