from app.core.security import create_access_token, get_password_hash, verify_password


class PasswordHasherAdapter:
    def hash(self, password: str) -> str:
        return get_password_hash(password)

    def verify(self, password: str, hashed_password: str) -> bool:
        return verify_password(password, hashed_password)


class TokenIssuerAdapter:
    def create_access_token(self, subject: str) -> str:
        return create_access_token(subject)
