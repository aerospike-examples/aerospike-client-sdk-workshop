"""Application configuration from environment variables."""

from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict

ClientProfile = Literal["old-client", "new-client", "new-client-answers"]


class Settings(BaseSettings):
    """Settings mirroring spring-server application.yml."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    aerospike_host: str = "localhost"
    aerospike_port: int = 3000
    aerospike_username: str | None = None
    aerospike_password: str | None = None
    aerospike_client_profile: ClientProfile = "old-client"
    server_port: int = 8080


@lru_cache
def get_settings() -> Settings:
    return Settings()
