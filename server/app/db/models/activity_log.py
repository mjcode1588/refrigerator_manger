from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, BigIntPK, pk_identity_column


class ActivityLog(Base):
    __tablename__ = "activity_logs"
    __table_args__ = (
        Index("ix_activity_logs_household_created", "household_id", "created_at"),
    )

    id: Mapped[int] = pk_identity_column()
    household_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("households.id", ondelete="CASCADE"), nullable=False
    )
    actor_user_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
    entity_type: Mapped[str] = mapped_column(String(20), nullable=False)
    entity_id: Mapped[int] = mapped_column(BigIntPK, nullable=False)
    action_type: Mapped[str] = mapped_column(String(20), nullable=False)
    detail_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
