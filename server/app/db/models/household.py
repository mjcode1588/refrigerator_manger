from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, BigIntPK, pk_identity_column


class Household(Base):
    __tablename__ = "households"

    id: Mapped[int] = pk_identity_column()
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    created_by: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    members: Mapped[list["HouseholdMember"]] = relationship(
        back_populates="household", cascade="all, delete-orphan"
    )
    invite_codes: Mapped[list["InviteCode"]] = relationship(
        back_populates="household", cascade="all, delete-orphan"
    )


class HouseholdMember(Base):
    __tablename__ = "household_members"
    __table_args__ = (
        UniqueConstraint("household_id", "user_id", name="uq_household_member"),
    )

    id: Mapped[int] = pk_identity_column()
    household_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("households.id", ondelete="CASCADE"), nullable=False, index=True
    )
    user_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    role: Mapped[str] = mapped_column(String(20), nullable=False, default="MEMBER")
    joined_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    household: Mapped["Household"] = relationship(back_populates="members")


class InviteCode(Base):
    __tablename__ = "invite_codes"

    id: Mapped[int] = pk_identity_column()
    household_id: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("households.id", ondelete="CASCADE"), nullable=False, index=True
    )
    code: Mapped[str] = mapped_column(String(16), nullable=False, unique=True)
    created_by: Mapped[int] = mapped_column(
        BigIntPK, ForeignKey("users.id"), nullable=False
    )
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    max_uses: Mapped[int] = mapped_column(Integer, nullable=False, default=5)
    used_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    household: Mapped["Household"] = relationship(back_populates="invite_codes")
