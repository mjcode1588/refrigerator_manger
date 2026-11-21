"""Add initial_quantity column to items table

Revision ID: 0002_add_item_initial_quantity
Revises: 0001_initial_schema
Create Date: 2026-11-21 22:51:00
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "0002_add_item_initial_quantity"
down_revision: str | Sequence[str] | None = "0001_initial_schema"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "items",
        sa.Column(
            "initial_quantity",
            sa.Numeric(precision=10, scale=2),
            nullable=False,
            server_default="1.00",
        ),
    )
    op.execute("UPDATE items SET initial_quantity = quantity")


def downgrade() -> None:
    op.drop_column("items", "initial_quantity")
