from fastapi import APIRouter, Depends, HTTPException, status

from app.application.errors import ForbiddenError
from app.application.use_cases.recipes import suggest_recipes
from app.domain.entities import User
from app.infrastructure.repositories.fridges import SqlFridgeRepository
from app.infrastructure.repositories.items import SqlItemRepository
from app.interfaces.api.deps import get_current_user, get_fridge_repo, get_item_repo, get_llm_client
from app.schemas.recipe import RecipeSuggestRequest, RecipeSuggestResponse

router = APIRouter(prefix="/recipes", tags=["recipes"])


@router.post("/suggest", response_model=RecipeSuggestResponse)
async def suggest_recipes_handler(
    payload: RecipeSuggestRequest,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
    llm_client=Depends(get_llm_client),
) -> RecipeSuggestResponse:
    try:
        recipes = await suggest_recipes(
            fridge_repo=fridge_repo,
            item_repo=item_repo,
            llm_client=llm_client,
            fridge_id=payload.fridge_id,
            user_id=current_user.id,
            prefer_expiring_first=payload.prefer_expiring_first,
        )
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    return RecipeSuggestResponse(recipes=[recipe for recipe in recipes])
