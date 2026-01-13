import uuid

from fastapi import APIRouter, Depends, HTTPException, status

from app.application.errors import ConflictError, ForbiddenError, NotFoundError, ValidationError
from app.application.use_cases.fridges import create_fridge, list_members
from app.application.use_cases.invites import create_invite_code, join_fridge_by_invite
from app.core.config import settings
from app.domain.entities import User
from app.domain.invite import generate_invite_code
from app.infrastructure.repositories.fridges import SqlFridgeRepository
from app.infrastructure.repositories.invites import SqlInviteRepository
from app.interfaces.api.deps import get_current_user, get_fridge_repo, get_invite_repo
from app.schemas.fridge import FridgeCreate, FridgeOut, InviteOut, InviteRequest, JoinRequest, MemberOut

router = APIRouter(prefix="/fridges", tags=["fridges"])


@router.post("", response_model=FridgeOut, status_code=status.HTTP_201_CREATED)
async def create_fridge_handler(
    payload: FridgeCreate,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
) -> FridgeOut:
    fridge = await create_fridge(fridge_repo=fridge_repo, name=payload.name, owner_user_id=current_user.id)
    return FridgeOut.model_validate(fridge)


@router.post("/invite", response_model=InviteOut, status_code=status.HTTP_201_CREATED)
async def create_invite_handler(
    payload: InviteRequest,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    invite_repo: SqlInviteRepository = Depends(get_invite_repo),
) -> InviteOut:
    expires_hours = payload.expires_hours or settings.invite_expires_hours
    max_uses = payload.max_uses or settings.invite_max_uses
    try:
        invite = await create_invite_code(
            fridge_repo=fridge_repo,
            invite_repo=invite_repo,
            fridge_id=payload.fridge_id,
            user_id=current_user.id,
            code=generate_invite_code(),
            expires_in_hours=expires_hours,
            max_uses=max_uses,
        )
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    return InviteOut(invite_code=invite.code, expires_at=invite.expires_at)


@router.post("/join", status_code=status.HTTP_200_OK)
async def join_fridge_handler(
    payload: JoinRequest,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    invite_repo: SqlInviteRepository = Depends(get_invite_repo),
) -> dict:
    try:
        fridge_id = await join_fridge_by_invite(
            fridge_repo=fridge_repo,
            invite_repo=invite_repo,
            invite_code=payload.invite_code,
            user_id=current_user.id,
        )
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ConflictError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc
    except ValidationError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return {"fridge_id": fridge_id, "role": "member"}


@router.get("/{fridge_id}/members", response_model=list[MemberOut])
async def list_members_handler(
    fridge_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
) -> list[MemberOut]:
    try:
        members = await list_members(fridge_repo=fridge_repo, fridge_id=fridge_id, user_id=current_user.id)
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    return [
        MemberOut(user_id=member.user_id, role=member.role, joined_at=member.created_at)
        for member in members
    ]
