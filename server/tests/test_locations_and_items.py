from datetime import date, timedelta

from fastapi.testclient import TestClient


def _setup_household(client: TestClient) -> tuple[dict[str, str], int]:
    tokens = client.post(
        "/api/v1/auth/dev-login",
        json={"nickname": "민수", "email": "minsu@example.com"},
    ).json()
    headers = {"Authorization": f"Bearer {tokens['access_token']}"}
    hh = client.post(
        "/api/v1/households",
        json={"name": "우리집", "create_default_locations": True},
        headers=headers,
    ).json()
    return headers, hh["id"]


def test_free_tree_locations_cycle_and_delete_strategies(client: TestClient) -> None:
    headers, hh_id = _setup_household(client)

    tree = client.get(f"/api/v1/households/{hh_id}/locations?format=tree", headers=headers).json()
    assert len(tree) == 2
    fridge = next(loc for loc in tree if loc["name"] == "주방 냉장고")
    cold_room = next(c for c in fridge["children"] if c["name"] == "냉장실")

    # Add 3rd level depth: 주방 냉장고 > 냉장실 > 문쪽 선반
    door_shelf = client.post(
        f"/api/v1/households/{hh_id}/locations",
        json={"parent_id": cold_room["id"], "name": "문쪽 선반", "storage_type": "FRIDGE"},
        headers=headers,
    )
    assert door_shelf.status_code == 201
    door_data = door_shelf.json()
    assert door_data["depth"] == 2
    assert door_data["full_path"] == "주방 냉장고 > 냉장실 > 문쪽 선반"

    # Attempt cycle: moving 주방 냉장고 under 문쪽 선반 must fail
    cycle = client.patch(
        f"/api/v1/households/{hh_id}/locations/{fridge['id']}",
        json={"parent_id": door_data["id"], "change_parent": True},
        headers=headers,
    )
    assert cycle.status_code == 400
    assert cycle.json()["error"]["code"] == "LOCATION_CYCLE_DETECTED"

    # Restrict delete fails when children exist
    restrict_del = client.delete(
        f"/api/v1/households/{hh_id}/locations/{cold_room['id']}?strategy=restrict",
        headers=headers,
    )
    assert restrict_del.status_code == 409

    # Move delete moves child to pantry
    pantry = next(loc for loc in tree if loc["name"] == "팬트리")
    move_del = client.delete(
        f"/api/v1/households/{hh_id}/locations/{cold_room['id']}?strategy=move&move_to_location_id={pantry['id']}",
        headers=headers,
    )
    assert move_del.status_code == 200

    flat = client.get(f"/api/v1/households/{hh_id}/locations?format=flat", headers=headers).json()
    moved_door = next(loc for loc in flat if loc["id"] == door_data["id"])
    assert moved_door["full_path"] == "팬트리 > 문쪽 선반"


def test_items_crud_consume_discard_expiring_and_activity_logs(client: TestClient) -> None:
    headers, hh_id = _setup_household(client)
    flat = client.get(f"/api/v1/households/{hh_id}/locations?format=flat", headers=headers).json()
    fridge = next(loc for loc in flat if loc["name"] == "주방 냉장고")
    cold_room = next(loc for loc in flat if loc["name"] == "냉장실")

    tomorrow = (date.today() + timedelta(days=1)).isoformat()
    later = (date.today() + timedelta(days=20)).isoformat()

    # Create item with blank memo -> normalized to None
    milk_resp = client.post(
        f"/api/v1/households/{hh_id}/items",
        json={
            "location_id": cold_room["id"],
            "name": "서울우유 1L",
            "category": "DAIRY",
            "quantity": 2,
            "unit": "팩",
            "expiry_date": tomorrow,
            "expiry_type": "USE_BY",
            "memo": "   ",
        },
        headers=headers,
    )
    assert milk_resp.status_code == 201
    milk = milk_resp.json()
    assert milk["memo"] is None
    assert milk["initial_quantity"] == 2.0
    assert milk["location_path"] == "주방 냉장고 > 냉장실"

    pasta_resp = client.post(
        f"/api/v1/households/{hh_id}/items",
        json={
            "location_id": cold_room["id"],
            "name": "토마토 파스타 소스",
            "category": "SAUCE",
            "quantity": 1,
            "unit": "병",
            "expiry_date": later,
            "expiry_type": "BEST_BEFORE",
        },
        headers=headers,
    )
    assert pasta_resp.status_code == 201
    pasta = pasta_resp.json()

    # Sublocations filter on parent fridge includes cold_room items
    fridge_items = client.get(
        f"/api/v1/households/{hh_id}/items?location_id={fridge['id']}&include_sublocations=true",
        headers=headers,
    ).json()
    assert len(fridge_items) == 2

    # Expiring within 3 days includes only milk
    expiring = client.get(
        f"/api/v1/households/{hh_id}/items/expiring?within_days=3",
        headers=headers,
    ).json()
    assert [it["id"] for it in expiring] == [milk["id"]]

    # Partial consume milk by remaining_quantity + note: 2 -> 1.5
    c0 = client.post(
        f"/api/v1/households/{hh_id}/items/{milk['id']}/consume",
        json={"remaining_quantity": 1.5, "note": "아침 시리얼에 반 팩 사용"},
        headers=headers,
    ).json()
    assert c0["quantity"] == 1.5
    assert c0["initial_quantity"] == 2.0
    assert c0["status"] == "ACTIVE"
    assert c0["memo"] == "아침 시리얼에 반 팩 사용"

    # Partial consume milk by amount: 1.5 -> 1.0
    c1 = client.post(
        f"/api/v1/households/{hh_id}/items/{milk['id']}/consume",
        json={"amount": 0.5},
        headers=headers,
    ).json()
    assert c1["quantity"] == 1.0
    assert c1["initial_quantity"] == 2.0
    assert c1["status"] == "ACTIVE"

    # Restocking beyond initial_quantity updates initial_quantity (2.0 -> 3.0)
    u1 = client.patch(
        f"/api/v1/households/{hh_id}/items/{milk['id']}",
        json={"quantity": 3.0},
        headers=headers,
    ).json()
    assert u1["quantity"] == 3.0
    assert u1["initial_quantity"] == 3.0

    # Full consume milk: 1 -> 0 (CONSUMED)
    c2 = client.post(
        f"/api/v1/households/{hh_id}/items/{milk['id']}/consume",
        headers=headers,
    ).json()
    assert c2["quantity"] == 0.0
    assert c2["status"] == "CONSUMED"

    # Discard pasta
    d1 = client.post(
        f"/api/v1/households/{hh_id}/items/{pasta['id']}/discard",
        headers=headers,
    ).json()
    assert d1["status"] == "DISCARDED"

    # Activity logs captured CREATED, CONSUMED, DISCARDED
    acts = client.get(f"/api/v1/households/{hh_id}/activities", headers=headers).json()
    actions = [a["action_type"] for a in acts["items"]]
    assert "DISCARDED" in actions
    assert "CONSUMED" in actions
    assert "CREATED" in actions


def test_item_photo_upload_and_registration(client, tmp_path, monkeypatch):
    from app.core.config import settings

    monkeypatch.setattr(settings, "media_storage_dir", str(tmp_path))

    token = client.post(
        "/api/v1/auth/dev-login", json={"nickname": "사진테스터", "email": "photo@test.com"}
    ).json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    hh_id = client.post("/api/v1/households", json={"name": "사진 테스트집"}, headers=headers).json()[
        "id"
    ]
    loc_id = client.post(
        f"/api/v1/households/{hh_id}/locations",
        json={"name": "냉장실", "storage_type": "FRIDGE"},
        headers=headers,
    ).json()["id"]

    # Reject oversized thumb (> 100KB)
    bad_url_resp = client.post(
        f"/api/v1/households/{hh_id}/items/photo-upload-url",
        json={"detail_size_bytes": 100000, "thumb_size_bytes": 150000, "content_type": "image/webp"},
        headers=headers,
    )
    assert bad_url_resp.status_code == 422

    # Request valid upload URLs
    url_resp = client.post(
        f"/api/v1/households/{hh_id}/items/photo-upload-url",
        json={"detail_size_bytes": 2048, "thumb_size_bytes": 512, "content_type": "image/webp"},
        headers=headers,
    )
    assert url_resp.status_code == 200
    urls = url_resp.json()
    assert urls["detail_image_key"].endswith("_detail.webp")
    assert urls["thumb_image_key"].endswith("_thumb.webp")

    # Upload detail & thumb bytes
    detail_bytes = b"RIFF....WEBPdetail"
    thumb_bytes = b"RIFF....WEBPthumb"
    up_d = client.put(urls["detail_upload_url"], content=detail_bytes, headers=headers)
    up_t = client.put(urls["thumb_upload_url"], content=thumb_bytes, headers=headers)
    assert up_d.status_code == 200
    assert up_t.status_code == 200

    # Create item referencing uploaded keys
    item_resp = client.post(
        f"/api/v1/households/{hh_id}/items",
        json={
            "location_id": loc_id,
            "name": "방울토마토",
            "category": "FRESH",
            "quantity": 1,
            "unit": "팩",
            "detail_image_key": urls["detail_image_key"],
            "thumb_image_key": urls["thumb_image_key"],
        },
        headers=headers,
    )
    assert item_resp.status_code == 201
    item = item_resp.json()
    assert item["detail_image_key"] == urls["detail_image_key"]
    assert item["thumb_image_key"] == urls["thumb_image_key"]
    assert item["detail_image_url"].endswith(urls["detail_image_key"])
    assert item["thumb_image_url"].endswith(urls["thumb_image_key"])

    # Serve uploaded media
    fetch_thumb = client.get(item["thumb_image_url"])
    assert fetch_thumb.status_code == 200
    assert fetch_thumb.content == thumb_bytes
