import os
import uuid
from pathlib import Path

from app.core.config import settings
from app.domain.entities import FileData


def ensure_upload_dir() -> Path:
    upload_path = Path(settings.upload_dir)
    upload_path.mkdir(parents=True, exist_ok=True)
    return upload_path


class LocalImageStorage:
    async def save(self, file: FileData) -> str:
        upload_dir = ensure_upload_dir()
        suffix = Path(file.filename or "").suffix
        filename = f"{uuid.uuid4().hex}{suffix}"
        target_path = upload_dir / filename

        target_path.write_bytes(file.content)

        if settings.image_base_url:
            return f"{settings.image_base_url.rstrip('/')}/{filename}"
        return os.path.join(settings.upload_dir, filename)


async def save_upload(file: FileData) -> str:
    storage = LocalImageStorage()
    return await storage.save(file)
