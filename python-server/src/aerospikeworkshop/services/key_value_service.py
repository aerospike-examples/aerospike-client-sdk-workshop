"""Key-value service interface — mirrors KeyValueServiceInterface.java."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol, runtime_checkable

from aerospikeworkshop.models.cart import Cart
from aerospikeworkshop.models.product import Product


@dataclass
class QueryResult:
    products: list[Product]
    time_ms: int


@runtime_checkable
class KeyValueService(Protocol):
    async def connect(self) -> None: ...

    async def close(self) -> None: ...

    async def clear_all_data(self) -> None: ...

    async def get_product(self, product_id: str) -> Product | None: ...

    async def query(self, index: str, filter_value: str, count: int) -> QueryResult: ...

    async def load_categories(
        self,
        category: str,
        sub_category: str,
        article_type: str,
        usage: str,
        brand_name: str,
    ) -> None: ...

    async def create_string_index(self, bin_name: str, index_name: str) -> None: ...

    async def store_product(self, product: Product) -> None: ...

    async def get_categories(self) -> list[str]: ...

    async def get_article_types(self) -> list[str]: ...

    async def get_usage(self) -> list[str]: ...

    async def get_brand_names(self) -> list[str]: ...

    async def advanced_search(
        self,
        category: str | None,
        article_type: str | None,
        usage: str | None,
        brand_name: str | None,
        search_text: str | None,
        count: int,
    ) -> QueryResult: ...

    async def get_product_count(self) -> int: ...

    async def get_cart(self, user_id: str) -> Cart: ...

    async def add_to_cart(self, user_id: str, product_id: str, quantity: int) -> Cart: ...

    async def update_cart_item(
        self, user_id: str, product_id: str, quantity: int
    ) -> Cart: ...

    async def remove_from_cart(self, user_id: str, product_id: str) -> Cart: ...

    async def clear_cart(self, user_id: str) -> Cart: ...
