from dataclasses import dataclass
from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.errors import AppError
from app.core.rate_limit import limit_auth
from app.core.security import (
    DUMMY_PASSWORD_HASH,
    constant_time_equals,
    create_access_token,
    generate_refresh_token,
    hash_password,
    hash_token,
    verify_password,
)
from app.db.models import Household, HouseholdMember, RefreshToken, SocialAccount, User
from app.db.session import get_db
from app.schemas.auth import (
    DevLoginRequest,
    LoginRequest,
    LogoutRequest,
    RefreshTokenRequest,
    RegisterRequest,
    TokenResponse,
    UpdateUserMeRequest,
    UserHouseholdBrief,
    UserMeResponse,
    UserSummary,
)

router = APIRouter(tags=["auth"])
auth_limit = [Depends(limit_auth)]


@dataclass(frozen=True)
class _DevUserInfo:
    provider: str
    provider_user_id: str
    nickname: str
    email: str | None = None


def _as_utc(dt: datetime) -> datetime:
    return dt if dt.tzinfo is not None else dt.replace(tzinfo=UTC)


def _issue_tokens(db: Session, user: User) -> TokenResponse:
    access_token = create_access_token(user.id)
    raw_refresh = generate_refresh_token()
    expires_at = datetime.now(UTC) + timedelta(days=settings.jwt_refresh_expire_days)
    db.add(
        RefreshToken(
            user_id=user.id,
            token_hash=hash_token(raw_refresh),
            expires_at=expires_at,
        )
    )
    db.commit()
    return TokenResponse(
        access_token=access_token,
        refresh_token=raw_refresh,
        expires_in=settings.jwt_access_expire_minutes * 60,
        user=UserSummary(id=user.id, nickname=user.nickname, email=user.email),
    )


def _upsert_dev_user(db: Session, info: _DevUserInfo) -> User:
    stmt = select(SocialAccount).where(
        SocialAccount.provider == info.provider,
        SocialAccount.provider_user_id == info.provider_user_id,
    )
    social = db.execute(stmt).scalar_one_or_none()
    if social is not None:
        user = db.get(User, social.user_id)
        if user is None:  # pragma: no cover
            raise AppError("USER_NOT_FOUND", "연결된 사용자를 찾을 수 없습니다.", 404)
        return user

    user = User(nickname=info.nickname, email=info.email)
    db.add(user)
    db.flush()
    db.add(
        SocialAccount(
            user_id=user.id,
            provider=info.provider,
            provider_user_id=info.provider_user_id,
        )
    )
    db.flush()
    return user


@router.post(
    "/auth/register",
    status_code=201,
    response_model=TokenResponse,
    dependencies=auth_limit,
)
def register(
    body: RegisterRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    expected_code = settings.family_registration_code.strip()
    if not expected_code:
        raise AppError(
            "REGISTRATION_DISABLED",
            "현재 신규 가족 등록이 비활성화되어 있습니다.",
            403,
        )
    if not constant_time_equals(body.registration_code.strip(), expected_code):
        raise AppError(
            "INVALID_REGISTRATION_CODE",
            "가족 등록코드가 올바르지 않습니다.",
            403,
        )

    registered_count = db.execute(
        select(func.count(User.id)).where(User.username.is_not(None))
    ).scalar_one()
    if registered_count >= settings.max_registered_users:
        raise AppError(
            "REGISTRATION_CLOSED",
            "가족 구성원 등록이 마감되었습니다.",
            403,
        )

    username = body.username.strip().lower()
    nickname = body.nickname.strip()
    if not nickname:
        raise AppError("INVALID_NICKNAME", "닉네임을 입력해 주세요.", 400)
    if not body.password.strip():
        raise AppError("INVALID_PASSWORD", "공백만 포함된 비밀번호는 사용할 수 없습니다.", 400)

    existing = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if existing is not None:
        raise AppError("USERNAME_ALREADY_EXISTS", "이미 사용 중인 아이디입니다.", 409)

    user = User(
        username=username,
        password_hash=hash_password(body.password),
        nickname=nickname,
    )
    db.add(user)
    db.flush()
    return _issue_tokens(db, user)


@router.post("/auth/login", response_model=TokenResponse, dependencies=auth_limit)
def login(
    body: LoginRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    username = body.username.strip().lower()
    user = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if user is None or user.password_hash is None:
        verify_password(body.password, DUMMY_PASSWORD_HASH)
        raise AppError("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.", 401)

    now = datetime.now(UTC)
    if user.locked_until is not None and _as_utc(user.locked_until) > now:
        raise AppError(
            "ACCOUNT_LOCKED",
            "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.",
            429,
        )

    if not verify_password(body.password, user.password_hash):
        user.failed_login_attempts += 1
        if user.failed_login_attempts >= settings.auth_max_failed_attempts:
            user.locked_until = now + timedelta(minutes=settings.auth_lockout_minutes)
        db.commit()
        raise AppError("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.", 401)

    user.failed_login_attempts = 0
    user.locked_until = None
    db.flush()
    return _issue_tokens(db, user)


@router.post("/auth/dev-login", response_model=TokenResponse, dependencies=auth_limit)
def dev_login(
    body: DevLoginRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    if not settings.allow_dev_login:
        raise AppError("NOT_FOUND", "체험 로그인이 비활성화되어 있습니다.", 404)
    info = _DevUserInfo(
        provider="DEV",
        provider_user_id=body.nickname.strip().lower(),
        nickname=body.nickname.strip(),
        email=body.email,
    )
    user = _upsert_dev_user(db, info)
    return _issue_tokens(db, user)


@router.post("/auth/refresh", response_model=TokenResponse, dependencies=auth_limit)
def refresh_tokens(
    body: RefreshTokenRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    token_hash = hash_token(body.refresh_token)
    stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash)
    stored = db.execute(stmt).scalar_one_or_none()
    now = datetime.now(UTC)
    if stored is not None and stored.revoked_at is not None:
        # 폐기된 토큰 재사용 = 탈취 의심: 해당 사용자의 모든 활성 토큰 폐기
        active = db.execute(
            select(RefreshToken).where(
                RefreshToken.user_id == stored.user_id,
                RefreshToken.revoked_at.is_(None),
            )
        ).scalars()
        for tok in active:
            tok.revoked_at = now
        db.commit()
        raise AppError("INVALID_REFRESH_TOKEN", "유효하지 않거나 만료된 리프레시 토큰입니다.", 401)
    if stored is None or _as_utc(stored.expires_at) < now:
        raise AppError("INVALID_REFRESH_TOKEN", "유효하지 않거나 만료된 리프레시 토큰입니다.", 401)

    user = db.get(User, stored.user_id)
    if user is None:
        raise AppError("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", 404)

    stored.revoked_at = now
    db.flush()
    return _issue_tokens(db, user)


@router.post("/auth/logout", dependencies=auth_limit)
def logout(
    body: LogoutRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    token_hash = hash_token(body.refresh_token)
    stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash)
    stored = db.execute(stmt).scalar_one_or_none()
    if stored is not None and stored.user_id != user.id:
        raise AppError("FORBIDDEN", "본인의 리프레시 토큰이 아닙니다.", 403)
    if stored is not None and stored.revoked_at is None:
        stored.revoked_at = datetime.now(UTC)
        db.commit()
    return {"status": "ok"}


@router.get("/users/me", response_model=UserMeResponse)
def get_me(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> UserMeResponse:
    stmt = (
        select(Household.id, Household.name, HouseholdMember.role)
        .join(HouseholdMember, HouseholdMember.household_id == Household.id)
        .where(HouseholdMember.user_id == user.id)
        .order_by(Household.id)
    )
    rows = db.execute(stmt).all()
    return UserMeResponse(
        id=user.id,
        nickname=user.nickname,
        email=user.email,
        households=[UserHouseholdBrief(id=r.id, name=r.name, role=r.role) for r in rows],
    )


@router.patch("/users/me", response_model=UserSummary)
def update_me(
    body: UpdateUserMeRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> UserSummary:
    user.nickname = body.nickname.strip()
    db.commit()
    db.refresh(user)
    return UserSummary(id=user.id, nickname=user.nickname, email=user.email)
