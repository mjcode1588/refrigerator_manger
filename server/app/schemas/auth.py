from pydantic import BaseModel, Field


class SocialLoginGoogleRequest(BaseModel):
    id_token: str = Field(min_length=1)


class SocialLoginKakaoRequest(BaseModel):
    access_token: str = Field(min_length=1)


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
