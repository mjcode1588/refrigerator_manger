import uuid

from app.application.errors import ForbiddenError, NotFoundError
from app.application.ports import FridgeRepository
from app.domain.entities import Fridge


async def create_fridge(*, fridge_repo: FridgeRepository, name: str, owner_user_id: uuid.UUID) -> Fridge:
    return await fridge_repo.create(name=name, owner_user_id=owner_user_id)


async def list_members(*, fridge_repo: FridgeRepository, fridge_id: uuid.UUID, user_id: uuid.UUID):
    if not await fridge_repo.is_member(fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    members = await fridge_repo.list_members(fridge_id)
    if members is None:
        raise NotFoundError("Fridge not found")
    return members
