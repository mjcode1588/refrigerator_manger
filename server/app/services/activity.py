import json
from typing import Any

from sqlalchemy.orm import Session

from app.db.models import ActivityLog


def record_activity(
    db: Session,
    *,
    household_id: int,
    actor_user_id: int,
    entity_type: str,
    entity_id: int,
    action_type: str,
    detail: dict[str, Any] | None = None,
) -> ActivityLog:
    log = ActivityLog(
        household_id=household_id,
        actor_user_id=actor_user_id,
        entity_type=entity_type,
        entity_id=entity_id,
        action_type=action_type,
        detail_json=json.dumps(detail, ensure_ascii=False) if detail is not None else None,
    )
    db.add(log)
    return log
