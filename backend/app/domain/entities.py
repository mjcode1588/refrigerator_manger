from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
import uuid


@dataclass
class User:
    id: uuid.UUID
    email: str
    hashed_password: str
    name: str | None = None
    locale: str | None = None
    created_at: datetime | None = None


@dataclass
class Fridge:
    id: uuid.UUID
    name: str
    owner_user_id: uuid.UUID
    created_at: datetime | None = None


@dataclass
class FridgeMember:
    id: uuid.UUID
    fridge_id: uuid.UUID
    user_id: uuid.UUID
    role: str
    created_at: datetime | None = None


@dataclass
class InviteCode:
    id: uuid.UUID
    fridge_id: uuid.UUID
    code: str
    expires_at: datetime
    created_by: uuid.UUID
    used_count: int
    max_uses: int
    created_at: datetime | None = None


@dataclass
class FridgeItem:
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
    created_at: datetime | None = None
    updated_at: datetime | None = None


@dataclass
class ItemCandidate:
    name: str
    quantity: float | None = None
    unit: str | None = None
    expiry_date: date | None = None
    storage_location: str | None = None
    confidence: float | None = None
    source: str = "text"


@dataclass
class RecipeSuggestion:
    title: str
    steps: list[str]
    use_items: list[str]
    missing_items: list[str]


@dataclass
class Notification:
    id: uuid.UUID
    user_id: uuid.UUID
    fridge_id: uuid.UUID
    item_id: uuid.UUID | None
    type: str
    status: str
    created_at: datetime | None = None


@dataclass
class FileData:
    filename: str
    content: bytes
