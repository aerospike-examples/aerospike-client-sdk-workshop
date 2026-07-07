"""Startup data initialization — mirrors DataInitializer.java."""

from __future__ import annotations

import logging
from pathlib import Path

from aerospikeworkshop.data_loading_service import DataLoadingService

logger = logging.getLogger(__name__)


async def run_startup(data_loading_service: DataLoadingService) -> None:
    try:
        await _create_secondary_indexes(data_loading_service)

        existing_count = await data_loading_service.get_product_count()
        if existing_count > 0:
            logger.info(
                "Database already contains %s products, skipping auto-load",
                existing_count,
            )
            return

        data_path = _resolve_data_path()
        logger.info("Auto-loading sample data from %s", data_path)
        result = await data_loading_service.load_all_data(str(data_path))
        logger.info("Auto-load complete: %s", result)
    except Exception as exc:
        logger.warning("Auto-load failed (database may not be ready): %s", exc)
        logger.info(
            "You can manually load data via: POST /rest/v1/data/load?dataPath=<path>"
        )


async def _create_secondary_indexes(data_loading_service: DataLoadingService) -> None:
    indexes = {
        "category": "cat_idx",
        "subCategory": "subCat_idx",
        "usage": "usage_idx",
        "brandName": "brand_idx",
        "articleType": "article_idx",
    }
    for bin_name, index_name in indexes.items():
        try:
            await data_loading_service.create_single_index(bin_name, index_name)
            logger.info("Created secondary index: %s on bin %s", index_name, bin_name)
        except Exception as exc:
            logger.debug("Index %s may already exist: %s", index_name, exc)


def _resolve_data_path() -> Path:
    relative = Path("../data")
    if (relative / "styles").is_dir():
        return relative.resolve()
    from_root = Path("data")
    if (from_root / "styles").is_dir():
        return from_root.resolve()
    return relative.resolve()
