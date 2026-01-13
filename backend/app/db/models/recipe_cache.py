import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, JSON, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class RecipeCache(Base):
    __tablename__ = "recipes_cache"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    fridge_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("fridges.id"), index=True, nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    ingredients: Mapped[list[str]] = mapped_column(JSON, nullable=False)
    steps: Mapped[list[str]] = mapped_column(JSON, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
