"""Add detail_image_key and thumb_image_key columns to items table

Revision ID: 0003_add_item_image_keys
Revises: 0002_add_item_initial_quantity
Create Date: 2026-11-21 23:00:00
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "0003_add_item_image_keys"
down_revision: str | Sequence[str] | None = "0002_add_item_initial_quantity"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("items", sa.Column("detail_image_key", sa.String(length=255), nullable=True))
    op.add_column("items", sa.Column("thumb_image_key", sa.String(length=255), nullable=True))


def downgrade() -> None:
    op.drop_column("items", "thumb_image_key")
    op.drop_column("items", "detail_image_key")
