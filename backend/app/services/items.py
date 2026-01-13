from __future__ import annotations

from datetime import date
from typing import Iterable

from app.core.config import settings
from app.schemas.item import ItemCandidate
from app.services import llm


def determine_status(expiry_date: date | None, today: date, expiring_days: int | None = None) -> str:
    if not expiry_date:
        return "fresh"
    if expiry_date < today:
        return "expired"
    threshold = expiring_days if expiring_days is not None else settings.default_expiring_days
    if (expiry_date - today).days <= threshold:
        return "expiring"
    return "fresh"


def candidates_from_inputs(text: str | None, image_names: Iterable[str]) -> list[ItemCandidate]:
    candidates: list[ItemCandidate] = []
    if text:
        candidates.extend(llm.extract_candidates_from_text(text))
    if image_names:
        candidates.extend(llm.extract_candidates_from_images(image_names))
    return candidates
