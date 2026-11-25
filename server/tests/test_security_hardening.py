import pytest
from fastapi.testclient import TestClient

from app.core.config import Settings
from app.core.config import settings as global_settings


def _login(client: TestClient, nickname: str) -> dict:
    res = client.post("/api/v1/auth/dev-login", json={"nickname": nickname})
    assert res.status_code == 200, res.text
    return res.json()


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_production_rejects_weak_settings() -> None:
    with pytest.raises(RuntimeError):
        Settings(app_env="production", allow_dev_login=False).validate_production_security()
    with pytest.raises(RuntimeError):
        Settings(
            app_env="production", jwt_secret_key="x" * 64, allow_dev_login=True
        ).validate_production_security()
    with pytest.raises(RuntimeError):
        Settings(
            app_env="production",
            jwt_secret_key="x" * 64,
            allow_dev_login=False,
            family_registration_code="short",
        ).validate_production_security()
    Settings(
        app_env="production", jwt_secret_key="x" * 64, allow_dev_login=False
    ).validate_production_security()
    Settings(
        app_env="production",
        jwt_secret_key="x" * 64,
        allow_dev_login=False,
        family_registration_code="our-home-secret-2026",
    ).validate_production_security()
    Settings(app_env="local").validate_production_security()


def test_refresh_reuse_revokes_all_tokens(client: TestClient) -> None:
    tokens = _login(client, "reuse")
    old_refresh = tokens["refresh_token"]
    rotated = client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})
    assert rotated.status_code == 200
    new_refresh = rotated.json()["refresh_token"]

    # 폐기된 토큰 재사용 -> 401 및 활성 토큰 전체 폐기
    reuse = client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})
    assert reuse.status_code == 401
    after = client.post("/api/v1/auth/refresh", json={"refresh_token": new_refresh})
    assert after.status_code == 401


def test_logout_rejects_other_users_refresh_token(client: TestClient) -> None:
    a = _login(client, "alice")
    b = _login(client, "bob")
    res = client.post(
        "/api/v1/auth/logout",
        json={"refresh_token": a["refresh_token"]},
        headers=_auth(b["access_token"]),
    )
    assert res.status_code == 403
    # Alice의 토큰은 그대로 유효
    ok = client.post("/api/v1/auth/refresh", json={"refresh_token": a["refresh_token"]})
    assert ok.status_code == 200


def test_auth_rate_limit_returns_429(client: TestClient, monkeypatch) -> None:
    monkeypatch.setattr(global_settings, "rate_limit_auth_per_minute", 3)
    codes = [
        client.post("/api/v1/auth/dev-login", json={"nickname": f"u{i}"}).status_code
        for i in range(5)
    ]
    assert codes[:3] == [200, 200, 200]
    assert codes[3:] == [429, 429]


def test_invite_code_cannot_exceed_max_uses(client: TestClient, db_session) -> None:
    owner = _login(client, "owner")
    household = client.post(
        "/api/v1/households",
        json={"name": "우리집"},
        headers=_auth(owner["access_token"]),
    ).json()
    invite = client.post(
        f"/api/v1/households/{household['id']}/invites",
        json={"max_uses": 1},
        headers=_auth(owner["access_token"]),
    )
    assert invite.status_code in (200, 201), invite.text
    code = invite.json()["code"]

    first = _login(client, "joiner1")
    second = _login(client, "joiner2")
    r1 = client.post(
        "/api/v1/households/join", json={"code": code}, headers=_auth(first["access_token"])
    )
    r2 = client.post(
        "/api/v1/households/join", json={"code": code}, headers=_auth(second["access_token"])
    )
    assert r1.status_code == 200, r1.text
    assert r2.status_code == 400
    assert r2.json()["error"]["code"] == "INVITE_CODE_EXHAUSTED"


def test_health_failure_does_not_leak_exception(client: TestClient, monkeypatch) -> None:
    import app.main as main_module

    def boom(*_a, **_k):
        raise RuntimeError("secret-connection-string")

    monkeypatch.setattr(main_module, "check_db_health", boom)
    res = client.get("/api/v1/health")
    assert res.status_code == 503
    assert "secret-connection-string" not in res.text
    assert "detail" not in res.json()


def test_social_endpoints_removed(client: TestClient) -> None:
    assert client.post("/api/v1/auth/social/google", json={"id_token": "x"}).status_code == 404
    assert client.post("/api/v1/auth/social/kakao", json={"access_token": "x"}).status_code == 404


def test_family_registration_code_and_two_user_cap(client: TestClient, monkeypatch) -> None:
    monkeypatch.setattr(global_settings, "family_registration_code", "")
    disabled = client.post(
        "/api/v1/auth/register",
        json={
            "username": "husband",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "any-code",
        },
    )
    assert disabled.status_code == 403
    assert disabled.json()["error"]["code"] == "REGISTRATION_DISABLED"

    monkeypatch.setattr(global_settings, "family_registration_code", "our-home-secret-2026")
    monkeypatch.setattr(global_settings, "max_registered_users", 2)

    wrong_code = client.post(
        "/api/v1/auth/register",
        json={
            "username": "husband",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "wrong-code",
        },
    )
    assert wrong_code.status_code == 403
    assert wrong_code.json()["error"]["code"] == "INVALID_REGISTRATION_CODE"

    r1 = client.post(
        "/api/v1/auth/register",
        json={
            "username": "husband",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "our-home-secret-2026",
        },
    )
    assert r1.status_code == 201, r1.text

    dup = client.post(
        "/api/v1/auth/register",
        json={
            "username": "HUSBAND",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "our-home-secret-2026",
        },
    )
    assert dup.status_code == 409
    assert dup.json()["error"]["code"] == "USERNAME_ALREADY_EXISTS"

    r2 = client.post(
        "/api/v1/auth/register",
        json={
            "username": "wife",
            "password": "StrongPassword!2",
            "nickname": "아내",
            "registration_code": "our-home-secret-2026",
        },
    )
    assert r2.status_code == 201, r2.text

    # 3번째 외부인 가입은 올바른 등록코드여도 자동 차단
    r3 = client.post(
        "/api/v1/auth/register",
        json={
            "username": "outsider",
            "password": "StrongPassword!3",
            "nickname": "외부인",
            "registration_code": "our-home-secret-2026",
        },
    )
    assert r3.status_code == 403
    assert r3.json()["error"]["code"] == "REGISTRATION_CLOSED"


def test_login_lockout_after_five_failures(client: TestClient, monkeypatch) -> None:
    monkeypatch.setattr(global_settings, "family_registration_code", "our-home-secret-2026")
    monkeypatch.setattr(global_settings, "rate_limit_enabled", False)
    monkeypatch.setattr(global_settings, "auth_max_failed_attempts", 5)
    monkeypatch.setattr(global_settings, "auth_lockout_minutes", 15)

    reg = client.post(
        "/api/v1/auth/register",
        json={
            "username": "husband",
            "password": "StrongPassword!1",
            "nickname": "남편",
            "registration_code": "our-home-secret-2026",
        },
    )
    assert reg.status_code == 201

    for _ in range(5):
        res = client.post(
            "/api/v1/auth/login",
            json={"username": "husband", "password": "WrongPassword!"},
        )
        assert res.status_code == 401

    # 5회 실패 후 올바른 비밀번호를 입력해도 잠금 유지
    locked = client.post(
        "/api/v1/auth/login",
        json={"username": "husband", "password": "StrongPassword!1"},
    )
    assert locked.status_code == 429
    assert locked.json()["error"]["code"] == "ACCOUNT_LOCKED"
