import uuid
from datetime import datetime

from pydantic import BaseModel, EmailStr


class UserCreate(BaseModel):
    email: EmailStr
    password: str
    name: str | None = None
    locale: str | None = None


class UserOut(BaseModel):
    id: uuid.UUID
    email: EmailStr
    name: str | None
    locale: str | None
    created_at: datetime

    model_config = {"from_attributes": True}
