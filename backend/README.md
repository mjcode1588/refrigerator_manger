# Refrigerator Manager API

FastAPI backend for the refrigerator manager app. Clean architecture layout with use-cases, infra adapters, and FastAPI interface.

## Stack
- FastAPI + Pydantic v2
- SQLAlchemy 2.0 (async) + Alembic
- PostgreSQL
- JWT auth

## Architecture
- `app/domain`: entities and business policies
- `app/application`: use-cases and ports (interfaces)
- `app/infrastructure`: DB/LLM/image adapters
- `app/interfaces`: FastAPI routers and HTTP wiring

## Setup
1. Create a virtual environment and install deps:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

2. Configure environment variables:

```bash
cp .env.example .env
```

3. Run migrations:

```bash
alembic upgrade head
```

4. Start the API:

```bash
uvicorn app.main:app --reload
```

OpenAPI docs are served at `/docs`.

## Tests (TDD baseline)

```bash
pytest
```

If running from repo root, set `PYTHONPATH=backend`.

## Cron / Expiry Notifications
Use a Render cron job (or any scheduler) to call:

```
POST /api/v1/notifications/cron/expiring
Header: X-Cron-Secret: <CRON_SECRET>
```

## Notes
- Image uploads are stored locally (not persistent on Render free tier). Use external storage for production.
- LLM integration is stubbed; plug Gemini in `app/infrastructure/llm/client.py`.
