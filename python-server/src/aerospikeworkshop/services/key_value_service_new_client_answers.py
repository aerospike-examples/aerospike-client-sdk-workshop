"""New Python SDK solution — mirrors KeyValueServiceNewClientAnswers.java."""

from __future__ import annotations

import logging
import time

from aerospike_async import MapOrder
from aerospike_sdk import Behavior, ClusterDefinition, DataSet
from aerospike_sdk.exceptions import GenerationError

from aerospikeworkshop.config import Settings
from aerospikeworkshop.models.cart import Cart
from aerospikeworkshop.models.cart_item import CartItem
from aerospikeworkshop.models.product import Product
from aerospikeworkshop.services.key_value_service import QueryResult
from aerospikeworkshop.services.utils import as_non_null_string, extract_product_image

logger = logging.getLogger(__name__)

ITEMS_BIN = "items"
NAMESPACE = "test"
PRODUCT_SET = "products"
CARTS_SET = "shopping_carts"
CATEGORY_SET = "cat_index"
CATEGORY_KEY = "product_meta"


class KeyValueServiceNewClientAnswers:
    """Active when AEROSPIKE_CLIENT_PROFILE=new-client-answers."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._cluster = None
        self._session = None
        # DataSet = namespace + set; use .id(key) to address a single record.
        self.product_dataset = DataSet.of(NAMESPACE, PRODUCT_SET)
        self.cart_dataset = DataSet.of(NAMESPACE, CARTS_SET)
        self.category_dataset = DataSet.of(NAMESPACE, CATEGORY_SET)

    async def connect(self) -> None:
        # STEP 1: open a cluster connection, then create a session for all reads/writes.
        cluster_def = ClusterDefinition(
            self._settings.aerospike_host, self._settings.aerospike_port
        )
        if self._settings.aerospike_username:
            cluster_def = cluster_def.with_native_credentials(
                self._settings.aerospike_username,
                self._settings.aerospike_password or "",
            )
        self._cluster = await cluster_def.connect()
        # Behavior.DEFAULT supplies sensible timeouts and retry policies.
        self._session = self._cluster.create_session(Behavior.DEFAULT)

    async def close(self) -> None:
        if self._cluster is not None:
            await self._cluster.close()
            self._cluster = None
            self._session = None

    def _require_session(self):
        if self._session is None:
            raise RuntimeError("Aerospike session is not connected")
        return self._session

    async def store_product(self, product: Product) -> None:
        # STEP 2: insert fails if the key already exists (unlike upsert).
        session = self._require_session()
        await (
            session.insert(self.product_dataset.id(product.id))
            .put(product.to_bins())  # bins = column-like fields on the record
            .execute()
        )

    async def get_product(self, product_id: str) -> Product | None:
        # STEP 3: point read — query a single key, not a set scan.
        session = self._require_session()
        stream = await session.query(self.product_dataset.id(product_id)).execute()
        result = await stream.first()
        stream.close()  # always close streams when done
        if result is None or not result.is_ok or result.record is None:
            return None
        return Product.from_bins(result.record.bins)

    async def query(self, index: str, filter_value: str, count: int) -> QueryResult:
        # STEP 4: filter the set with AEL; Aerospike picks a secondary index when possible.
        session = self._require_session()
        start_time = time.time()
        stream = await (
            session.query(self.product_dataset)
            .where(f"$.{index} == '{filter_value}'")  # $.binName references a bin value
            .bins(["id", "name", "images", "brandName", "price"])  # project only needed bins
            .limit(count)
            .execute()
        )
        products: list[Product] = []
        async for row in stream:
            if row.is_ok and row.record is not None:
                products.append(Product.from_bins(row.record.bins))
        stream.close()
        return QueryResult(products=products, time_ms=int((time.time() - start_time) * 1000))

    async def advanced_search(
        self,
        category: str | None,
        article_type: str | None,
        usage: str | None,
        brand_name: str | None,
        search_text: str | None,
        count: int,
    ) -> QueryResult:
        del search_text
        session = self._require_session()
        start_time = time.time()

        indexes = {
            "category": as_non_null_string(category),
            "articleType": as_non_null_string(article_type),
            "usage": as_non_null_string(usage),
            "brandName": as_non_null_string(brand_name),
        }

        # STEP 5: combine multiple filters into one AEL expression.
        ael = ""
        for field, value in indexes.items():
            if value:
                if ael:
                    ael += " and "
                ael += f"$.{field} == '{value}'"
        print(f"AEL: {ael}")

        stream = await (
            session.query(self.product_dataset).where(ael).limit(count).execute()
        )
        products: list[Product] = []
        async for row in stream:
            if row.is_ok and row.record is not None:
                products.append(Product.from_bins(row.record.bins))
        stream.close()
        return QueryResult(products=products, time_ms=int((time.time() - start_time) * 1000))

    async def get_cart(self, user_id: str) -> Cart:
        # STEP 6: one cart record per user; cart items live in the ITEMS_BIN map.
        try:
            session = self._require_session()
            stream = await session.query(self.cart_dataset.id(user_id)).execute()
            result = await stream.first()
            stream.close()
            if result is None or not result.is_ok or result.record is None:
                return Cart()
            return Cart.from_bins(result.record.bins)
        except Exception as exc:
            logger.exception("Error getting cart: %s", exc)
            return Cart()

    async def add_to_cart(self, user_id: str, product_id: str, quantity: int) -> Cart:
        # STEP 7: cart updates use CDT map ops inside ITEMS_BIN, keyed by product_id.
        # generation checks prevent lost updates when two requests modify the same cart.
        try:
            product = await self.get_product(product_id)
            if product is None:
                raise RuntimeError(f"Product not found: {product_id}")

            session = self._require_session()
            key = self.cart_dataset.id(user_id)
            image = extract_product_image(product.images)
            result_cart = None

            while result_cart is None:
                try:
                    stream = await session.query(key).execute()
                    result = await stream.first()
                    stream.close()

                    if result is not None and result.is_ok and result.record is not None:
                        cart = Cart.from_bins(result.record.bins)
                        generation = result.record.generation  # needed for ensure_generation_is
                        existing = cart.find_item(product_id)
                        if existing is not None:
                            existing.quantity += quantity
                            await (
                                session.update(key)
                                .bin(ITEMS_BIN)
                                .on_map_key(product_id)       # navigate into the items map
                                .on_map_key("quantity")       # then into the item's quantity field
                                .add(quantity)
                                .ensure_generation_is(generation)  # fail if cart changed since read
                                .execute()
                            )
                        else:
                            new_item = CartItem.from_product(
                                user_id, quantity, image, product
                            )
                            cart.add(new_item)
                            await (
                                session.update(key)
                                .bin(ITEMS_BIN)
                                .on_map_key(product_id)
                                .set_to(new_item.to_bins())
                                .ensure_generation_is(generation)
                                .execute()
                            )
                        result_cart = cart
                    else:
                        # no cart record yet — insert creates the record and first map entry
                        cart = Cart()
                        new_item = CartItem.from_product(
                            user_id, quantity, image, product
                        )
                        cart.add(new_item)
                        await (
                            session.insert(key)
                            .bin(ITEMS_BIN)
                            .on_map_key(product_id)
                            .set_to(new_item.to_bins())
                            .execute()
                        )
                        result_cart = cart
                except GenerationError:
                    # another request won the race; re-read and retry
                    logger.info("Lost race condition when adding product %s", product_id)

            return result_cart
        except Exception as exc:
            logger.exception("Error adding to cart: %s", exc)
            raise RuntimeError(f"Failed to add item to cart: {exc}") from exc

    async def clear_cart(self, user_id: str) -> Cart:
        try:
            session = self._require_session()
            await (
                session.upsert(self.cart_dataset.id(user_id))
                .bin(ITEMS_BIN)
                .map_clear()
                .execute()
            )
            return Cart()
        except Exception as exc:
            logger.exception("Error clearing cart: %s", exc)
            raise RuntimeError(f"Failed to clear cart: {exc}") from exc

    async def update_cart_item(
        self, user_id: str, product_id: str, quantity: int
    ) -> Cart:
        try:
            session = self._require_session()
            key = self.cart_dataset.id(user_id)
            stream = await session.query(key).execute()
            result = await stream.first()
            stream.close()
            if result is None or not result.is_ok or result.record is None:
                return Cart()

            cart = Cart.from_bins(result.record.bins)
            if quantity <= 0:
                await (
                    session.update(key)
                    .bin(ITEMS_BIN)
                    .on_map_key(product_id)
                    .remove()
                    .execute()
                )
                cart.remove(product_id)
                return cart

            item = cart.find_item(product_id)
            if item is not None:
                item.quantity = quantity
                await (
                    session.update(key)
                    .bin(ITEMS_BIN)
                    .on_map_key(product_id)
                    .on_map_key("quantity")
                    .set_to(quantity)
                    .execute()
                )
            return cart
        except Exception as exc:
            logger.exception("Error updating cart item: %s", exc)
            raise RuntimeError(f"Failed to update cart item: {exc}") from exc

    async def remove_from_cart(self, user_id: str, product_id: str) -> Cart:
        return await self.update_cart_item(user_id, product_id, 0)

    async def get_product_count(self) -> int:
        session = self._require_session()
        stream = await session.query(self.product_dataset).with_no_bins().execute()
        count = 0
        async for _row in stream:
            count += 1
        stream.close()
        return count

    async def clear_all_data(self) -> None:
        session = self._require_session()
        await session.truncate(self.cart_dataset)
        await session.truncate(self.product_dataset)
        await session.truncate(self.category_dataset)

    async def get_categories(self) -> list[str]:
        session = self._require_session()
        stream = await (
            session.upsert(self.category_dataset.id(CATEGORY_KEY))
            .bin("categories")
            .on_map_key_range("A", "Z")
            .get_keys()
            .execute()
        )
        result = await stream.first()
        stream.close()
        if result is None or result.record is None:
            return []
        categories = result.record.bins.get("categories", [])
        return list(categories) if categories else []

    async def _get_category_part(self, bin_name: str) -> list[str]:
        session = self._require_session()
        stream = await (
            session.query(self.category_dataset.id(CATEGORY_KEY))
            .bins([bin_name])
            .execute()
        )
        result = await stream.first()
        stream.close()
        if result is None or result.record is None:
            return []
        values = result.record.bins.get(bin_name, [])
        return [v for v in values if v and v != "NA"]

    async def get_article_types(self) -> list[str]:
        return await self._get_category_part("articleTypes")

    async def get_usage(self) -> list[str]:
        return await self._get_category_part("usage")

    async def get_brand_names(self) -> list[str]:
        return await self._get_category_part("brandNames")

    async def load_categories(
        self,
        category: str,
        sub_category: str,
        article_type: str,
        usage: str,
        brand_name: str,
    ) -> None:
        session = self._require_session()
        await (
            session.upsert(self.category_dataset.id(CATEGORY_KEY))
            .bin("categories")
            .on_map_key(category, create_type=MapOrder.KEY_ORDERED)
            .on_map_key(sub_category)
            .add(1)
            .bin("articleTypes")
            .list_append(article_type, unique=True, no_fail=True)
            .bin("usage")
            .list_append(usage, unique=True, no_fail=True)
            .bin("brandNames")
            .list_append(brand_name, unique=True, no_fail=True)
            .execute()
        )

    async def create_string_index(self, bin_name: str, index_name: str) -> None:
        session = self._require_session()
        try:
            await (
                session.index(dataset=self.product_dataset)
                .on_bin(bin_name)
                .named(index_name)
                .string()
                .create()
            )
        except Exception as exc:
            print(f"Index {index_name} already exists or failed to create: {exc}")
