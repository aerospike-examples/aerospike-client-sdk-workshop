"""CartItem model — mirrors com.aerospikeworkshop.model.CartItem."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from aerospikeworkshop.models.product import Product


class CartItem(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    product_id: str = Field(alias="productId")
    name: str | None = None
    price: int = 0
    brand_name: str | None = Field(default=None, alias="brandName")
    quantity: int = 1
    image: str | None = None
    user_id: str | None = Field(default=None, alias="userId")

    @classmethod
    def from_product(
        cls, user_id: str, quantity: int, image: str | None, product: Product
    ) -> CartItem:
        return cls(
            productId=product.id or "",
            name=product.name,
            price=product.price or 0,
            brandName=product.brand_name,
            quantity=quantity,
            image=image,
            userId=user_id,
        )

    @classmethod
    def from_bins(cls, bins: dict[str, Any]) -> CartItem:
        return cls.model_validate(bins)

    def to_bins(self) -> dict[str, Any]:
        return self.model_dump(by_alias=True, exclude_none=False)
