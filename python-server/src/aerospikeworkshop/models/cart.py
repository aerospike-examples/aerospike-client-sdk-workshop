"""Cart model — mirrors com.aerospikeworkshop.model.Cart."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field

from aerospikeworkshop.models.cart_item import CartItem


class Cart(BaseModel):
    items: dict[str, CartItem] = Field(default_factory=dict)

    def get_items_list(self) -> list[CartItem]:
        return list(self.items.values())

    @property
    def total(self) -> float:
        return sum(item.price * item.quantity for item in self.items.values())

    @property
    def item_count(self) -> int:
        return sum(item.quantity for item in self.items.values())

    def find_item(self, product_id: str) -> CartItem | None:
        return self.items.get(product_id)

    def add(self, item: CartItem) -> Cart:
        self.items[item.product_id] = item
        return self

    def remove(self, product_id: str) -> CartItem | None:
        return self.items.pop(product_id, None)

    @classmethod
    def from_bins(cls, bins: dict[str, Any]) -> Cart:
        cart = cls()
        raw_items = bins.get("items")
        if isinstance(raw_items, dict):
            for _key, value in raw_items.items():
                if isinstance(value, dict):
                    item = CartItem.from_bins(value)
                    cart.items[item.product_id] = item
        return cart

    def to_bins(self) -> dict[str, Any]:
        return {
            "items": {
                product_id: item.to_bins() for product_id, item in self.items.items()
            }
        }
