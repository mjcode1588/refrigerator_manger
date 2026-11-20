from datetime import date
from decimal import Decimal

from fastapi.testclient import TestClient
from sqlalchemy import inspect
from sqlalchemy.orm import Session

from app.core.config import DbProvider, Settings
from app.db.engine import build_engine_kwargs, get_healthcheck_sql
from app.db.models import ActivityLog, Household, HouseholdMember, Item, StorageLocation, User


def test_build_engine_kwargs_for_multiple_providers() -> None:
    sqlite_cfg = Settings(db_provider=DbProvider.SQLITE, database_url="sqlite:///:memory:")
    sqlite_kwargs = build_engine_kwargs(sqlite_cfg)
    assert sqlite_kwargs["connect_args"] == {"check_same_thread": False}

    supa_cfg = Settings(
        db_provider=DbProvider.SUPABASE,
        database_url="postgresql+psycopg://user:pw@pooler.supabase.com:6543/postgres",
        supabase_use_transaction_pooler=True,
    )
    supa_kwargs = build_engine_kwargs(supa_cfg)
    assert supa_kwargs["pool_pre_ping"] is True
    assert supa_kwargs["connect_args"] == {"prepare_threshold": None}

    oracle_cfg = Settings(
        db_provider=DbProvider.ORACLE,
        database_url="oracle+oracledb://ADMIN:pw@adb_high",
        oracle_wallet_dir="/etc/oci-wallet",
        oracle_wallet_password="secret",
    )
    oracle_kwargs = build_engine_kwargs(oracle_cfg)
    assert oracle_kwargs["pool_pre_ping"] is True
    assert oracle_kwargs["connect_args"] == {
        "config_dir": "/etc/oci-wallet",
        "wallet_location": "/etc/oci-wallet",
        "wallet_password": "secret",
    }


def test_healthcheck_sql_by_provider() -> None:
    assert get_healthcheck_sql(DbProvider.ORACLE) == "SELECT 1 FROM DUAL"
    assert get_healthcheck_sql(DbProvider.SUPABASE) == "SELECT 1"
    assert get_healthcheck_sql(DbProvider.POSTGRES) == "SELECT 1"
    assert get_healthcheck_sql(DbProvider.SQLITE) == "SELECT 1"


def test_all_tables_created_and_tree_crud_works(db_session: Session) -> None:
    inspector = inspect(db_session.bind)
    tables = set(inspector.get_table_names())
    expected_tables = {
        "users",
        "social_accounts",
        "refresh_tokens",
        "households",
        "household_members",
        "invite_codes",
        "storage_locations",
        "items",
        "activity_logs",
    }
    assert expected_tables.issubset(tables)

    user = User(nickname="민수", email="minsu@example.com")
    db_session.add(user)
    db_session.flush()

    household = Household(name="우리집", created_by=user.id)
    db_session.add(household)
    db_session.flush()

    member = HouseholdMember(household_id=household.id, user_id=user.id, role="OWNER")
    root_loc = StorageLocation(
        household_id=household.id,
        parent_id=None,
        name="주방 양문형 냉장고",
        storage_type="FRIDGE",
    )
    db_session.add_all([member, root_loc])
    db_session.flush()

    child_loc = StorageLocation(
        household_id=household.id,
        parent_id=root_loc.id,
        name="냉장실 문쪽 선반",
        storage_type="FRIDGE",
    )
    db_session.add(child_loc)
    db_session.flush()

    item = Item(
        household_id=household.id,
        location_id=child_loc.id,
        name="서울우유 1L",
        category="DAIRY",
        quantity=Decimal("2.00"),
        unit="팩",
        expiry_date=date(2026, 11, 26),
        expiry_type="USE_BY",
        status="ACTIVE",
        created_by=user.id,
        updated_by=user.id,
    )
    db_session.add(item)
    db_session.flush()

    log = ActivityLog(
        household_id=household.id,
        actor_user_id=user.id,
        entity_type="ITEM",
        entity_id=item.id,
        action_type="CREATED",
        detail_json='{"name": "서울우유 1L"}',
    )
    db_session.add(log)
    db_session.commit()

    assert child_loc.parent_id == root_loc.id
    assert item.id is not None


def test_health_endpoint(client: TestClient) -> None:
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json() == {"status": "healthy", "db_provider": "sqlite"}


def test_web_prototype_served(client: TestClient) -> None:
    response = client.get("/web/")
    assert response.status_code == 200
    assert "우리집 식재료 관리" in response.text
