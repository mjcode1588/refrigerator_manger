from app.application.errors import ConflictError, ValidationError
from app.application.ports import PasswordHasher, TokenIssuer, UserRepository
from app.domain.entities import User


async def register_user(
    *,
    repo: UserRepository,
    hasher: PasswordHasher,
    email: str,
    password: str,
    name: str | None,
    locale: str | None,
) -> User:
    existing = await repo.get_by_email(email)
    if existing:
        raise ConflictError("Email already registered")
    hashed = hasher.hash(password)
    return await repo.create(email=email, hashed_password=hashed, name=name, locale=locale)


async def login_user(
    *,
    repo: UserRepository,
    hasher: PasswordHasher,
    token_issuer: TokenIssuer,
    email: str,
    password: str,
) -> str:
    user = await repo.get_by_email(email)
    if not user or not hasher.verify(password, user.hashed_password):
        raise ValidationError("Invalid credentials")
    return token_issuer.create_access_token(str(user.id))
