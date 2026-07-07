"""JSON parsing for product data files."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


class JsonParsingService:
    def parse_product_file(self, file_path: str) -> dict[str, Any]:
        with open(file_path, encoding="utf-8") as handle:
            root = json.load(handle)
        data = root.get("data")
        if data is None:
            raise ValueError("Invalid JSON structure - missing 'data' node")
        return data

    def get_style_files(self, data_root_path: str) -> list[str]:
        styles_path = Path(data_root_path) / "styles"
        if not styles_path.is_dir():
            raise FileNotFoundError(f"Styles directory not found: {styles_path}")
        return sorted(str(path) for path in styles_path.rglob("*.json"))

    def extract_product_id(self, file_path: str) -> str:
        return Path(file_path).stem

    def format_product_data(self, data: dict[str, Any], product_id: str) -> dict[str, Any]:
        product: dict[str, Any] = {
            "id": product_id,
            "price": data.get("price"),
            "salePrice": data.get("discountedPrice"),
            "name": data.get("productDisplayName"),
            "descriptors": data.get("productDescriptors"),
            "variantName": data.get("variantName"),
            "added": data.get("catalogAddDate"),
            "brandName": data.get("brandName"),
            "brandProfile": data.get("brandUserProfile"),
            "ageGroup": data.get("ageGroup"),
            "gender": data.get("gender"),
            "colors": [
                data.get("baseColour"),
                data.get("colour1"),
                data.get("colour2"),
            ],
            "season": data.get("season"),
            "usage": data.get("usage"),
            "articleAttr": data.get("articleAttributes"),
            "images": data.get("styleImages"),
            "options": data.get("styleOptions"),
        }

        display_categories = data.get("displayCategories")
        if display_categories:
            product["displayCat"] = display_categories.split(",")
        else:
            product["displayCat"] = ["NA"]

        master_category = data.get("masterCategory") or {}
        if isinstance(master_category, dict):
            product["category"] = master_category.get("typeName")

        sub_category = data.get("subCategory") or {}
        if isinstance(sub_category, dict):
            product["subCategory"] = sub_category.get("typeName")

        article_type = data.get("articleType") or {}
        if isinstance(article_type, dict):
            product["articleType"] = article_type.get("typeName")

        colours = data.get("colours") or {}
        if isinstance(colours, dict) and colours.get("colors") is not None:
            product["styles"] = colours.get("colors")

        return product
