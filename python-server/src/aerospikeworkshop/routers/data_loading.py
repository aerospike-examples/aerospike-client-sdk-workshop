"""Data loading API endpoints — mirrors DataLoadingController.java."""

from __future__ import annotations

import time
from pathlib import Path

from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

router = APIRouter(prefix="/rest/v1/data", tags=["data"])


@router.get("/health")
async def health() -> dict:
    return {
        "status": "OK",
        "service": "Data Loading Service",
        "message": "Data loading service is operational",
        "timestamp": int(time.time() * 1000),
    }


@router.post("/load")
async def load_data(request: Request, dataPath: str):
    path = Path(dataPath)
    if not path.is_dir():
        return JSONResponse(
            status_code=400,
            content={
                "error": f"Invalid data path: {dataPath}",
                "message": "Path does not exist or is not a directory",
            },
        )
    if not (path / "styles").is_dir():
        return JSONResponse(
            status_code=400,
            content={
                "error": "Missing styles directory",
                "message": "The data path must contain a 'styles' subdirectory with JSON files",
            },
        )
    try:
        result = await request.app.state.data_loading_service.load_all_data(dataPath)
        return {
            "success": True,
            "totalFiles": result.total_files,
            "successCount": result.success_count,
            "errorCount": result.error_count,
            "productsStored": result.products_stored,
            "successRate": result.success_rate,
            "message": "Data loading completed",
        }
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={
                "error": "Unexpected error during data loading",
                "message": str(exc),
            },
        )


@router.post("/load-single")
async def load_single_product(request: Request, filePath: str):
    path = Path(filePath)
    if not path.is_file():
        return JSONResponse(
            status_code=400,
            content={
                "error": f"Invalid file path: {filePath}",
                "message": "File does not exist or is not a regular file",
            },
        )
    try:
        await request.app.state.data_loading_service.load_single_product(filePath)
        return {
            "success": True,
            "filePath": filePath,
            "message": "Product loaded successfully",
        }
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={
                "error": "Unexpected error loading product",
                "message": str(exc),
                "filePath": filePath,
            },
        )


@router.get("/count")
async def get_product_count(request: Request):
    try:
        count = await request.app.state.data_loading_service.get_product_count()
        return {
            "productCount": count,
            "message": "Product count retrieved successfully",
        }
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={
                "error": "Error retrieving product count",
                "message": str(exc),
            },
        )


@router.delete("/clear")
async def clear_all_data(request: Request, confirm: str):
    if confirm != "yes-delete-all":
        return JSONResponse(
            status_code=400,
            content={
                "error": "Missing or invalid confirmation",
                "message": "To clear all data, provide confirm=yes-delete-all parameter",
            },
        )
    try:
        await request.app.state.data_loading_service.clear_all_data()
        return {"success": True, "message": "All product data cleared"}
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={"error": "Error clearing data", "message": str(exc)},
        )


@router.post("/create-indexes")
async def create_secondary_indexes(request: Request):
    indexes = {
        "category": "cat_idx",
        "subCategory": "subCat_idx",
        "usage": "usage_idx",
        "brandName": "brand_idx",
        "articleType": "article_idx",
    }
    results: dict[str, str] = {}
    success_count = 0
    error_count = 0
    for bin_name, index_name in indexes.items():
        try:
            await request.app.state.data_loading_service.create_single_index(
                bin_name, index_name
            )
            results[index_name] = "Created successfully"
            success_count += 1
        except Exception as exc:
            results[index_name] = f"Error: {exc}"
            error_count += 1
    return {
        "success": error_count == 0,
        "totalIndexes": len(indexes),
        "successCount": success_count,
        "errorCount": error_count,
        "indexResults": results,
        "message": "Secondary index creation completed",
    }
