from __future__ import annotations

from typing import Iterable

from app.core.config import settings
from app.domain.entities import ItemCandidate, RecipeSuggestion


def _stub_guess_name(token: str) -> str:
    return token.strip().split(" ")[0]


def _stub_parse_quantity(token: str) -> tuple[float | None, str | None]:
    parts = token.strip().split(" ")
    if not parts:
        return None, None
    try:
        qty = float(parts[1]) if len(parts) > 1 else float(parts[0])
        unit = parts[2] if len(parts) > 2 else None
        return qty, unit
    except ValueError:
        return None, None


def extract_candidates_from_text(text: str) -> list[ItemCandidate]:
    candidates: list[ItemCandidate] = []
    for raw in [chunk.strip() for chunk in text.split(",") if chunk.strip()]:
        name = _stub_guess_name(raw)
        qty, unit = _stub_parse_quantity(raw)
        candidates.append(
            ItemCandidate(
                name=name,
                quantity=qty,
                unit=unit,
                expiry_date=None,
                storage_location=None,
                confidence=0.5,
                source="text",
            )
        )
    return candidates


def extract_candidates_from_images(filenames: Iterable[str]) -> list[ItemCandidate]:
    candidates: list[ItemCandidate] = []
    for name in filenames:
        clean = name.rsplit(".", 1)[0].replace("_", " ")
        item_name = clean.strip() or "unknown"
        candidates.append(
            ItemCandidate(
                name=item_name,
                quantity=None,
                unit=None,
                expiry_date=None,
                storage_location=None,
                confidence=0.4,
                source="image",
            )
        )
    return candidates


def suggest_recipes_stub(items: list[str], prefer_expiring_first: bool) -> list[RecipeSuggestion]:
    if not items:
        return [
            RecipeSuggestion(
                title="Simple pantry salad",
                steps=["Combine whatever you have.", "Season and serve."],
                use_items=[],
                missing_items=["olive oil", "salt"],
            )
        ]
    selected = items[:3]
    missing = ["salt", "oil"]
    title = "Quick mix with " + ", ".join(selected)
    steps = [
        f"Prepare {', '.join(selected)}.",
        "Cook or mix as needed.",
        "Season and serve.",
    ]
    return [RecipeSuggestion(title=title, steps=steps, use_items=selected, missing_items=missing)]


class StubLLMClient:
    def extract_candidates_from_text(self, text: str) -> list[ItemCandidate]:
        return extract_candidates_from_text(text)

    def extract_candidates_from_images(self, image_names: Iterable[str]) -> list[ItemCandidate]:
        return extract_candidates_from_images(image_names)

    def suggest_recipes(self, items: list[str], prefer_expiring_first: bool) -> list[RecipeSuggestion]:
        return suggest_recipes_stub(items, prefer_expiring_first)


def build_llm_client() -> StubLLMClient:
    if settings.llm_mode != "stub" and settings.gemini_api_key:
        # Gemini integration can be added here.
        return StubLLMClient()
    return StubLLMClient()
