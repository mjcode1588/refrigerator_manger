from fastapi.testclient import TestClient


def _login_user(client: TestClient, nickname: str, email: str) -> dict[str, str]:
    tokens = client.post(
        "/api/v1/auth/dev-login",
        json={"nickname": nickname, "email": email},
    ).json()
    return {"Authorization": f"Bearer {tokens['access_token']}"}


def test_shopping_list_shared_purchase_tracking_and_cleanup(client: TestClient) -> None:
    minsu_headers = _login_user(client, "민수", "minsu@example.com")
    hh = client.post(
        "/api/v1/households",
        json={"name": "우리집", "create_default_locations": False},
        headers=minsu_headers,
    ).json()
    hh_id = hh["id"]

    # Invite 민지 to join the same household
    invite = client.post(
        f"/api/v1/households/{hh_id}/invites",
        json={"expires_in_hours": 24, "max_uses": 2},
        headers=minsu_headers,
    ).json()
    minji_headers = _login_user(client, "민지", "minji@example.com")
    join_res = client.post(
        "/api/v1/households/join",
        json={"code": invite["code"]},
        headers=minji_headers,
    )
    assert join_res.status_code == 200

    # 1. 민수 adds two shopping items
    milk = client.post(
        f"/api/v1/households/{hh_id}/shopping-items",
        json={
            "name": "서울우유 1L",
            "category": "DAIRY",
            "quantity": 2,
            "unit": "팩",
            "memo": "저지방으로",
        },
        headers=minsu_headers,
    )
    assert milk.status_code == 201
    milk_data = milk.json()
    assert milk_data["is_purchased"] is False
    assert milk_data["purchased_by_nickname"] is None

    eggs = client.post(
        f"/api/v1/households/{hh_id}/shopping-items",
        json={"name": "계란 30구", "category": "FRESH", "quantity": 1, "unit": "판"},
        headers=minsu_headers,
    )
    assert eggs.status_code == 201
    eggs_data = eggs.json()

    # 2. 민지 marks 서울우유 1L as purchased
    purchased = client.patch(
        f"/api/v1/households/{hh_id}/shopping-items/{milk_data['id']}",
        json={"is_purchased": True},
        headers=minji_headers,
    )
    assert purchased.status_code == 200
    purchased_data = purchased.json()
    assert purchased_data["is_purchased"] is True
    assert purchased_data["purchased_by_nickname"] == "민지"
    assert purchased_data["purchased_at"] is not None

    # 3. 민수 views the list: unpurchased(계란) first, purchased(서울우유) at bottom
    listing = client.get(
        f"/api/v1/households/{hh_id}/shopping-items", headers=minsu_headers
    )
    assert listing.status_code == 200
    items = listing.json()
    assert [i["id"] for i in items] == [eggs_data["id"], milk_data["id"]]
    assert items[1]["is_purchased"] is True
    assert items[1]["purchased_by_nickname"] == "민지"

    # 4. Unpurchase restores item and clears purchaser metadata
    unpurchased = client.patch(
        f"/api/v1/households/{hh_id}/shopping-items/{milk_data['id']}",
        json={"is_purchased": False, "memo": "일반 우유도 괜찮음"},
        headers=minsu_headers,
    )
    assert unpurchased.status_code == 200
    assert unpurchased.json()["is_purchased"] is False
    assert unpurchased.json()["purchased_by"] is None
    assert unpurchased.json()["purchased_by_nickname"] is None
    assert unpurchased.json()["purchased_at"] is None
    assert unpurchased.json()["memo"] == "일반 우유도 괜찮음"

    # 5. Mark 계란 as purchased, then clear purchased items
    client.patch(
        f"/api/v1/households/{hh_id}/shopping-items/{eggs_data['id']}",
        json={"is_purchased": True},
        headers=minji_headers,
    )
    cleared = client.post(
        f"/api/v1/households/{hh_id}/shopping-items/clear-purchased",
        headers=minsu_headers,
    )
    assert cleared.status_code == 200
    assert cleared.json()["deleted_count"] == 1

    remaining = client.get(
        f"/api/v1/households/{hh_id}/shopping-items", headers=minsu_headers
    ).json()
    assert [i["id"] for i in remaining] == [milk_data["id"]]

    # 6. Delete remaining item
    del_res = client.delete(
        f"/api/v1/households/{hh_id}/shopping-items/{milk_data['id']}",
        headers=minsu_headers,
    )
    assert del_res.status_code == 204
    assert (
        client.get(
            f"/api/v1/households/{hh_id}/shopping-items", headers=minsu_headers
        ).json()
        == []
    )


def test_shopping_list_forbidden_for_non_member(client: TestClient) -> None:
    owner_headers = _login_user(client, "소유자", "owner@example.com")
    hh = client.post(
        "/api/v1/households",
        json={"name": "소유자집", "create_default_locations": False},
        headers=owner_headers,
    ).json()

    outsider_headers = _login_user(client, "외부인", "outsider@example.com")
    res = client.get(
        f"/api/v1/households/{hh['id']}/shopping-items",
        headers=outsider_headers,
    )
    assert res.status_code == 403
