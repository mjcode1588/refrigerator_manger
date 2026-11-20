from dataclasses import dataclass
from typing import Protocol

import httpx

from app.core.config import settings
from app.core.errors import AppError


@dataclass(frozen=True)
class SocialUserInfo:
    provider: str
    provider_user_id: str
    nickname: str
    email: str | None = None


class OAuthVerifier(Protocol):
    async def verify_google(self, id_token: str) -> SocialUserInfo: ...
    async def verify_kakao(self, access_token: str) -> SocialUserInfo: ...


class HttpOAuthVerifier:
    def __init__(self, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self._transport = transport

    async def verify_google(self, id_token: str) -> SocialUserInfo:
        async with httpx.AsyncClient(transport=self._transport, timeout=10.0) as client:
            resp = await client.get(
                "https://oauth2.googleapis.com/tokeninfo",
                params={"id_token": id_token},
            )
        if resp.status_code != 200:
            raise AppError("INVALID_SOCIAL_TOKEN", "Google 토큰 검증에 실패했습니다.", 401)
        data = resp.json()
        if settings.google_client_id and data.get("aud") != settings.google_client_id:
            raise AppError("INVALID_SOCIAL_TOKEN", "Google Client ID가 일치하지 않습니다.", 401)
        sub = data.get("sub")
        if not sub:
            raise AppError("INVALID_SOCIAL_TOKEN", "Google 사용자 식별자가 없습니다.", 401)
        nickname = data.get("name") or data.get("email", "Google 사용자").split("@")[0]
        return SocialUserInfo(
            provider="GOOGLE",
            provider_user_id=str(sub),
            nickname=nickname[:100],
            email=data.get("email"),
        )

    async def verify_kakao(self, access_token: str) -> SocialUserInfo:
        async with httpx.AsyncClient(transport=self._transport, timeout=10.0) as client:
            resp = await client.get(
                "https://kapi.kakao.com/v2/user/me",
                headers={"Authorization": f"Bearer {access_token}"},
            )
        if resp.status_code != 200:
            raise AppError("INVALID_SOCIAL_TOKEN", "Kakao 토큰 검증에 실패했습니다.", 401)
        data = resp.json()
        kakao_id = data.get("id")
        if not kakao_id:
            raise AppError("INVALID_SOCIAL_TOKEN", "Kakao 사용자 식별자가 없습니다.", 401)
        kakao_account = data.get("kakao_account") or {}
        profile = kakao_account.get("profile") or {}
        nickname = profile.get("nickname") or f"카카오_{str(kakao_id)[-4:]}"
        return SocialUserInfo(
            provider="KAKAO",
            provider_user_id=str(kakao_id),
            nickname=nickname[:100],
            email=kakao_account.get("email"),
        )


def get_oauth_verifier() -> OAuthVerifier:
    return HttpOAuthVerifier()
