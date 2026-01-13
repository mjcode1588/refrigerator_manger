import uuid
from datetime import datetime

from pydantic import BaseModel


class NotificationOut(BaseModel):
    id: uuid.UUID
    user_id: uuid.UUID
    fridge_id: uuid.UUID
    item_id: uuid.UUID | None
    type: str
    status: str
    created_at: datetime

    model_config = {"from_attributes": True}
