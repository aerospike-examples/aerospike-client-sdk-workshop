"""Retail API endpoints — mirrors RetailController.java."""

from __future__ import annotations

import time

from fastapi import APIRouter, Request

router = APIRouter(prefix="/rest/v1", tags=["retail"])


def _product_dict(product) -> dict:
    return product.model_dump(by_alias=True, mode="json")


@router.get("/health")
async def health() -> dict:
    return {
        "status": "OK",
        "message": "FastAPI server is running",
        "timestamp": int(time.time() * 1000),
    }


@router.get("/home")
async def get_home(request: Request) -> dict:
    kv = request.app.state.kv_service
    shoes = await kv.query("subCategory", "Shoes", 10)
    bags = await kv.query("subCategory", "Bags", 10)
    wallets = await kv.query("subCategory", "Wallets", 10)
    watches = await kv.query("subCategory", "Watches", 10)
    headwear = await kv.query("subCategory", "Headwear", 10)
    return {
        "Shoes": [_product_dict(p) for p in shoes.products],
        "Bags": [_product_dict(p) for p in bags.products],
        "Wallets": [_product_dict(p) for p in wallets.products],
        "Watches": [_product_dict(p) for p in watches.products],
        "Headwear": [_product_dict(p) for p in headwear.products],
    }


@router.get("/get")
async def get_product(request: Request, prod: str) -> dict:
    kv = request.app.state.kv_service
    product = await kv.get_product(prod)
    if product is None:
        return {"error": "Product not found"}
    return {
        "error": None,
        "product": _product_dict(product),
        "related": [],
        "also_bought": [],
    }


@router.get("/search")
async def search(
    request: Request,
    q: str | None = None,
    category: str | None = None,
    articleType: str | None = None,
    usage: str | None = None,
    brandName: str | None = None,
) -> dict:
    kv = request.app.state.kv_service
    result = await kv.advanced_search(category, articleType, usage, brandName, q, 20)
    return {
        "products": [_product_dict(p) for p in result.products],
        "time": result.time_ms,
    }


@router.get("/categories")
async def get_categories(request: Request) -> dict:
    categories = await request.app.state.kv_service.get_categories()
    return {"categories": categories, "count": len(categories)}


@router.get("/article-types")
async def get_article_types(request: Request) -> dict:
    article_types = await request.app.state.kv_service.get_article_types()
    return {"articleTypes": article_types, "count": len(article_types)}


@router.get("/usage-types")
async def get_usage_types(request: Request) -> dict:
    usage_types = await request.app.state.kv_service.get_usage()
    return {"usageTypes": usage_types, "count": len(usage_types)}


@router.get("/brand-names")
async def get_brand_names(request: Request) -> dict:
    brand_names = await request.app.state.kv_service.get_brand_names()
    return {"brandNames": brand_names, "count": len(brand_names)}


@router.get("/category")
async def get_category(request: Request, idx: str, filter_value: str) -> dict:
    kv = request.app.state.kv_service
    result = await kv.query(idx, filter_value, 20)
    return {
        "products": [_product_dict(p) for p in result.products],
        "time": result.time_ms,
    }
