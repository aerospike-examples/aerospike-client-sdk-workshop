"""Shared helpers for key-value services."""

from __future__ import annotations

from typing import Any


def extract_product_image(images: dict[str, Any] | None) -> str | None:
    if images is None:
        return None
    image = _get_image_from_path(images, "search", "resolutions", "125X161")
    if image is not None:
        return image
    return _get_image_from_path(images, "front", "resolutions", "125X161")


def _get_image_from_path(root: dict[str, Any], *path: str) -> str | None:
    current: Any = root
    for segment in path[:-1]:
        if not isinstance(current, dict):
            return None
        current = current.get(segment)
        if not isinstance(current, dict):
            return None
    if not isinstance(current, dict):
        return None
    result = current.get(path[-1])
    return result if isinstance(result, str) else None


def as_non_null_string(value: str | None) -> str:
    return "" if value is None else value
