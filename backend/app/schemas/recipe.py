import uuid

from pydantic import BaseModel


class RecipeSuggestRequest(BaseModel):
    fridge_id: uuid.UUID
    prefer_expiring_first: bool = True


class RecipeSuggestion(BaseModel):
    title: str
    steps: list[str]
    use_items: list[str]
    missing_items: list[str]

    model_config = {"from_attributes": True}


class RecipeSuggestResponse(BaseModel):
    recipes: list[RecipeSuggestion]
