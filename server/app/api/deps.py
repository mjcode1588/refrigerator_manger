import jwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.errors import AppError
from app.core.security import decode_access_token
from app.db.models import HouseholdMember, User
from app.db.session import get_db

bearer_scheme = HTTPBearer(auto_error=False)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None or not credentials.credentials:
        raise AppError("UNAUTHORIZED", "인증 토큰이 필요합니다.", 401)
    try:
        user_id = decode_access_token(credentials.credentials)
    except jwt.ExpiredSignatureError as exc:
        raise AppError("TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다.", 401) from exc
    except jwt.InvalidTokenError as exc:
        raise AppError("INVALID_TOKEN", "유효하지 않은 액세스 토큰입니다.", 401) from exc

    user = db.get(User, user_id)
    if user is None:
        raise AppError("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", 401)
    return user


def get_household_membership(
    household_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> HouseholdMember:
    stmt = select(HouseholdMember).where(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == user.id,
    )
    member = db.execute(stmt).scalar_one_or_none()
    if member is None:
        raise AppError("FORBIDDEN", "해당 가구에 접근 권한이 없습니다.", 403)
    return member


def require_household_owner(
    member: HouseholdMember = Depends(get_household_membership),
) -> HouseholdMember:
    if member.role != "OWNER":
        raise AppError("FORBIDDEN", "가구 방장(OWNER) 권한이 필요합니다.", 403)
    return member
