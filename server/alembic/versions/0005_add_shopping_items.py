"""Add shopping_items table for household shared shopping list

Revision ID: 0005_add_shopping_items
Revises: 0004_add_user_credentials
Create Date: 2026-11-26 16:45:00
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "0005_add_shopping_items"
down_revision: str | Sequence[str] | None = "0004_add_user_credentials"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "shopping_items",
        sa.Column("id", sa.BigInteger(), sa.Identity(always=False), nullable=False),
        sa.Column("household_id", sa.BigInteger(), nullable=False),
        sa.Column("name", sa.String(length=150), nullable=False),
        sa.Column("category", sa.String(length=30), nullable=False, server_default="ETC"),
        sa.Column(
            "quantity", sa.Numeric(precision=10, scale=2), nullable=False, server_default="1.00"
        ),
        sa.Column("unit", sa.String(length=30), nullable=False, server_default="개"),
        sa.Column("memo", sa.String(length=500), nullable=True),
        sa.Column("is_purchased", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("purchased_by", sa.BigInteger(), nullable=True),
        sa.Column("purchased_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=False),
        sa.Column("updated_by", sa.BigInteger(), nullable=False),
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
        sa.ForeignKeyConstraint(["purchased_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"]),
        sa.ForeignKeyConstraint(["updated_by"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_shopping_items_household_purchased",
        "shopping_items",
        ["household_id", "is_purchased", "created_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_shopping_items_household_purchased", table_name="shopping_items")
    op.drop_table("shopping_items")
