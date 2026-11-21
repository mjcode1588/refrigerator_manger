from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.errors import AppError
from app.db.models import Item


def generate_item_image_keys(household_id: int) -> tuple[str, str]:
    date_part = datetime.now(UTC).strftime("%Y%m%d")
    uid = uuid4().hex
    detail_key = f"households/{household_id}/items/{date_part}/{uid}_detail.webp"
    thumb_key = f"households/{household_id}/items/{date_part}/{uid}_thumb.webp"
    return detail_key, thumb_key


def resolve_media_path(image_key: str) -> Path:
    clean_key = image_key.lstrip("/")
    if not clean_key or ".." in clean_key.split("/"):
        raise AppError("INVALID_MEDIA_KEY", "유효하지 않은 미디어 키입니다.", 400)
    base_dir = Path(settings.media_storage_dir).resolve()
    target = (base_dir / clean_key).resolve()
    if not target.is_relative_to(base_dir):
        raise AppError("INVALID_MEDIA_KEY", "허용되지 않은 미디어 경로입니다.", 400)
    return target


def build_media_url(image_key: str | None) -> str | None:
    if not image_key:
        return None
    base = settings.media_public_base_url.rstrip("/")
    if base:
        return f"{base}/{image_key.lstrip('/')}"
    return f"{settings.api_v1_prefix}/media/{image_key.lstrip('/')}"


def build_upload_url(image_key: str) -> str:
    return f"{settings.api_v1_prefix}/media/upload/{image_key.lstrip('/')}"


def save_media_bytes(image_key: str, payload: bytes) -> Path:
    is_thumb = image_key.endswith("_thumb.webp")
    max_bytes = settings.media_max_thumb_bytes if is_thumb else settings.media_max_detail_bytes
    if len(payload) == 0:
        raise AppError("EMPTY_MEDIA_FILE", "빈 이미지 파일은 업로드할 수 없습니다.", 400)
    if len(payload) > max_bytes:
        limit_kb = max_bytes // 1024
        raise AppError(
            "MEDIA_FILE_TOO_LARGE",
            f"이미지 크기가 상한({limit_kb}KB)을 초과했습니다.",
            413,
        )
    target = resolve_media_path(image_key)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(payload)
    return target


def delete_media_file(image_key: str | None) -> None:
    if not image_key:
        return
    try:
        target = resolve_media_path(image_key)
        if target.is_file():
            target.unlink()
    except AppError:
        pass


def cleanup_expired_detail_images(db: Session, retention_days: int = 30) -> int:
    """소비·폐기 후 지정 일수가 지난 품목의 detail 이미지를 정리합니다."""
    cutoff = datetime.now(UTC) - timedelta(days=retention_days)
    stmt = select(Item).where(
        Item.status.in_(["CONSUMED", "DISCARDED"]),
        Item.detail_image_key.is_not(None),
        Item.updated_at <= cutoff,
    )
    targets = list(db.execute(stmt).scalars().all())
    cleaned = 0
    for item in targets:
        delete_media_file(item.detail_image_key)
        item.detail_image_key = None
        cleaned += 1
    if cleaned > 0:
        db.commit()
    return cleaned
