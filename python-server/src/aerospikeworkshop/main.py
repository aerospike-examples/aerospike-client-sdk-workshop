"""FastAPI application entry point."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from pathlib import Path

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from aerospikeworkshop.config import get_settings
from aerospikeworkshop.data_loading_service import DataLoadingService
from aerospikeworkshop.dependencies import create_key_value_service
from aerospikeworkshop.json_parsing_service import JsonParsingService
from aerospikeworkshop.routers import cart, data_loading, retail
from aerospikeworkshop.startup import run_startup

logger = logging.getLogger(__name__)

LOG_FORMAT = "%(levelname)s:     %(message)s"


def configure_logging() -> None:
    """Ensure application INFO logs appear when running under uvicorn."""
    app_logger = logging.getLogger("aerospikeworkshop")
    if app_logger.handlers:
        return
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter(LOG_FORMAT))
    app_logger.addHandler(handler)
    app_logger.setLevel(logging.INFO)
    app_logger.propagate = False


configure_logging()

STATIC_DIR = Path(__file__).resolve().parents[2] / "static"


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    kv_service = create_key_value_service(settings)
    await kv_service.connect()
    data_loading_service = DataLoadingService(JsonParsingService(), kv_service)
    app.state.kv_service = kv_service
    app.state.data_loading_service = data_loading_service
    logger.info(
        "Started with client profile: %s", settings.aerospike_client_profile
    )
    await run_startup(data_loading_service)
    yield
    await kv_service.close()


def create_app() -> FastAPI:
    app = FastAPI(
        title="Aerospike Retail Demo",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        lifespan=lifespan,
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(retail.router)
    app.include_router(cart.router)
    app.include_router(data_loading.router)

    if STATIC_DIR.is_dir():
        app.mount("/static", StaticFiles(directory=STATIC_DIR / "static"), name="static")

        @app.get("/favicon.ico")
        async def favicon():
            path = STATIC_DIR / "favicon.ico"
            if path.is_file():
                return FileResponse(path)
            return FileResponse(STATIC_DIR / "index.html")

        @app.get("/manifest.json")
        async def manifest():
            path = STATIC_DIR / "manifest.json"
            if path.is_file():
                return FileResponse(path)
            return FileResponse(STATIC_DIR / "index.html")

        @app.get("/robots.txt")
        async def robots():
            path = STATIC_DIR / "robots.txt"
            if path.is_file():
                return FileResponse(path)
            return FileResponse(STATIC_DIR / "index.html")

        @app.get("/{full_path:path}")
        async def spa_fallback(full_path: str):
            if full_path.startswith("rest/"):
                raise HTTPException(status_code=404)
            candidate = STATIC_DIR / full_path
            if candidate.is_file():
                return FileResponse(candidate)
            return FileResponse(STATIC_DIR / "index.html")

    return app


app = create_app()


def run() -> None:
    settings = get_settings()
    uvicorn.run(
        "aerospikeworkshop.main:app",
        host="0.0.0.0",
        port=settings.server_port,
        reload=False,
    )


if __name__ == "__main__":
    run()
