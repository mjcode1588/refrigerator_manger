import hashlib
import secrets
from datetime import UTC, datetime, timedelta

import jwt

from app.core.config import settings

INVITE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"


def create_access_token(user_id: int) -> str:
    now = datetime.now(UTC)
    expire = now + timedelta(minutes=settings.jwt_access_expire_minutes)
    payload = {
        "sub": str(user_id),
        "iat": int(now.timestamp()),
        "exp": int(expire.timestamp()),
        "type": "access",
    }
    return jwt.encode(payload, settings.jwt_secret_key, algorithm=settings.jwt_algorithm)


def decode_access_token(token: str) -> int:
    payload = jwt.decode(token, settings.jwt_secret_key, algorithms=[settings.jwt_algorithm])
    if payload.get("type") != "access" or "sub" not in payload:
        raise jwt.InvalidTokenError("Invalid access token payload")
    return int(payload["sub"])


def generate_refresh_token() -> str:
    return secrets.token_urlsafe(48)


def hash_token(raw_token: str) -> str:
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


def generate_invite_code(length: int = 8) -> str:
    return "".join(secrets.choice(INVITE_ALPHABET) for _ in range(length))
