from datetime import datetime, timedelta, timezone
import uuid

from app.application.errors import ConflictError, ForbiddenError, NotFoundError, ValidationError
from app.application.ports import FridgeRepository, InviteRepository
from app.domain.entities import InviteCode


async def create_invite_code(
    *,
    fridge_repo: FridgeRepository,
    invite_repo: InviteRepository,
    fridge_id: uuid.UUID,
    user_id: uuid.UUID,
    code: str,
    expires_in_hours: int,
    max_uses: int,
) -> InviteCode:
    if not await fridge_repo.is_member(fridge_id, user_id):
        raise ForbiddenError("Not a fridge member")
    expires_at = datetime.now(tz=timezone.utc) + timedelta(hours=expires_in_hours)
    return await invite_repo.create(
        fridge_id=fridge_id,
        code=code,
        expires_at=expires_at,
        created_by=user_id,
        max_uses=max_uses,
    )


async def join_fridge_by_invite(
    *,
    fridge_repo: FridgeRepository,
    invite_repo: InviteRepository,
    invite_code: str,
    user_id: uuid.UUID,
) -> uuid.UUID:
    invite = await invite_repo.get_by_code(invite_code)
    if not invite:
        raise NotFoundError("Invite code not found")
    now = datetime.now(tz=timezone.utc)
    if invite.expires_at <= now:
        raise ValidationError("Invite code expired")
    if invite.used_count >= invite.max_uses:
        raise ValidationError("Invite code already used")
    if await fridge_repo.is_member(invite.fridge_id, user_id):
        raise ConflictError("Already a member")

    await fridge_repo.add_member(invite.fridge_id, user_id, role="member")
    await invite_repo.increment_used(invite.id)
    return invite.fridge_id
