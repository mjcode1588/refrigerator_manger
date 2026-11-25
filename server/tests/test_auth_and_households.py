from fastapi.testclient import TestClient

from app.core.config import settings as global_settings


def _auth_header(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_register_login_and_refresh_rotation(client: TestClient, monkeypatch) -> None:
    monkeypatch.setattr(global_settings, "family_registration_code", "our-home-secret-2026")

    reg = client.post(
        "/api/v1/auth/register",
        json={
            "username": "Husband_01",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "our-home-secret-2026",
        },
    )
    assert reg.status_code == 201, reg.text
    reg_data = reg.json()
    assert reg_data["user"]["nickname"] == "남편"

    # 대소문자 구분 없이 로그인 가능
    bad = client.post(
        "/api/v1/auth/login",
        json={"username": "husband_01", "password": "WrongPassword!"},
    )
    assert bad.status_code == 401
    assert bad.json()["error"]["code"] == "INVALID_CREDENTIALS"

    ok = client.post(
        "/api/v1/auth/login",
        json={"username": "husband_01", "password": "StrongPassword!1"},
    )
    assert ok.status_code == 200, ok.text
    refresh1 = ok.json()["refresh_token"]

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
    assert me.json()["nickname"] == "남편"


def test_household_invite_and_permissions(client: TestClient, monkeypatch) -> None:
    monkeypatch.setattr(global_settings, "family_registration_code", "our-home-secret-2026")

    owner_tokens = client.post(
        "/api/v1/auth/register",
        json={
            "username": "husband",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "our-home-secret-2026",
        },
    ).json()
    member_tokens = client.post(
        "/api/v1/auth/register",
        json={
            "username": "wife",
            "password": "StrongPassword!2",
            "nickname": "아내",
            "registration_code": "our-home-secret-2026",
        },
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
