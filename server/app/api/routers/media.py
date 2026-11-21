from fastapi import APIRouter, Depends, Request, status
from fastapi.responses import FileResponse

from app.api.deps import get_current_user
from app.core.errors import AppError
from app.db.models import User
from app.services.media import resolve_media_path, save_media_bytes

router = APIRouter(prefix="/media", tags=["media"])


@router.put("/upload/{image_key:path}", status_code=status.HTTP_200_OK)
async def upload_media_object(
    image_key: str,
    request: Request,
    _user: User = Depends(get_current_user),
) -> dict[str, str]:
    payload = await request.body()
    save_media_bytes(image_key, payload)
    return {"image_key": image_key, "status": "uploaded"}


@router.get("/{image_key:path}")
def get_media_object(image_key: str) -> FileResponse:
    target = resolve_media_path(image_key)
    if not target.is_file():
        raise AppError("MEDIA_NOT_FOUND", "이미지 파일을 찾을 수 없습니다.", 404)
    return FileResponse(path=target, media_type="image/webp")
