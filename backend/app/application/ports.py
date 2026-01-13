from __future__ import annotations

import uuid
from datetime import datetime
from typing import Iterable, Protocol

from app.domain.entities import (
    FileData,
    Fridge,
    FridgeItem,
    FridgeMember,
    InviteCode,
    ItemCandidate,
    Notification,
    RecipeSuggestion,
    User,
)


class UserRepository(Protocol):
    async def get_by_email(self, email: str) -> User | None: ...

    async def get_by_id(self, user_id: uuid.UUID) -> User | None: ...

    async def create(self, email: str, hashed_password: str, name: str | None, locale: str | None) -> User: ...


class FridgeRepository(Protocol):
    async def get_by_id(self, fridge_id: uuid.UUID) -> Fridge | None: ...

    async def create(self, name: str, owner_user_id: uuid.UUID) -> Fridge: ...

    async def list_members(self, fridge_id: uuid.UUID) -> list[FridgeMember]: ...

    async def add_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID, role: str) -> None: ...

    async def is_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID) -> bool: ...


class InviteRepository(Protocol):
    async def create(
        self,
        fridge_id: uuid.UUID,
        code: str,
        expires_at: datetime,
        created_by: uuid.UUID,
        max_uses: int,
    ) -> InviteCode: ...

    async def get_by_code(self, code: str) -> InviteCode | None: ...

    async def increment_used(self, invite_id: uuid.UUID) -> None: ...


class ItemRepository(Protocol):
    async def create_items(self, fridge_id: uuid.UUID, items: list[dict]) -> list[FridgeItem]: ...

    async def list_items(self, fridge_id: uuid.UUID) -> list[FridgeItem]: ...

    async def get_by_id(self, item_id: uuid.UUID) -> FridgeItem | None: ...

    async def update_item(self, item_id: uuid.UUID, updates: dict) -> FridgeItem | None: ...

    async def delete_item(self, item_id: uuid.UUID) -> bool: ...

    async def list_expiring(self, fridge_id: uuid.UUID, days: int) -> list[FridgeItem]: ...

    async def list_expiring_all(self, days: int) -> list[FridgeItem]: ...


class NotificationRepository(Protocol):
    async def exists(self, user_id: uuid.UUID, item_id: uuid.UUID | None, notif_type: str) -> bool: ...

    async def create(self, user_id: uuid.UUID, fridge_id: uuid.UUID, item_id: uuid.UUID | None, notif_type: str) -> Notification: ...


class ImageStorage(Protocol):
    async def save(self, file: FileData) -> str: ...


class LLMClient(Protocol):
    def extract_candidates_from_text(self, text: str) -> list[ItemCandidate]: ...

    def extract_candidates_from_images(self, image_names: Iterable[str]) -> list[ItemCandidate]: ...

    def suggest_recipes(self, items: list[str], prefer_expiring_first: bool) -> list[RecipeSuggestion]: ...


class PasswordHasher(Protocol):
    def hash(self, password: str) -> str: ...

    def verify(self, password: str, hashed_password: str) -> bool: ...


class TokenIssuer(Protocol):
    def create_access_token(self, subject: str) -> str: ...
