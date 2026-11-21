#!/bin/sh
set -eu

# DB 기동 직후 일시적 연결 지연에 대비해 Alembic 마이그레이션 최대 5회 재시도
attempt=1
max_attempts=5
until alembic upgrade head; do
  if [ "$attempt" -ge "$max_attempts" ]; then
    echo "Alembic migration failed after $max_attempts attempts." >&2
    exit 1
  fi
  echo "Waiting for database... (attempt $attempt/$max_attempts)"
  attempt=$((attempt + 1))
  sleep 2
done

exec uvicorn app.main:app \
  --host 0.0.0.0 \
  --port 8000 \
  --proxy-headers \
  --forwarded-allow-ips='*'
