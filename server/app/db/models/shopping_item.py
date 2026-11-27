from datetime import datetime
from decimal import Decimal

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, BigIntPK, TimestampMixin, pk_identity_column


class ShoppingItem(Base, TimestampMixin):
    __tablename__ = "shopping_items"
    __table_args__ = (
        Index(
            "ix_shopping_items_household_purchased",
            "household_id",
            "is_purchased",
            "created_at",
        ),
    )

    id: Mapped[int] = pk_identity_column()
    household_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("households.id", ondelete="CASCADE"), nullable=False
    )
    name: Mapped[str] = mapped_column(String(150), nullable=False)
    category: Mapped[str] = mapped_column(String(30), nullable=False, default="ETC")
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(10, 2), nullable=False, default=Decimal("1.00")
    )
    unit: Mapped[str] = mapped_column(String(30), nullable=False, default="개")
    memo: Mapped[str | None] = mapped_column(String(500), nullable=True)
    is_purchased: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    purchased_by: Mapped[int | None] = mapped_column(
        BigIntPK, ForeignKey("users.id", ondelete="SET NULL"), nullable=True
    )
    purchased_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_by: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
    updated_by: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
