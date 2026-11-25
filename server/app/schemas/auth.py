from pydantic import BaseModel, Field


class RegisterRequest(BaseModel):
    username: str = Field(min_length=3, max_length=32, pattern=r"^[a-zA-Z0-9_-]+$")
    password: str = Field(min_length=8, max_length=128)
    nickname: str = Field(min_length=1, max_length=100)
    registration_code: str = Field(min_length=1, max_length=128)


class LoginRequest(BaseModel):
    username: str = Field(min_length=1, max_length=64)
    password: str = Field(min_length=1, max_length=128)


class DevLoginRequest(BaseModel):
    nickname: str = Field(min_length=1, max_length=100)
    email: str | None = Field(default=None, max_length=255)


class RefreshTokenRequest(BaseModel):
    refresh_token: str = Field(min_length=1)


class LogoutRequest(BaseModel):
    refresh_token: str = Field(min_length=1)


class UserSummary(BaseModel):
    id: int
    nickname: str
    email: str | None = None


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    user: UserSummary


class UserHouseholdBrief(BaseModel):
    id: int
    name: str
    role: str


class UserMeResponse(BaseModel):
    id: int
    nickname: str
    email: str | None = None
    households: list[UserHouseholdBrief]


class UpdateUserMeRequest(BaseModel):
    nickname: str = Field(min_length=1, max_length=100)
