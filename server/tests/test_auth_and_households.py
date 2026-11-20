from fastapi.testclient import TestClient

from app.core.errors import AppError
from app.services.oauth import SocialUserInfo, get_oauth_verifier


class FakeOAuthVerifier:
    async def verify_google(self, id_token: str) -> SocialUserInfo:
        if id_token == "valid-google-token":
            return SocialUserInfo(
                provider="GOOGLE",
                provider_user_id="g-12345",
                nickname="구글민수",
                email="minsu@gmail.com",
            )
        raise AppError("INVALID_SOCIAL_TOKEN", "Invalid Google token", 401)

    async def verify_kakao(self, access_token: str) -> SocialUserInfo:
        if access_token == "valid-kakao-token":
            return SocialUserInfo(
                provider="KAKAO",
                provider_user_id="k-98765",
                nickname="카카오지영",
                email="jiyoung@kakao.com",
            )
        raise AppError("INVALID_SOCIAL_TOKEN", "Invalid Kakao token", 401)


def _auth_header(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_social_login_and_refresh_rotation(client: TestClient) -> None:
    client.app.dependency_overrides[get_oauth_verifier] = lambda: FakeOAuthVerifier()

    bad = client.post("/api/v1/auth/social/google", json={"id_token": "bad"})
    assert bad.status_code == 401

    ok = client.post("/api/v1/auth/social/google", json={"id_token": "valid-google-token"})
    assert ok.status_code == 200
    data = ok.json()
    assert data["user"]["nickname"] == "구글민수"
    refresh1 = data["refresh_token"]

    # Refresh token rotation
    refreshed = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh1})
    assert refreshed.status_code == 200
    refresh2 = refreshed.json()["refresh_token"]
    assert refresh2 != refresh1

    # Reusing rotated refresh token must fail
    reused = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh1})
    assert reused.status_code == 401

    me = client.get("/api/v1/users/me", headers=_auth_header(refreshed.json()["access_token"]))
    assert me.status_code == 200
    assert me.json()["nickname"] == "구글민수"


def test_household_invite_and_permissions(client: TestClient) -> None:
    client.app.dependency_overrides[get_oauth_verifier] = lambda: FakeOAuthVerifier()

    owner_tokens = client.post(
        "/api/v1/auth/social/google", json={"id_token": "valid-google-token"}
    ).json()
    member_tokens = client.post(
        "/api/v1/auth/social/kakao", json={"access_token": "valid-kakao-token"}
    ).json()

    owner_h = _auth_header(owner_tokens["access_token"])
    member_h = _auth_header(member_tokens["access_token"])

    # Owner creates household with default locations
    hh_resp = client.post(
        "/api/v1/households",
        json={"name": "우리집", "create_default_locations": True},
        headers=owner_h,
    )
    assert hh_resp.status_code == 201
    hh_id = hh_resp.json()["id"]

    # Non-member cannot access household locations
    forbidden = client.get(f"/api/v1/households/{hh_id}/locations", headers=member_h)
    assert forbidden.status_code == 403

    # Owner creates invite code
    inv_resp = client.post(
        f"/api/v1/households/{hh_id}/invites",
        json={"expires_in_hours": 24, "max_uses": 2},
        headers=owner_h,
    )
    assert inv_resp.status_code == 201
    code = inv_resp.json()["code"]

    # Member joins via invite code
    join_resp = client.post("/api/v1/households/join", json={"code": code}, headers=member_h)
    assert join_resp.status_code == 200
    assert join_resp.json()["role"] == "MEMBER"

    # Member cannot issue invite code (OWNER only)
    member_inv = client.post(
        f"/api/v1/households/{hh_id}/invites",
        json={"expires_in_hours": 24, "max_uses": 2},
        headers=member_h,
    )
    assert member_inv.status_code == 403

    # Activity log recorded JOINED
    acts = client.get(f"/api/v1/households/{hh_id}/activities", headers=owner_h)
    assert acts.status_code == 200
    assert any(a["action_type"] == "JOINED" for a in acts.json()["items"])
