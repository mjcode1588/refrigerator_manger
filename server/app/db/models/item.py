from datetime import date
from decimal import Decimal

from sqlalchemy import Date, ForeignKey, Index, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, BigIntPK, TimestampMixin, pk_identity_column


class Item(Base, TimestampMixin):
    __tablename__ = "items"
    __table_args__ = (
        Index("ix_items_household_expiry", "household_id", "status", "expiry_date"),
        Index("ix_items_household_location", "household_id", "location_id", "status"),
    )

    id: Mapped[int] = pk_identity_column()
    household_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("households.id", ondelete="CASCADE"), nullable=False
    )
    location_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("storage_locations.id", ondelete="CASCADE"), nullable=False
    )
    name: Mapped[str] = mapped_column(String(150), nullable=False)
    category: Mapped[str] = mapped_column(String(30), nullable=False, default="ETC")
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(10, 2), nullable=False, default=Decimal("1.00")
    )
    unit: Mapped[str] = mapped_column(String(30), nullable=False, default="개")
    expiry_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    expiry_type: Mapped[str] = mapped_column(String(20), nullable=False, default="USE_BY")
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="ACTIVE")
    memo: Mapped[str | None] = mapped_column(String(500), nullable=True)
    created_by: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
    updated_by: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
