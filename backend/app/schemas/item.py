import uuid
from datetime import date, datetime

from pydantic import BaseModel


class ItemCandidate(BaseModel):
    name: str
    quantity: float | None = None
    unit: str | None = None
    expiry_date: date | None = None
    storage_location: str | None = None
    confidence: float | None = None
    source: str

    model_config = {"from_attributes": True}


class ItemIngestResponse(BaseModel):
    candidates: list[ItemCandidate]


class ItemCreate(BaseModel):
    name: str
    category: str | None = None
    quantity: float | None = None
    unit: str | None = None
    purchase_date: date | None = None
    expiry_date: date | None = None
    storage_location: str | None = None
    notes: str | None = None


class ItemConfirmRequest(BaseModel):
    fridge_id: uuid.UUID
    items: list[ItemCreate]


class ItemUpdate(BaseModel):
    name: str | None = None
    category: str | None = None
    quantity: float | None = None
    unit: str | None = None
    purchase_date: date | None = None
    expiry_date: date | None = None
    storage_location: str | None = None
    status: str | None = None
    notes: str | None = None


class ItemOut(BaseModel):
    id: uuid.UUID
    fridge_id: uuid.UUID
    name: str
    category: str | None
    quantity: float | None
    unit: str | None
    purchase_date: date | None
    expiry_date: date | None
    storage_location: str | None
    status: str
    notes: str | None
    created_at: datetime
    updated_at: datetime | None

    model_config = {"from_attributes": True}
