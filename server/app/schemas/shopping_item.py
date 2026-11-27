from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field, field_validator

from app.schemas.item import ItemCategory, _blank_to_none


class CreateShoppingItemRequest(BaseModel):
    name: str = Field(min_length=1, max_length=150)
    category: ItemCategory = ItemCategory.ETC
    quantity: Decimal = Field(default=Decimal("1"), gt=0)
    unit: str = Field(default="개", min_length=1, max_length=30)
    memo: str | None = Field(default=None, max_length=500)

    @field_validator("name", "unit", mode="before")
    @classmethod
    def strip_required_text(cls, v: str) -> str:
        return v.strip() if isinstance(v, str) else v

    @field_validator("memo", mode="before")
    @classmethod
    def normalize_memo(cls, v: str | None) -> str | None:
        return _blank_to_none(v)


class UpdateShoppingItemRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=150)
    category: ItemCategory | None = None
    quantity: Decimal | None = Field(default=None, gt=0)
    unit: str | None = Field(default=None, min_length=1, max_length=30)
    memo: str | None = Field(default=None, max_length=500)
    clear_memo: bool = False
    is_purchased: bool | None = None

    @field_validator("name", "unit", mode="before")
    @classmethod
    def strip_optional_text(cls, v: str | None) -> str | None:
        return v.strip() if isinstance(v, str) else v

    @field_validator("memo", mode="before")
    @classmethod
    def normalize_memo(cls, v: str | None) -> str | None:
        return _blank_to_none(v)


class ShoppingItemResponse(BaseModel):
    id: int
    household_id: int
    name: str
    category: ItemCategory
    quantity: float
    unit: str
    memo: str | None
    is_purchased: bool
    purchased_by: int | None
    purchased_by_nickname: str | None
    purchased_at: datetime | None
    created_by: int
    created_at: datetime
    updated_at: datetime


class ClearPurchasedResponse(BaseModel):
    deleted_count: int
