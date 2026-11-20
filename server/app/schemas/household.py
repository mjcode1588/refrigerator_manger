from datetime import datetime

from pydantic import BaseModel, Field


class CreateHouseholdRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    create_default_locations: bool = True


class UpdateHouseholdRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)


class HouseholdResponse(BaseModel):
    id: int
    name: str
    role: str
    created_at: datetime


class HouseholdMemberResponse(BaseModel):
    user_id: int
    nickname: str
    email: str | None = None
    role: str
    joined_at: datetime


class CreateInviteRequest(BaseModel):
    expires_in_hours: int = Field(default=24, ge=1, le=168)
    max_uses: int = Field(default=5, ge=1, le=50)


class InviteCodeResponse(BaseModel):
    code: str
    expires_at: datetime
    max_uses: int
    used_count: int


class JoinHouseholdRequest(BaseModel):
    code: str = Field(min_length=4, max_length=16)
