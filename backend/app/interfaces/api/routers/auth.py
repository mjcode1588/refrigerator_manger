from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.application.errors import ConflictError, ValidationError
from app.application.use_cases.auth import login_user, register_user
from app.infrastructure.security import PasswordHasherAdapter, TokenIssuerAdapter
from app.infrastructure.repositories.users import SqlUserRepository
from app.interfaces.api.deps import get_current_user, get_db
from app.schemas.auth import LoginRequest, Token
from app.schemas.user import UserCreate, UserOut

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register_user_handler(payload: UserCreate, db: AsyncSession = Depends(get_db)) -> UserOut:
    repo = SqlUserRepository(db)
    try:
        user = await register_user(
            repo=repo,
            hasher=PasswordHasherAdapter(),
            email=payload.email,
            password=payload.password,
            name=payload.name,
            locale=payload.locale,
        )
    except ConflictError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return UserOut.model_validate(user)


@router.post("/login", response_model=Token)
async def login_handler(payload: LoginRequest, db: AsyncSession = Depends(get_db)) -> Token:
    repo = SqlUserRepository(db)
    try:
        token = await login_user(
            repo=repo,
            hasher=PasswordHasherAdapter(),
            token_issuer=TokenIssuerAdapter(),
            email=payload.email,
            password=payload.password,
        )
    except ValidationError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(exc)) from exc
    return Token(access_token=token)


@router.get("/me", response_model=UserOut)
async def get_me(current_user=Depends(get_current_user)) -> UserOut:
    return UserOut.model_validate(current_user)
