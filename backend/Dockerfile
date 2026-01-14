# ================================
# Stage 1: Build Stage
# ================================
FROM python:3.12-slim AS builder

WORKDIR /app

# 시스템 의존성 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# 가상환경 생성
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# ================================
# Stage 2: Production Stage
# ================================
FROM python:3.12-slim AS runner

WORKDIR /app

# 런타임 의존성 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system --gid 1001 appgroup \
    && adduser --system --uid 1001 appuser

# 가상환경 복사
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 애플리케이션 코드 복사
COPY --chown=appuser:appgroup . .

# uploads 디렉토리 생성
RUN mkdir -p /app/uploads && chown appuser:appgroup /app/uploads

# 사용자 전환
USER appuser

# 포트 노출
EXPOSE 8000

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

# 서버 시작
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
