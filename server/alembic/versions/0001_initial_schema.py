"""Initial ANSI SQL schema for multi-DB (SQLite / PostgreSQL(Supabase) / Oracle)

Revision ID: 0001_initial_schema
Revises:
Create Date: 2026-11-20 05:35:00
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "0001_initial_schema"
down_revision: str | Sequence[str] | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

BIGINT_PK = sa.BigInteger().with_variant(sa.Integer(), "sqlite")


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("nickname", sa.String(length=100), nullable=False),
        sa.Column("email", sa.String(length=255), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "social_accounts",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("user_id", BIGINT_PK, nullable=False),
        sa.Column("provider", sa.String(length=20), nullable=False),
        sa.Column("provider_user_id", sa.String(length=255), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("provider", "provider_user_id", name="uq_social_provider_uid"),
    )
    op.create_index("ix_social_accounts_user_id", "social_accounts", ["user_id"])

    op.create_table(
        "refresh_tokens",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("user_id", BIGINT_PK, nullable=False),
        sa.Column("token_hash", sa.String(length=128), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("token_hash"),
    )
    op.create_index("ix_refresh_tokens_user_id", "refresh_tokens", ["user_id"])

    op.create_table(
        "households",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("created_by", BIGINT_PK, nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "household_members",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("household_id", BIGINT_PK, nullable=False),
        sa.Column("user_id", BIGINT_PK, nullable=False),
        sa.Column("role", sa.String(length=20), nullable=False),
        sa.Column(
            "joined_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["household_id"], ["households.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("household_id", "user_id", name="uq_household_member"),
    )
    op.create_index("ix_household_members_household_id", "household_members", ["household_id"])
    op.create_index("ix_household_members_user_id", "household_members", ["user_id"])

    op.create_table(
        "invite_codes",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("household_id", BIGINT_PK, nullable=False),
        sa.Column("code", sa.String(length=16), nullable=False),
        sa.Column("created_by", BIGINT_PK, nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("max_uses", sa.Integer(), nullable=False),
        sa.Column("used_count", sa.Integer(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"]),
        sa.ForeignKeyConstraint(["household_id"], ["households.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("code"),
    )
    op.create_index("ix_invite_codes_household_id", "invite_codes", ["household_id"])

    op.create_table(
        "storage_locations",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("household_id", BIGINT_PK, nullable=False),
        sa.Column("parent_id", BIGINT_PK, nullable=True),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("storage_type", sa.String(length=30), nullable=False),
        sa.Column("icon", sa.String(length=50), nullable=True),
        sa.Column("sort_order", sa.Integer(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["household_id"], ["households.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["parent_id"], ["storage_locations.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_storage_locations_tree",
        "storage_locations",
        ["household_id", "parent_id", "sort_order"],
    )

    op.create_table(
        "items",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("household_id", BIGINT_PK, nullable=False),
        sa.Column("location_id", BIGINT_PK, nullable=False),
        sa.Column("name", sa.String(length=150), nullable=False),
        sa.Column("category", sa.String(length=30), nullable=False),
        sa.Column("quantity", sa.Numeric(precision=10, scale=2), nullable=False),
        sa.Column("unit", sa.String(length=30), nullable=False),
        sa.Column("expiry_date", sa.Date(), nullable=True),
        sa.Column("expiry_type", sa.String(length=20), nullable=False),
        sa.Column("status", sa.String(length=20), nullable=False),
        sa.Column("memo", sa.String(length=500), nullable=True),
        sa.Column("created_by", BIGINT_PK, nullable=False),
        sa.Column("updated_by", BIGINT_PK, nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"]),
        sa.ForeignKeyConstraint(["household_id"], ["households.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["location_id"], ["storage_locations.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["updated_by"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_items_household_expiry", "items", ["household_id", "status", "expiry_date"])
    op.create_index(
        "ix_items_household_location", "items", ["household_id", "location_id", "status"]
    )

    op.create_table(
        "activity_logs",
        sa.Column("id", BIGINT_PK, sa.Identity(), nullable=False),
        sa.Column("household_id", BIGINT_PK, nullable=False),
        sa.Column("actor_user_id", BIGINT_PK, nullable=False),
        sa.Column("entity_type", sa.String(length=20), nullable=False),
        sa.Column("entity_id", BIGINT_PK, nullable=False),
        sa.Column("action_type", sa.String(length=20), nullable=False),
        sa.Column("detail_json", sa.Text(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["actor_user_id"], ["users.id"]),
        sa.ForeignKeyConstraint(["household_id"], ["households.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_activity_logs_household_created", "activity_logs", ["household_id", "created_at"]
    )


def downgrade() -> None:
    op.drop_index("ix_activity_logs_household_created", table_name="activity_logs")
    op.drop_table("activity_logs")
    op.drop_index("ix_items_household_location", table_name="items")
    op.drop_index("ix_items_household_expiry", table_name="items")
    op.drop_table("items")
    op.drop_index("ix_storage_locations_tree", table_name="storage_locations")
    op.drop_table("storage_locations")
    op.drop_index("ix_invite_codes_household_id", table_name="invite_codes")
    op.drop_table("invite_codes")
    op.drop_index("ix_household_members_user_id", table_name="household_members")
    op.drop_index("ix_household_members_household_id", table_name="household_members")
    op.drop_table("household_members")
    op.drop_table("households")
    op.drop_index("ix_refresh_tokens_user_id", table_name="refresh_tokens")
    op.drop_table("refresh_tokens")
    op.drop_index("ix_social_accounts_user_id", table_name="social_accounts")
    op.drop_table("social_accounts")
    op.drop_table("users")
