"""Cart API endpoints — mirrors CartController.java."""

from __future__ import annotations

from fastapi import APIRouter, Form, Request
from fastapi.responses import JSONResponse

from aerospikeworkshop.models.cart import Cart

router = APIRouter(prefix="/rest/v1/cart", tags=["cart"])


def _cart_response(cart: Cart, **extra) -> dict:
    response = {
        "items": [item.model_dump(by_alias=True, mode="json") for item in cart.get_items_list()],
        "total": cart.total,
        "itemCount": cart.item_count,
        "success": True,
    }
    response.update(extra)
    return response


@router.get("/health")
async def health() -> dict:
    return {"status": "healthy", "service": "cart"}


@router.get("/{user_id}")
async def get_cart(request: Request, user_id: str):
    try:
        cart = await request.app.state.kv_service.get_cart(user_id)
        return _cart_response(cart)
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={"success": False, "error": str(exc)},
        )


@router.post("/{user_id}/add")
async def add_to_cart(
    request: Request,
    user_id: str,
    productId: str = Form(...),
    quantity: int = Form(1),
):
    try:
        cart = await request.app.state.kv_service.add_to_cart(
            user_id, productId, quantity
        )
        return _cart_response(cart, message="Item added to cart successfully")
    except Exception as exc:
        return JSONResponse(
            status_code=400,
            content={"success": False, "error": str(exc)},
        )


@router.put("/{user_id}/update")
async def update_cart_item(
    request: Request,
    user_id: str,
    productId: str = Form(...),
    quantity: int = Form(...),
):
    try:
        cart = await request.app.state.kv_service.update_cart_item(
            user_id, productId, quantity
        )
        return _cart_response(cart, message="Cart updated successfully")
    except Exception as exc:
        return JSONResponse(
            status_code=400,
            content={"success": False, "error": str(exc)},
        )


@router.delete("/{user_id}/remove")
async def remove_from_cart(request: Request, user_id: str, productId: str):
    try:
        cart = await request.app.state.kv_service.remove_from_cart(user_id, productId)
        return _cart_response(cart, message="Item removed from cart")
    except Exception as exc:
        return JSONResponse(
            status_code=400,
            content={"success": False, "error": str(exc)},
        )


@router.delete("/{user_id}/clear")
async def clear_cart(request: Request, user_id: str):
    try:
        cart = await request.app.state.kv_service.clear_cart(user_id)
        return _cart_response(cart, message="Cart cleared successfully")
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={"success": False, "error": str(exc)},
        )
