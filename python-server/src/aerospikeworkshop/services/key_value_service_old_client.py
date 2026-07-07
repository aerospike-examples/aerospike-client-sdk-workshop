"""Legacy aerospike client implementation — mirrors KeyValueServiceOldClient.java."""

from __future__ import annotations

import asyncio
import time
from typing import Any

import aerospike
from aerospike import predicates as p
from aerospike_helpers import cdt_ctx
from aerospike_helpers.operations import list_operations, map_operations

from aerospikeworkshop.config import Settings
from aerospikeworkshop.models.cart import Cart
from aerospikeworkshop.models.cart_item import CartItem
from aerospikeworkshop.models.product import Product
from aerospikeworkshop.services.key_value_service import QueryResult
from aerospikeworkshop.services.utils import as_non_null_string, extract_product_image

ITEMS_BIN = "items"
NAMESPACE = "test"
PRODUCT_SET = "products"
CARTS_SET = "shopping_carts"
CATEGORY_SET = "cat_index"
CATEGORY_KEY = "product_meta"


class KeyValueServiceOldClient:
    """Active when AEROSPIKE_CLIENT_PROFILE=old-client."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._client: aerospike.Client | None = None

    async def connect(self) -> None:
        await asyncio.to_thread(self._connect_sync)

    def _connect_sync(self) -> None:
        config: dict[str, Any] = {
            "hosts": [(self._settings.aerospike_host, self._settings.aerospike_port)],
        }
        if self._settings.aerospike_username:
            config["user"] = self._settings.aerospike_username
            config["password"] = self._settings.aerospike_password or ""
        self._client = aerospike.client(config).connect()

    async def close(self) -> None:
        if self._client is not None:
            client = self._client
            self._client = None
            await asyncio.to_thread(client.close)

    def _require_client(self) -> aerospike.Client:
        if self._client is None:
            raise RuntimeError("Aerospike client is not connected")
        return self._client

    async def store_product(self, product: Product) -> None:
        await asyncio.to_thread(self._store_product_sync, product)

    def _store_product_sync(self, product: Product) -> None:
        client = self._require_client()
        key = (NAMESPACE, PRODUCT_SET, product.id)
        bins = product.to_bins()
        policy = {"exists": aerospike.POLICY_EXISTS_CREATE}
        client.put(key, bins, policy=policy)

    async def get_product(self, product_id: str) -> Product | None:
        return await asyncio.to_thread(self._get_product_sync, product_id)

    def _get_product_sync(self, product_id: str) -> Product | None:
        client = self._require_client()
        key = (NAMESPACE, PRODUCT_SET, product_id)
        try:
            _key, _meta, bins = client.get(key)
            return Product.from_bins(bins)
        except aerospike.exception.RecordNotFound:
            return None

    async def clear_all_data(self) -> None:
        await asyncio.to_thread(self._clear_all_data_sync)

    def _clear_all_data_sync(self) -> None:
        client = self._require_client()
        client.truncate(NAMESPACE, PRODUCT_SET, 0)
        client.truncate(NAMESPACE, CARTS_SET, 0)
        try:
            client.remove((NAMESPACE, CATEGORY_SET, CATEGORY_KEY))
        except aerospike.exception.RecordNotFound:
            pass

    async def query(self, index: str, filter_value: str, count: int) -> QueryResult:
        return await asyncio.to_thread(self._query_sync, index, filter_value, count)

    def _query_sync(self, index: str, filter_value: str, count: int) -> QueryResult:
        client = self._require_client()
        start = time.time()
        query = client.query(NAMESPACE, PRODUCT_SET)
        query.where(p.equals(index, filter_value))
        query.select("id", "name", "images", "brandName")
        query.max_records = count
        products = []
        for _key, _meta, bins in query.results():
            products.append(Product.from_bins(bins))
        elapsed = int((time.time() - start) * 1000)
        return QueryResult(products=products, time_ms=elapsed)

    async def get_categories(self) -> list[str]:
        return await asyncio.to_thread(self._get_categories_sync)

    def _get_categories_sync(self) -> list[str]:
        client = self._require_client()
        key = (NAMESPACE, CATEGORY_SET, CATEGORY_KEY)
        map_policy = {
            "map_order": aerospike.MAP_KEY_ORDERED,
        }
        ops = [
            map_operations.map_get_by_key_range(
                "categories", "A", "Z", aerospike.MAP_RETURN_KEY, map_policy
            )
        ]
        _key, _meta, bins = client.operate(key, ops)
        categories = bins.get("categories", [])
        return list(categories) if categories else []

    async def get_article_types(self) -> list[str]:
        return await self._get_category_part("articleTypes")

    async def get_usage(self) -> list[str]:
        values = await self._get_category_part("usage")
        return [v for v in values if v and v != "NA"]

    async def get_brand_names(self) -> list[str]:
        values = await self._get_category_part("brandNames")
        return [v for v in values if v and v != "NA"]

    async def _get_category_part(self, bin_name: str) -> list[str]:
        return await asyncio.to_thread(self._get_category_part_sync, bin_name)

    def _get_category_part_sync(self, bin_name: str) -> list[str]:
        client = self._require_client()
        key = (NAMESPACE, CATEGORY_SET, CATEGORY_KEY)
        try:
            _key, _meta, bins = client.select(key, [bin_name])
            values = bins.get(bin_name, [])
            return list(values) if values else []
        except aerospike.exception.RecordNotFound:
            return []

    async def load_categories(
        self,
        category: str,
        sub_category: str,
        article_type: str,
        usage: str,
        brand_name: str,
    ) -> None:
        await asyncio.to_thread(
            self._load_categories_sync,
            category,
            sub_category,
            article_type,
            usage,
            brand_name,
        )

    def _load_categories_sync(
        self,
        category: str,
        sub_category: str,
        article_type: str,
        usage: str,
        brand_name: str,
    ) -> None:
        client = self._require_client()
        key = (NAMESPACE, CATEGORY_SET, CATEGORY_KEY)
        map_policy = {
            "map_write_flags": aerospike.MAP_WRITE_FLAGS_CREATE_ONLY
            | aerospike.MAP_WRITE_FLAGS_NO_FAIL,
            "map_order": aerospike.MAP_KEY_ORDERED,
        }
        list_policy = {
            "write_flags": aerospike.LIST_WRITE_ADD_UNIQUE
            | aerospike.LIST_WRITE_NO_FAIL,
            "list_order": aerospike.LIST_ORDERED,
        }
        ops = [
            map_operations.map_put("categories", category, {}, map_policy),
            map_operations.map_increment(
                "categories",
                sub_category,
                1,
                map_policy,
                ctx=[cdt_ctx.cdt_ctx_map_key(category)],
            ),
            list_operations.list_append("articleTypes", article_type, list_policy),
            list_operations.list_append("usage", usage, list_policy),
            list_operations.list_append("brandNames", brand_name, list_policy),
        ]
        client.operate(key, ops)

    async def create_string_index(self, bin_name: str, index_name: str) -> None:
        await asyncio.to_thread(self._create_string_index_sync, bin_name, index_name)

    def _create_string_index_sync(self, bin_name: str, index_name: str) -> None:
        client = self._require_client()
        try:
            client.index_string_create(NAMESPACE, PRODUCT_SET, bin_name, index_name)
        except aerospike.exception.IndexFoundError:
            pass
        except Exception as exc:
            print(f"Index {index_name} already exists or failed to create: {exc}")

    async def advanced_search(
        self,
        category: str | None,
        article_type: str | None,
        usage: str | None,
        brand_name: str | None,
        search_text: str | None,
        count: int,
    ) -> QueryResult:
        return await asyncio.to_thread(
            self._advanced_search_sync,
            category,
            article_type,
            usage,
            brand_name,
            search_text,
            count,
        )

    def _advanced_search_sync(
        self,
        category: str | None,
        article_type: str | None,
        usage: str | None,
        brand_name: str | None,
        search_text: str | None,
        count: int,
    ) -> QueryResult:
        del search_text  # Not used in Java implementation either
        client = self._require_client()
        start = time.time()
        indexes = {
            "category": as_non_null_string(category),
            "articleType": as_non_null_string(article_type),
            "usage": as_non_null_string(usage),
            "brandName": as_non_null_string(brand_name),
        }
        from aerospike_helpers import expressions as exp_helpers

        index_field = None
        exp_parts: list[Any] = []
        for field, value in indexes.items():
            if value:
                if index_field is None:
                    index_field = field
                else:
                    exp_parts.append(
                        exp_helpers.Eq(exp_helpers.StrBin(field), exp_helpers.Val(value))
                    )
        exp_parts.extend([exp_helpers.Val(True), exp_helpers.Val(True)])
        filter_exp = exp_helpers.And(*exp_parts)

        query = client.query(NAMESPACE, PRODUCT_SET)
        if index_field is not None:
            query.where(p.equals(index_field, indexes[index_field]))
        query.select("id", "name", "images", "brandName")
        query.max_records = count
        query.set_options({"filterexp": filter_exp})

        products = []
        for _key, _meta, bins in query.results():
            products.append(Product.from_bins(bins))
        elapsed = int((time.time() - start) * 1000)
        return QueryResult(products=products, time_ms=elapsed)

    async def get_product_count(self) -> int:
        return await asyncio.to_thread(self._get_product_count_sync)

    def _get_product_count_sync(self) -> int:
        client = self._require_client()
        query = client.query(NAMESPACE, PRODUCT_SET)
        query.select()
        count = 0
        for _ in query.results():
            count += 1
        return count

    async def get_cart(self, user_id: str) -> Cart:
        try:
            return await asyncio.to_thread(self._get_cart_sync, user_id)
        except Exception as exc:
            print(f"Error getting cart: {exc}")
            return Cart()

    def _get_cart_sync(self, user_id: str) -> Cart:
        client = self._require_client()
        key = (NAMESPACE, CARTS_SET, user_id)
        try:
            _key, _meta, bins = client.get(key)
            return Cart.from_bins(bins)
        except aerospike.exception.RecordNotFound:
            return Cart()

    async def add_to_cart(self, user_id: str, product_id: str, quantity: int) -> Cart:
        try:
            product = await self.get_product(product_id)
            if product is None:
                raise RuntimeError(f"Product not found: {product_id}")
            return await asyncio.to_thread(
                self._add_to_cart_sync, user_id, product_id, quantity, product
            )
        except Exception as exc:
            print(f"Error adding to cart: {exc}")
            raise RuntimeError(f"Failed to add item to cart: {exc}") from exc

    def _add_to_cart_sync(
        self, user_id: str, product_id: str, quantity: int, product: Product
    ) -> Cart:
        client = self._require_client()
        key = (NAMESPACE, CARTS_SET, user_id)
        image = extract_product_image(product.images)
        map_policy = {
            "map_order": aerospike.MAP_KEY_ORDERED,
        }

        while True:
            try:
                existing_record = None
                try:
                    existing_record = client.get(key)
                except aerospike.exception.RecordNotFound:
                    pass

                write_policy: dict[str, Any] = {}
                cart: Cart | None = None
                existing = False

                if existing_record is not None:
                    _k, meta, bins = existing_record
                    write_policy = {"gen": meta["gen"], "gen_eq": True}
                    cart = Cart.from_bins(bins)
                    item = cart.find_item(product_id)
                    if item is not None:
                        existing = True
                        item.quantity += quantity
                    else:
                        cart.add(CartItem.from_product(user_id, quantity, image, product))
                else:
                    write_policy = {"exists": aerospike.POLICY_EXISTS_CREATE}
                    cart = Cart()
                    cart.add(CartItem.from_product(user_id, quantity, image, product))

                if existing:
                    ops = [
                        map_operations.map_increment(
                            ITEMS_BIN,
                            "quantity",
                            quantity,
                            map_policy,
                            ctx=[cdt_ctx.cdt_ctx_map_key(product_id)],
                        )
                    ]
                else:
                    new_item = cart.find_item(product_id)
                    assert new_item is not None
                    ops = [
                        map_operations.map_put(
                            ITEMS_BIN,
                            product_id,
                            new_item.to_bins(),
                            map_policy,
                        )
                    ]
                client.operate(key, ops, meta=write_policy)
                return cart
            except aerospike.exception.RecordGenerationError:
                continue

    async def update_cart_item(
        self, user_id: str, product_id: str, quantity: int
    ) -> Cart:
        try:
            return await asyncio.to_thread(
                self._update_cart_item_sync, user_id, product_id, quantity
            )
        except Exception as exc:
            print(f"Error updating cart item: {exc}")
            raise RuntimeError(f"Failed to update cart item: {exc}") from exc

    def _update_cart_item_sync(
        self, user_id: str, product_id: str, quantity: int
    ) -> Cart:
        client = self._require_client()
        key = (NAMESPACE, CARTS_SET, user_id)
        try:
            _k, _meta, bins = client.get(key)
        except aerospike.exception.RecordNotFound:
            return Cart()

        cart = Cart.from_bins(bins)
        map_policy = {"map_order": aerospike.MAP_KEY_ORDERED}

        if quantity <= 0:
            cart.remove(product_id)
            ops = [
                map_operations.map_remove_by_key(
                    ITEMS_BIN, product_id, aerospike.MAP_RETURN_NONE
                )
            ]
            client.operate(key, ops)
        else:
            item = cart.find_item(product_id)
            if item is not None:
                item.quantity = quantity
                ops = [
                    map_operations.map_put(
                        ITEMS_BIN,
                        "quantity",
                        quantity,
                        map_policy,
                        ctx=[cdt_ctx.cdt_ctx_map_key(product_id)],
                    )
                ]
                client.operate(key, ops)
        return cart

    async def remove_from_cart(self, user_id: str, product_id: str) -> Cart:
        return await self.update_cart_item(user_id, product_id, 0)

    async def clear_cart(self, user_id: str) -> Cart:
        try:
            await asyncio.to_thread(self._clear_cart_sync, user_id)
            return Cart()
        except Exception as exc:
            print(f"Error clearing cart: {exc}")
            raise RuntimeError(f"Failed to clear cart: {exc}") from exc

    def _clear_cart_sync(self, user_id: str) -> None:
        client = self._require_client()
        key = (NAMESPACE, CARTS_SET, user_id)
        ops = [map_operations.map_clear(ITEMS_BIN)]
        try:
            client.operate(key, ops)
        except aerospike.exception.RecordNotFound:
            pass
