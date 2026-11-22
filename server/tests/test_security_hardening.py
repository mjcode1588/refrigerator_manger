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
    Settings(
        app_env="production", jwt_secret_key="x" * 64, allow_dev_login=False
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
