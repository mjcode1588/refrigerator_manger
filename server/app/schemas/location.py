from enum import StrEnum

from pydantic import BaseModel, Field, field_validator


def _blank_to_none(v: str | None) -> str | None:
    if v is None:
        return None
    stripped = v.strip()
    return stripped if stripped else None


class StorageType(StrEnum):
    FRIDGE = "FRIDGE"
    FREEZER = "FREEZER"
    PANTRY = "PANTRY"
    ROOM_TEMP = "ROOM_TEMP"
    CUSTOM = "CUSTOM"


class CreateLocationRequest(BaseModel):
    parent_id: int | None = None
    name: str = Field(min_length=1, max_length=100)
    storage_type: StorageType = StorageType.CUSTOM
    icon: str | None = Field(default=None, max_length=50)
    sort_order: int = Field(default=0, ge=0)

    @field_validator("icon", mode="before")
    @classmethod
    def normalize_icon(cls, v: str | None) -> str | None:
        return _blank_to_none(v)


class UpdateLocationRequest(BaseModel):
    parent_id: int | None = None
    change_parent: bool = False
    name: str | None = Field(default=None, min_length=1, max_length=100)
    storage_type: StorageType | None = None
    icon: str | None = Field(default=None, max_length=50)
    sort_order: int | None = Field(default=None, ge=0)

    @field_validator("icon", mode="before")
    @classmethod
    def normalize_icon(cls, v: str | None) -> str | None:
        return _blank_to_none(v)


class LocationNodeResponse(BaseModel):
    id: int
    household_id: int
    parent_id: int | None
    name: str
    storage_type: str
    icon: str | None
    sort_order: int
    depth: int
    full_path: str
    children: list["LocationNodeResponse"] = Field(default_factory=list)
