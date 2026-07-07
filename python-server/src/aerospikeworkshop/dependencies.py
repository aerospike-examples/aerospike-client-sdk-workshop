"""Factory for selecting the active KeyValueService implementation."""

from aerospikeworkshop.config import Settings
from aerospikeworkshop.services.key_value_service import KeyValueService
from aerospikeworkshop.services.key_value_service_new_client import KeyValueServiceNewClient
from aerospikeworkshop.services.key_value_service_new_client_answers import (
    KeyValueServiceNewClientAnswers,
)
from aerospikeworkshop.services.key_value_service_old_client import KeyValueServiceOldClient


def create_key_value_service(settings: Settings) -> KeyValueService:
    match settings.aerospike_client_profile:
        case "old-client":
            return KeyValueServiceOldClient(settings)
        case "new-client":
            return KeyValueServiceNewClient(settings)
        case "new-client-answers":
            return KeyValueServiceNewClientAnswers(settings)
        case _:
            raise ValueError(
                f"Unknown client profile: {settings.aerospike_client_profile}"
            )
