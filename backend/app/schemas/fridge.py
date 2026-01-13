import uuid
from datetime import datetime

from pydantic import BaseModel


class FridgeCreate(BaseModel):
    name: str


class FridgeOut(BaseModel):
    id: uuid.UUID
    name: str
    owner_user_id: uuid.UUID
    created_at: datetime

    model_config = {"from_attributes": True}


class InviteOut(BaseModel):
    invite_code: str
    expires_at: datetime


class InviteRequest(BaseModel):
    fridge_id: uuid.UUID
    expires_hours: int | None = None
    max_uses: int | None = None


class JoinRequest(BaseModel):
    invite_code: str


class MemberOut(BaseModel):
    user_id: uuid.UUID
    role: str
    joined_at: datetime
