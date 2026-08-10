"""Data loading service — mirrors DataLoadingService.java."""

from __future__ import annotations

from dataclasses import dataclass

from aerospikeworkshop.json_parsing_service import JsonParsingService
from aerospikeworkshop.models.product import Product
from aerospikeworkshop.services.key_value_service import KeyValueService


@dataclass
class LoadResult:
    success_count: int
    error_count: int
    total_files: int
    products_stored: int = 0

    @property
    def success_rate(self) -> float:
        if self.total_files == 0:
            return 0.0
        return self.success_count / self.total_files * 100

    def __str__(self) -> str:
        summary = (
            f"processed {self.success_count}/{self.total_files} sample files "
            f"({self.error_count} errors); {self.products_stored} products in products set"
        )
        if self.success_count > 0 and self.products_stored == 0:
            summary += "; category metadata updated in cat_index set"
        return summary


class DataLoadingService:
    def __init__(
        self,
        json_parsing_service: JsonParsingService,
        key_value_service: KeyValueService,
    ) -> None:
        self._json = json_parsing_service
        self._kv = key_value_service

    async def load_all_data(self, data_root_path: str) -> LoadResult:
        await self._create_secondary_indexes()
        json_files = self._json.get_style_files(data_root_path)
        success_count = 0
        error_count = 0
        for file_path in json_files:
            try:
                await self.load_single_product(file_path)
                success_count += 1
            except Exception as exc:
                error_count += 1
                print(f"Error loading file {file_path}: {exc}")
        products_stored = await self.get_product_count()
        return LoadResult(
            success_count, error_count, len(json_files), products_stored
        )

    async def load_single_product(self, file_path: str) -> None:
        raw_data = self._json.parse_product_file(file_path)
        product_id = self._json.extract_product_id(file_path)
        product_map = self._json.format_product_data(raw_data, product_id)
        await self._load_product_categories(product_map)
        await self._kv.store_product(Product.from_bins(product_map))

    async def _create_secondary_indexes(self) -> None:
        indexes = {
            "category": "cat_idx",
            "subCategory": "subCat_idx",
            "articleType": "articleType_idx",
            "usage": "usage_idx",
            "brandName": "brand_idx",
        }
        for bin_name, index_name in indexes.items():
            await self._kv.create_string_index(bin_name, index_name)

    async def _load_product_categories(self, product: dict) -> None:
        category = product.get("category")
        sub_category = product.get("subCategory")
        article_type = product.get("articleType")
        usage = product.get("usage")
        brand_name = product.get("brandName")
        if all([category, sub_category, article_type, usage, brand_name]):
            await self._kv.load_categories(
                str(category),
                str(sub_category),
                str(article_type),
                str(usage),
                str(brand_name),
            )

    async def get_product_count(self) -> int:
        return await self._kv.get_product_count()

    async def create_single_index(self, bin_name: str, index_name: str) -> None:
        await self._kv.create_string_index(bin_name, index_name)

    async def clear_all_data(self) -> None:
        await self._kv.clear_all_data()
