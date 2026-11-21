from datetime import date, datetime
from decimal import Decimal
from enum import StrEnum
from zoneinfo import ZoneInfo

from pydantic import BaseModel, Field, field_validator, model_validator

KST = ZoneInfo("Asia/Seoul")


def _blank_to_none(v: str | None) -> str | None:
    if v is None:
        return None
    stripped = v.strip()
    return stripped if stripped else None


class ItemCategory(StrEnum):
    FRESH = "FRESH"
    MEAT_FISH = "MEAT_FISH"
    DAIRY = "DAIRY"
    PROCESSED = "PROCESSED"
    BEVERAGE = "BEVERAGE"
    SAUCE = "SAUCE"
    ETC = "ETC"


class ExpiryType(StrEnum):
    USE_BY = "USE_BY"
    BEST_BEFORE = "BEST_BEFORE"
    NONE = "NONE"


class ItemStatus(StrEnum):
    ACTIVE = "ACTIVE"
    CONSUMED = "CONSUMED"
    DISCARDED = "DISCARDED"


class ExpiryState(StrEnum):
    EXPIRED = "EXPIRED"
    TODAY = "TODAY"
    EXPIRING_SOON = "EXPIRING_SOON"
    SAFE = "SAFE"
    NONE = "NONE"


def compute_expiry_meta(
    expiry_date: date | None,
    *,
    alert_days: int = 3,
    today: date | None = None,
) -> tuple[int | None, ExpiryState]:
    if expiry_date is None:
        return None, ExpiryState.NONE
    ref_today = today or datetime.now(KST).date()
    d_day = (expiry_date - ref_today).days
    if d_day < 0:
        return d_day, ExpiryState.EXPIRED
    if d_day == 0:
        return 0, ExpiryState.TODAY
    if d_day <= alert_days:
        return d_day, ExpiryState.EXPIRING_SOON
    return d_day, ExpiryState.SAFE


class CreateItemRequest(BaseModel):
    location_id: int
    name: str = Field(min_length=1, max_length=150)
    category: ItemCategory = ItemCategory.ETC
    quantity: Decimal = Field(default=Decimal("1"), gt=0)
    unit: str = Field(default="개", min_length=1, max_length=30)
    expiry_date: date | None = None
    expiry_type: ExpiryType = ExpiryType.USE_BY
    memo: str | None = Field(default=None, max_length=500)

    @field_validator("memo", mode="before")
    @classmethod
    def normalize_memo(cls, v: str | None) -> str | None:
        return _blank_to_none(v)


class UpdateItemRequest(BaseModel):
    location_id: int | None = None
    name: str | None = Field(default=None, min_length=1, max_length=150)
    category: ItemCategory | None = None
    quantity: Decimal | None = Field(default=None, gt=0)
    unit: str | None = Field(default=None, min_length=1, max_length=30)
    expiry_date: date | None = None
    clear_expiry_date: bool = False
    expiry_type: ExpiryType | None = None
    memo: str | None = Field(default=None, max_length=500)

    @field_validator("memo", mode="before")
    @classmethod
    def normalize_memo(cls, v: str | None) -> str | None:
        return _blank_to_none(v)


class ConsumeItemRequest(BaseModel):
    amount: Decimal | None = Field(default=None, gt=0)
    remaining_quantity: Decimal | None = Field(default=None, ge=0)
    note: str | None = Field(default=None, max_length=200)

    @field_validator("note")
    @classmethod
    def normalize_note(cls, v: str | None) -> str | None:
        return _blank_to_none(v)

    @model_validator(mode="after")
    def validate_amount_or_remaining(self) -> "ConsumeItemRequest":
        if self.amount is not None and self.remaining_quantity is not None:
            raise ValueError("Provide either amount or remaining_quantity, not both")
        return self


class ItemResponse(BaseModel):
    id: int
    household_id: int
    location_id: int
    location_path: str
    name: str
    category: str
    quantity: float
    unit: str
    expiry_date: date | None
    expiry_type: str
    d_day: int | None
    expiry_state: ExpiryState
    status: str
    memo: str | None
    updated_at: datetime
