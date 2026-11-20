from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.errors import AppError
from app.core.security import create_access_token, generate_refresh_token, hash_token
from app.db.models import Household, HouseholdMember, RefreshToken, SocialAccount, User
from app.db.session import get_db
from app.schemas.auth import (
    DevLoginRequest,
    LogoutRequest,
    RefreshTokenRequest,
    SocialLoginGoogleRequest,
    SocialLoginKakaoRequest,
    TokenResponse,
    UpdateUserMeRequest,
    UserHouseholdBrief,
    UserMeResponse,
    UserSummary,
)
from app.services.oauth import OAuthVerifier, SocialUserInfo, get_oauth_verifier

router = APIRouter(tags=["auth"])


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


def _upsert_social_user(db: Session, info: SocialUserInfo) -> User:
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


@router.post("/auth/social/google", response_model=TokenResponse)
async def login_with_google(
    body: SocialLoginGoogleRequest,
    db: Session = Depends(get_db),
    verifier: OAuthVerifier = Depends(get_oauth_verifier),
) -> TokenResponse:
    info = await verifier.verify_google(body.id_token)
    user = _upsert_social_user(db, info)
    return _issue_tokens(db, user)


@router.post("/auth/social/kakao", response_model=TokenResponse)
async def login_with_kakao(
    body: SocialLoginKakaoRequest,
    db: Session = Depends(get_db),
    verifier: OAuthVerifier = Depends(get_oauth_verifier),
) -> TokenResponse:
    info = await verifier.verify_kakao(body.access_token)
    user = _upsert_social_user(db, info)
    return _issue_tokens(db, user)


@router.post("/auth/dev-login", response_model=TokenResponse)
def dev_login(
    body: DevLoginRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    if settings.app_env != "local":
        raise AppError("NOT_FOUND", "로컬 개발 환경에서만 사용할 수 있습니다.", 404)
    info = SocialUserInfo(
        provider="DEV",
        provider_user_id=body.nickname.strip().lower(),
        nickname=body.nickname.strip(),
        email=body.email,
    )
    user = _upsert_social_user(db, info)
    return _issue_tokens(db, user)


@router.post("/auth/refresh", response_model=TokenResponse)
def refresh_tokens(
    body: RefreshTokenRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    token_hash = hash_token(body.refresh_token)
    stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash)
    stored = db.execute(stmt).scalar_one_or_none()
    now = datetime.now(UTC)
    if stored is None or stored.revoked_at is not None or _as_utc(stored.expires_at) < now:
        raise AppError("INVALID_REFRESH_TOKEN", "유효하지 않거나 만료된 리프레시 토큰입니다.", 401)

    user = db.get(User, stored.user_id)
    if user is None:
        raise AppError("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", 404)

    stored.revoked_at = now
    db.flush()
    return _issue_tokens(db, user)


@router.post("/auth/logout")
def logout(
    body: LogoutRequest,
    _user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    token_hash = hash_token(body.refresh_token)
    stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash)
    stored = db.execute(stmt).scalar_one_or_none()
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
