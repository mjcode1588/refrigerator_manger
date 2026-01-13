from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
import uuid

import pytest

from app.application.errors import ConflictError, NotFoundError, ValidationError
from app.application.use_cases.invites import join_fridge_by_invite
from app.domain.entities import InviteCode


@dataclass
class FakeFridgeRepo:
    members: set[tuple[uuid.UUID, uuid.UUID]]

    async def is_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID) -> bool:
        return (fridge_id, user_id) in self.members

    async def add_member(self, fridge_id: uuid.UUID, user_id: uuid.UUID, role: str) -> None:
        self.members.add((fridge_id, user_id))


@dataclass
class FakeInviteRepo:
    invite: InviteCode | None

    async def get_by_code(self, code: str) -> InviteCode | None:
        return self.invite if self.invite and self.invite.code == code else None

    async def increment_used(self, invite_id: uuid.UUID) -> None:
        if self.invite and self.invite.id == invite_id:
            self.invite.used_count += 1


@pytest.mark.asyncio
async def test_join_fridge_invalid_code():
    fridge_repo = FakeFridgeRepo(members=set())
    invite_repo = FakeInviteRepo(invite=None)

    with pytest.raises(NotFoundError):
        await join_fridge_by_invite(
            fridge_repo=fridge_repo,
            invite_repo=invite_repo,
            invite_code="missing",
            user_id=uuid.uuid4(),
        )


@pytest.mark.asyncio
async def test_join_fridge_expired():
    fridge_id = uuid.uuid4()
    invite = InviteCode(
        id=uuid.uuid4(),
        fridge_id=fridge_id,
        code="abc",
        expires_at=datetime.now(tz=timezone.utc) - timedelta(hours=1),
        created_by=uuid.uuid4(),
        used_count=0,
        max_uses=1,
    )
    fridge_repo = FakeFridgeRepo(members=set())
    invite_repo = FakeInviteRepo(invite=invite)

    with pytest.raises(ValidationError):
        await join_fridge_by_invite(
            fridge_repo=fridge_repo,
            invite_repo=invite_repo,
            invite_code="abc",
            user_id=uuid.uuid4(),
        )


@pytest.mark.asyncio
async def test_join_fridge_used_up():
    fridge_id = uuid.uuid4()
    invite = InviteCode(
        id=uuid.uuid4(),
        fridge_id=fridge_id,
        code="abc",
        expires_at=datetime.now(tz=timezone.utc) + timedelta(hours=1),
        created_by=uuid.uuid4(),
        used_count=1,
        max_uses=1,
    )
    fridge_repo = FakeFridgeRepo(members=set())
    invite_repo = FakeInviteRepo(invite=invite)

    with pytest.raises(ValidationError):
        await join_fridge_by_invite(
            fridge_repo=fridge_repo,
            invite_repo=invite_repo,
            invite_code="abc",
            user_id=uuid.uuid4(),
        )


@pytest.mark.asyncio
async def test_join_fridge_existing_member():
    fridge_id = uuid.uuid4()
    user_id = uuid.uuid4()
    invite = InviteCode(
        id=uuid.uuid4(),
        fridge_id=fridge_id,
        code="abc",
        expires_at=datetime.now(tz=timezone.utc) + timedelta(hours=1),
        created_by=uuid.uuid4(),
        used_count=0,
        max_uses=1,
    )
    fridge_repo = FakeFridgeRepo(members={(fridge_id, user_id)})
    invite_repo = FakeInviteRepo(invite=invite)

    with pytest.raises(ConflictError):
        await join_fridge_by_invite(
            fridge_repo=fridge_repo,
            invite_repo=invite_repo,
            invite_code="abc",
            user_id=user_id,
        )


@pytest.mark.asyncio
async def test_join_fridge_success():
    fridge_id = uuid.uuid4()
    user_id = uuid.uuid4()
    invite = InviteCode(
        id=uuid.uuid4(),
        fridge_id=fridge_id,
        code="abc",
        expires_at=datetime.now(tz=timezone.utc) + timedelta(hours=1),
        created_by=uuid.uuid4(),
        used_count=0,
        max_uses=2,
    )
    fridge_repo = FakeFridgeRepo(members=set())
    invite_repo = FakeInviteRepo(invite=invite)

    returned = await join_fridge_by_invite(
        fridge_repo=fridge_repo,
        invite_repo=invite_repo,
        invite_code="abc",
        user_id=user_id,
    )

    assert returned == fridge_id
    assert (fridge_id, user_id) in fridge_repo.members
    assert invite.used_count == 1
