import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status

from app.application.errors import ForbiddenError, NotFoundError
from app.application.use_cases.items import (
    confirm_items,
    delete_item,
    ingest_candidates,
    list_expiring,
    list_items,
    update_item,
)
from app.core.config import settings
from app.domain.entities import FileData, User
from app.infrastructure.repositories.fridges import SqlFridgeRepository
from app.infrastructure.repositories.items import SqlItemRepository
from app.interfaces.api.deps import get_current_user, get_fridge_repo, get_item_repo, get_llm_client, get_image_storage
from app.schemas.item import ItemConfirmRequest, ItemIngestResponse, ItemOut, ItemUpdate

router = APIRouter(prefix="/items", tags=["items"])


@router.post("/ingest", response_model=ItemIngestResponse)
async def ingest_items_handler(
    text: Annotated[str | None, Form()] = None,
    images: Annotated[list[UploadFile] | None, File()] = None,
    llm_client=Depends(get_llm_client),
    image_storage=Depends(get_image_storage),
) -> ItemIngestResponse:
    if not text and not images:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Provide text or images")
    image_names: list[str] = []
    if images:
        for image in images:
            content = await image.read()
            file_data = FileData(filename=image.filename or "upload", content=content)
            await image_storage.save(file_data)
            image_names.append(image.filename or "upload")
    candidates = await ingest_candidates(llm_client=llm_client, text=text, image_names=image_names)
    return ItemIngestResponse(candidates=[candidate for candidate in candidates])


@router.post("/image", response_model=ItemIngestResponse)
async def image_only_handler(
    image: UploadFile = File(...),
    llm_client=Depends(get_llm_client),
    image_storage=Depends(get_image_storage),
) -> ItemIngestResponse:
    content = await image.read()
    file_data = FileData(filename=image.filename or "upload", content=content)
    await image_storage.save(file_data)
    candidates = await ingest_candidates(
        llm_client=llm_client,
        text=None,
        image_names=[image.filename or "upload"],
    )
    return ItemIngestResponse(candidates=[candidate for candidate in candidates])


@router.post("/confirm", response_model=list[ItemOut], status_code=status.HTTP_201_CREATED)
async def confirm_items_handler(
    payload: ItemConfirmRequest,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
) -> list[ItemOut]:
    try:
        items = await confirm_items(
            fridge_repo=fridge_repo,
            item_repo=item_repo,
            fridge_id=payload.fridge_id,
            user_id=current_user.id,
            items=[item.model_dump() for item in payload.items],
            expiring_days=settings.default_expiring_days,
        )
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    return [ItemOut.model_validate(item) for item in items]


@router.get("", response_model=list[ItemOut])
async def list_items_handler(
    fridge_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
) -> list[ItemOut]:
    try:
        items = await list_items(fridge_repo=fridge_repo, item_repo=item_repo, fridge_id=fridge_id, user_id=current_user.id)
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    return [ItemOut.model_validate(item) for item in items]


@router.get("/expiring", response_model=list[ItemOut])
async def expiring_items_handler(
    fridge_id: uuid.UUID,
    days: int = settings.default_expiring_days,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
) -> list[ItemOut]:
    try:
        items = await list_expiring(
            fridge_repo=fridge_repo,
            item_repo=item_repo,
            fridge_id=fridge_id,
            user_id=current_user.id,
            days=days,
        )
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    return [ItemOut.model_validate(item) for item in items]


@router.put("/{item_id}", response_model=ItemOut)
async def update_item_handler(
    item_id: uuid.UUID,
    payload: ItemUpdate,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
) -> ItemOut:
    try:
        item = await update_item(
            fridge_repo=fridge_repo,
            item_repo=item_repo,
            item_id=item_id,
            user_id=current_user.id,
            updates=payload.model_dump(exclude_unset=True),
            expiring_days=settings.default_expiring_days,
        )
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    return ItemOut.model_validate(item)


@router.delete("/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_item_handler(
    item_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    fridge_repo: SqlFridgeRepository = Depends(get_fridge_repo),
    item_repo: SqlItemRepository = Depends(get_item_repo),
) -> None:
    try:
        await delete_item(fridge_repo=fridge_repo, item_repo=item_repo, item_id=item_id, user_id=current_user.id)
    except ForbiddenError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
