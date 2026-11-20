from sqlalchemy import ForeignKey, Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, BigIntPK, TimestampMixin, pk_identity_column


class StorageLocation(Base, TimestampMixin):
    __tablename__ = "storage_locations"
    __table_args__ = (
        Index("ix_storage_locations_tree", "household_id", "parent_id", "sort_order"),
    )

    id: Mapped[int] = pk_identity_column()
    household_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("households.id", ondelete="CASCADE"), nullable=False
    )
    parent_id: Mapped[int | None] = mapped_column(
        BigIntPK, ForeignKey("storage_locations.id", ondelete="CASCADE"), nullable=True
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    storage_type: Mapped[str] = mapped_column(String(30), nullable=False, default="CUSTOM")
    icon: Mapped[str | None] = mapped_column(String(50), nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    parent: Mapped["StorageLocation | None"] = relationship(
        remote_side="StorageLocation.id", back_populates="children"
    )
    children: Mapped[list["StorageLocation"]] = relationship(back_populates="parent")
