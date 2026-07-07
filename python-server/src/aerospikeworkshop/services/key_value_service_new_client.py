"""New Python SDK workshop skeleton — mirrors KeyValueServiceNewClient.java."""

from __future__ import annotations

import logging

from aerospike_sdk import Behavior, ClusterDefinition, DataSet

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


class KeyValueServiceNewClient:
    """Active when AEROSPIKE_CLIENT_PROFILE=new-client."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._cluster = None
        self._session = None
        self.product_dataset = DataSet.of(NAMESPACE, PRODUCT_SET)
        self.cart_dataset = DataSet.of(NAMESPACE, CARTS_SET)
        self.category_dataset = DataSet.of(NAMESPACE, CATEGORY_SET)

    async def connect(self) -> None:
        # =================================================================================
        # TODO: STEP 1: VALIDATE THE CONNECTION
        # =================================================================================
        cluster_def = ClusterDefinition(
            self._settings.aerospike_host, self._settings.aerospike_port
        )
        if self._settings.aerospike_username:
            cluster_def = cluster_def.with_native_credentials(
                self._settings.aerospike_username,
                self._settings.aerospike_password or "",
            )
        self._cluster = await cluster_def.connect()
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
        # =================================================================================
        # TODO: STEP 2: STORE A PRODUCT OBJECT
        # =================================================================================
        # Implement the logic to store a Product in the database.
        #
        # This task tests dict-based writes. Your goal is to:
        #  - Use the session to insert into product_dataset with the product id.
        #  - Pass product.to_bins() to the put() call.
        #  - Execute the operation.
        #  - The insert should fail if the record already exists.
        # =================================================================================
        pass

    async def get_product(self, product_id: str) -> Product | None:
        # =================================================================================
        # TODO: STEP 3: GET A PRODUCT BY ID
        # =================================================================================
        # Implement the logic to fetch a single product by its product_id.
        #
        # This is a key-value lookup. Your goal is to:
        #  - Query product_dataset using .id(product_id).
        #  - Execute the query and get the first result.
        #  - Convert record.bins into a Product using Product.from_bins().
        # =================================================================================
        return None

    async def query(self, index: str, filter_value: str, count: int) -> QueryResult:
        import time

        start_time = time.time()

        # =================================================================================
        # TODO: STEP 4: QUERY FOR PRODUCTS
        # =================================================================================
        # Implement the logic to query for a list of products.
        #
        # Refer to guide-to-python-sdk.md. Your goal is to:
        #  - Query product_dataset.
        #  - Filter using .where() with index and filter_value (e.g. $.subCategory == 'Shoes').
        #  - Limit results to count.
        #  - Project bins: id, name, images, brandName, price.
        #  - Convert results to a list of Product objects.
        # =================================================================================
        product = await self.get_product("13283")
        products = [product] if product else []

        return QueryResult(
            products=products, time_ms=int((time.time() - start_time) * 1000)
        )

    async def advanced_search(
        self,
        category: str | None,
        article_type: str | None,
        usage: str | None,
        brand_name: str | None,
        search_text: str | None,
        count: int,
    ) -> QueryResult:
        import time

        start_time = time.time()
        del search_text

        indexes = {
            "category": as_non_null_string(category),
            "articleType": as_non_null_string(article_type),
            "usage": as_non_null_string(usage),
            "brandName": as_non_null_string(brand_name),
        }

        ael = ""
        for field, value in indexes.items():
            if value:
                if ael:
                    ael += " and "
                ael += f"$.{field} == '{value}'"
        print(f"AEL: {ael}")

        # =================================================================================
        # TODO: STEP 5: EXECUTE THE ADVANCED SEARCH
        # =================================================================================
        # The AEL query string has been built above. Now execute the query.
        #
        # Your goal is to:
        #  - Query product_dataset with .where(ael).
        #  - Limit results to count.
        #  - Convert record bins to Product objects.
        # =================================================================================
        product = await self.get_product("13283")
        products = [product] if product else []

        return QueryResult(
            products=products, time_ms=int((time.time() - start_time) * 1000)
        )

    async def get_cart(self, user_id: str) -> Cart:
        try:
            # =================================================================================
            # TODO: STEP 6: GET THE CART OBJECT
            # =================================================================================
            # Implement the logic to query a Cart object by user_id.
            #
            # Your goal is to:
            #  - Query cart_dataset.id(user_id).
            #  - Execute and return the first cart, or an empty Cart if not found.
            # =================================================================================
            product = await self.get_product("13283")
            if product is None:
                return Cart()
            return Cart(
                items={
                    "13283": CartItem.from_product(
                        user_id,
                        2,
                        "http://assets.myntassets.com/v1/images/"
                        "style/properties/3c03eb9654656c19f467f85b7de928f5_images.jpg",
                        product,
                    )
                }
            )
        except Exception as exc:
            logger.exception("Error getting cart: %s", exc)
            return Cart()

    async def add_to_cart(self, user_id: str, product_id: str, quantity: int) -> Cart:
        from aerospike_sdk.exceptions import GenerationError

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
                    # =================================================================================
                    # TODO: STEP 7: UPDATE THE CART
                    # =================================================================================
                    # Step 7a: Retrieve the user's cart with generation metadata.
                    # Step 7b: If item exists, increment quantity with ensure_generation_is().
                    # Step 7c: If cart missing, insert new record with the cart item.
                    # =================================================================================
                    cart = await self.get_cart(user_id)
                    generation = 1

                    if cart.find_item(product_id) is not None:
                        item = cart.find_item(product_id)
                        assert item is not None
                        item.quantity += quantity
                        # TODO STEP 7b: update quantity in Aerospike with generation check
                    else:
                        new_item = CartItem.from_product(user_id, quantity, image, product)
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
                except GenerationError:
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
        info = session.info()
        details = await info.namespace_details(NAMESPACE)
        if not details:
            return 0
        replication_factor = int(details.get("effective_replication_factor", 1))
        sets_info = await info.info(f"sets/{NAMESPACE}")
        for _key, value in sets_info.items():
            for entry in str(value).split(";"):
                if entry.startswith(f"{PRODUCT_SET}:"):
                    parts = dict(
                        token.split("=", 1)
                        for token in entry.split(":")[1].split(",")
                        if "=" in token
                    )
                    objects = int(parts.get("objects", 0))
                    return objects // replication_factor
        return 0

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
            .on_map_key(category)
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
