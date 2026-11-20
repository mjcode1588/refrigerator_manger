from datetime import UTC, datetime

from sqlalchemy import BigInteger, DateTime, Identity, Integer, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


def utc_now() -> datetime:
    return datetime.now(UTC)


# SQLite에서는 INTEGER PRIMARY KEY가 자동 증가(rowid)이므로 variant 지정,
# PostgreSQL/Oracle에서는 BIGINT/NUMBER IDENTITY로 매핑
BigIntPK = BigInteger().with_variant(Integer, "sqlite")


class Base(DeclarativeBase):
    pass


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=utc_now,
        server_default=func.now(),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=utc_now,
        onupdate=utc_now,
        server_default=func.now(),
        nullable=False,
    )


def pk_identity_column() -> Mapped[int]:
    return mapped_column(BigIntPK, Identity(), primary_key=True)
