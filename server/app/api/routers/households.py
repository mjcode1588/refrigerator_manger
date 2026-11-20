from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_household_membership, require_household_owner
from app.core.errors import AppError
from app.core.security import generate_invite_code
from app.db.models import (
    Household,
    HouseholdMember,
    InviteCode,
    StorageLocation,
    User,
)
from app.db.session import get_db
from app.schemas.household import (
    CreateHouseholdRequest,
    CreateInviteRequest,
    HouseholdMemberResponse,
    HouseholdResponse,
    InviteCodeResponse,
    JoinHouseholdRequest,
    UpdateHouseholdRequest,
)
from app.services.activity import record_activity

router = APIRouter(prefix="/households", tags=["households"])


def _as_utc(dt: datetime) -> datetime:
    return dt if dt.tzinfo is not None else dt.replace(tzinfo=UTC)


def _create_default_locations(db: Session, household_id: int) -> None:
    fridge = StorageLocation(
        household_id=household_id,
        parent_id=None,
        name="주방 냉장고",
        storage_type="FRIDGE",
        icon="🧊",
        sort_order=0,
    )
    pantry = StorageLocation(
        household_id=household_id,
        parent_id=None,
        name="팬트리",
        storage_type="PANTRY",
        icon="📦",
        sort_order=1,
    )
    db.add_all([fridge, pantry])
    db.flush()
    db.add_all(
        [
            StorageLocation(
                household_id=household_id,
                parent_id=fridge.id,
                name="냉장실",
                storage_type="FRIDGE",
                icon="🥗",
                sort_order=0,
            ),
            StorageLocation(
                household_id=household_id,
                parent_id=fridge.id,
                name="냉동실",
                storage_type="FREEZER",
                icon="❄️",
                sort_order=1,
            ),
        ]
    )


@router.post("", response_model=HouseholdResponse, status_code=status.HTTP_201_CREATED)
def create_household(
    body: CreateHouseholdRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> HouseholdResponse:
    household = Household(name=body.name.strip(), created_by=user.id)
    db.add(household)
    db.flush()

    member = HouseholdMember(household_id=household.id, user_id=user.id, role="OWNER")
    db.add(member)

    if body.create_default_locations:
        _create_default_locations(db, household.id)

    db.commit()
    db.refresh(household)
    return HouseholdResponse(
        id=household.id,
        name=household.name,
        role="OWNER",
        created_at=household.created_at,
    )


@router.get("", response_model=list[HouseholdResponse])
def list_my_households(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[HouseholdResponse]:
    stmt = (
        select(Household, HouseholdMember.role)
        .join(HouseholdMember, HouseholdMember.household_id == Household.id)
        .where(HouseholdMember.user_id == user.id)
        .order_by(Household.id)
    )
    rows = db.execute(stmt).all()
    return [
        HouseholdResponse(id=h.id, name=h.name, role=role, created_at=h.created_at)
        for h, role in rows
    ]


@router.patch("/{household_id}", response_model=HouseholdResponse)
def update_household(
    household_id: int,
    body: UpdateHouseholdRequest,
    member: HouseholdMember = Depends(require_household_owner),
    db: Session = Depends(get_db),
) -> HouseholdResponse:
    household = db.get(Household, household_id)
    if household is None:
        raise AppError("NOT_FOUND", "가구를 찾을 수 없습니다.", 404)
    household.name = body.name.strip()
    db.commit()
    db.refresh(household)
    return HouseholdResponse(
        id=household.id,
        name=household.name,
        role=member.role,
        created_at=household.created_at,
    )


@router.get("/{household_id}/members", response_model=list[HouseholdMemberResponse])
def list_household_members(
    household_id: int,
    _member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> list[HouseholdMemberResponse]:
    stmt = (
        select(HouseholdMember, User)
        .join(User, User.id == HouseholdMember.user_id)
        .where(HouseholdMember.household_id == household_id)
        .order_by(HouseholdMember.joined_at)
    )
    rows = db.execute(stmt).all()
    return [
        HouseholdMemberResponse(
            user_id=u.id,
            nickname=u.nickname,
            email=u.email,
            role=m.role,
            joined_at=m.joined_at,
        )
        for m, u in rows
    ]


@router.delete("/{household_id}/members/{target_user_id}")
def remove_or_leave_member(
    household_id: int,
    target_user_id: int,
    actor_member: HouseholdMember = Depends(get_household_membership),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    if actor_member.user_id != target_user_id and actor_member.role != "OWNER":
        raise AppError("FORBIDDEN", "다른 구성원을 내보낼 권한이 없습니다.", 403)

    target_stmt = select(HouseholdMember).where(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == target_user_id,
    )
    target = db.execute(target_stmt).scalar_one_or_none()
    if target is None:
        raise AppError("MEMBER_NOT_FOUND", "구성원을 찾을 수 없습니다.", 404)

    if target.role == "OWNER":
        other_owners = db.execute(
            select(HouseholdMember).where(
                HouseholdMember.household_id == household_id,
                HouseholdMember.role == "OWNER",
                HouseholdMember.user_id != target_user_id,
            )
        ).scalars().all()
        other_members = db.execute(
            select(HouseholdMember).where(
                HouseholdMember.household_id == household_id,
                HouseholdMember.user_id != target_user_id,
            )
        ).scalars().all()
        if not other_owners and other_members:
            raise AppError(
                "LAST_OWNER_CANNOT_LEAVE",
                "다른 방장이 없으면 가구를 떠날 수 없습니다.",
                400,
            )

    db.delete(target)
    record_activity(
        db,
        household_id=household_id,
        actor_user_id=actor_member.user_id,
        entity_type="MEMBER",
        entity_id=target_user_id,
        action_type="LEFT",
        detail={"target_user_id": target_user_id},
    )
    db.commit()
    return {"status": "ok"}


@router.post(
    "/{household_id}/invites",
    response_model=InviteCodeResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_invite(
    household_id: int,
    body: CreateInviteRequest,
    member: HouseholdMember = Depends(require_household_owner),
    db: Session = Depends(get_db),
) -> InviteCodeResponse:
    code = generate_invite_code(8)
    expires_at = datetime.now(UTC) + timedelta(hours=body.expires_in_hours)
    invite = InviteCode(
        household_id=household_id,
        code=code,
        created_by=member.user_id,
        expires_at=expires_at,
        max_uses=body.max_uses,
        used_count=0,
    )
    db.add(invite)
    db.commit()
    db.refresh(invite)
    return InviteCodeResponse(
        code=invite.code,
        expires_at=invite.expires_at,
        max_uses=invite.max_uses,
        used_count=invite.used_count,
    )


@router.post("/join", response_model=HouseholdResponse)
def join_household(
    body: JoinHouseholdRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> HouseholdResponse:
    normalized = body.code.strip().upper()
    invite = db.execute(
        select(InviteCode).where(InviteCode.code == normalized)
    ).scalar_one_or_none()
    if invite is None:
        raise AppError("INVALID_INVITE_CODE", "존재하지 않는 초대 코드입니다.", 404)

    now = datetime.now(UTC)
    if _as_utc(invite.expires_at) < now:
        raise AppError("INVITE_CODE_EXPIRED", "만료된 초대 코드입니다.", 400)
    if invite.used_count >= invite.max_uses:
        raise AppError("INVITE_CODE_EXHAUSTED", "사용 횟수를 초과한 초대 코드입니다.", 400)

    existing = db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == invite.household_id,
            HouseholdMember.user_id == user.id,
        )
    ).scalar_one_or_none()
    if existing is not None:
        raise AppError("ALREADY_HOUSEHOLD_MEMBER", "이미 참여 중인 가구입니다.", 409)

    invite.used_count += 1
    db.add(
        HouseholdMember(
            household_id=invite.household_id,
            user_id=user.id,
            role="MEMBER",
        )
    )
    record_activity(
        db,
        household_id=invite.household_id,
        actor_user_id=user.id,
        entity_type="MEMBER",
        entity_id=user.id,
        action_type="JOINED",
        detail={"nickname": user.nickname},
    )
    db.commit()

    household = db.get(Household, invite.household_id)
    assert household is not None
    return HouseholdResponse(
        id=household.id,
        name=household.name,
        role="MEMBER",
        created_at=household.created_at,
    )
