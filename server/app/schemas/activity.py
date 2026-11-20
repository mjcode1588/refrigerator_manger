from datetime import datetime
from typing import Any

from pydantic import BaseModel


class ActivityLogResponse(BaseModel):
    id: int
    household_id: int
    actor_user_id: int
    actor_nickname: str
    entity_type: str
    entity_id: int
    action_type: str
    detail: dict[str, Any] | None = None
    created_at: datetime


class ActivityListResponse(BaseModel):
    items: list[ActivityLogResponse]
    next_cursor: int | None = None
