"""Product model — mirrors com.aerospikeworkshop.model.Product."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class Product(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str | None = None
    brand_name: str | None = Field(default=None, alias="brandName")
    images: dict[str, Any] | None = None
    sub_category: str | None = Field(default=None, alias="subCategory")
    gender: str | None = None
    sale_price: int | None = Field(default=None, alias="salePrice")
    added: int | None = None
    usage: str | None = None
    display_cat: list[str] | None = Field(default=None, alias="displayCat")
    age_group: str | None = Field(default=None, alias="ageGroup")
    colors: list[str] | None = None
    descriptors: dict[str, Any] | None = None
    article_type: str | None = Field(default=None, alias="articleType")
    price: int | None = None
    name: str | None = None
    options: list[dict[str, Any]] | None = None
    season: str | None = None
    article_attr: dict[str, str] | None = Field(default=None, alias="articleAttr")
    variant_name: str | None = Field(default=None, alias="variantName")
    category: str | None = None

    @classmethod
    def from_bins(cls, bins: dict[str, Any]) -> Product:
        return cls.model_validate(bins)

    def to_bins(self) -> dict[str, Any]:
        return self.model_dump(by_alias=True, exclude_none=False)
