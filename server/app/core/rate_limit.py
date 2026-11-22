import threading
import time
from collections import defaultdict, deque

from fastapi import Request

from app.core.config import settings
from app.core.errors import AppError


class RateLimiter:
    """IP+버킷 기준 슬라이딩 윈도우(60초) 제한기. 단일 프로세스 메모리 기반."""

    def __init__(self, window_seconds: int = 60) -> None:
        self.window = window_seconds
        self._hits: dict[str, deque[float]] = defaultdict(deque)
        self._lock = threading.Lock()

    def hit(self, key: str, limit: int) -> bool:
        now = time.monotonic()
        with self._lock:
            q = self._hits[key]
            while q and now - q[0] > self.window:
                q.popleft()
            if len(q) >= limit:
                return False
            q.append(now)
            return True


def _client_ip(request: Request) -> str:
    return request.client.host if request.client else "unknown"


def _check(request: Request, bucket: str, limit: int) -> None:
    if not settings.rate_limit_enabled:
        return
    limiter: RateLimiter = request.app.state.rate_limiter
    if not limiter.hit(f"{bucket}:{_client_ip(request)}", limit):
        raise AppError(
            "RATE_LIMITED",
            "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
            429,
        )


def limit_auth(request: Request) -> None:
    _check(request, "auth", settings.rate_limit_auth_per_minute)


def limit_join(request: Request) -> None:
    _check(request, "join", settings.rate_limit_join_per_minute)
