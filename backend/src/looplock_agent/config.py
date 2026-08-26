from functools import lru_cache

from pydantic import Field, PositiveInt
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="LOOPLOCK_",
        extra="ignore",
        case_sensitive=False,
    )

    model_name: str = "gemini-3.5-flash"
    google_cloud_project: str | None = Field(
        default=None,
        validation_alias="GOOGLE_CLOUD_PROJECT",
    )
    google_cloud_location: str = Field(
        default="global",
        validation_alias="GOOGLE_CLOUD_LOCATION",
    )
    google_genai_use_vertexai: bool = Field(
        default=True,
        validation_alias="GOOGLE_GENAI_USE_VERTEXAI",
    )
    firestore_database: str = "(default)"
    firestore_collection: str = "classification_events"
    processing_lease_seconds: PositiveInt = 60


@lru_cache
def get_settings() -> Settings:
    return Settings()
