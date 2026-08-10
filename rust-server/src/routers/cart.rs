use std::sync::Arc;

use axum::extract::{Form, Path, Query, State};
use axum::http::StatusCode;
use axum::routing::{delete, get, post, put};
use axum::Json;
use axum::Router;
use serde::Serialize;
use serde_json::{json, Value};

use crate::models::cart::Cart;
use crate::startup::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct CartResponse {
    items: Vec<crate::models::cart_item::CartItem>,
    total: f64,
    item_count: i64,
    success: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    message: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<String>,
}

fn cart_response(cart: &Cart, message: Option<&str>) -> CartResponse {
    CartResponse {
        items: cart.items.values().cloned().collect(),
        total: cart.total(),
        item_count: cart.item_count(),
        success: true,
        message: message.map(str::to_string),
        error: None,
    }
}

pub fn router() -> Router<Arc<AppState>> {
    Router::new()
        .route("/rest/v1/cart/health", get(health))
        .route("/rest/v1/cart/{user_id}", get(get_cart))
        .route("/rest/v1/cart/{user_id}/add", post(add_to_cart))
        .route("/rest/v1/cart/{user_id}/update", put(update_cart_item))
        .route("/rest/v1/cart/{user_id}/remove", delete(remove_from_cart))
        .route("/rest/v1/cart/{user_id}/clear", delete(clear_cart))
}

async fn health() -> Json<Value> {
    Json(json!({ "status": "healthy", "service": "cart" }))
}

async fn get_cart(
    State(state): State<Arc<AppState>>,
    Path(user_id): Path<String>,
) -> Result<Json<CartResponse>, (StatusCode, Json<Value>)> {
    match state.kv_service.get_cart(&user_id).await {
        Ok(cart) => Ok(Json(cart_response(&cart, None))),
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({ "success": false, "error": err.to_string() })),
        )),
    }
}

#[derive(serde::Deserialize)]
#[serde(rename_all = "camelCase")]
struct AddToCartForm {
    product_id: String,
    #[serde(default = "default_quantity")]
    quantity: i32,
}

fn default_quantity() -> i32 {
    1
}

async fn add_to_cart(
    State(state): State<Arc<AppState>>,
    Path(user_id): Path<String>,
    Form(form): Form<AddToCartForm>,
) -> Result<Json<CartResponse>, (StatusCode, Json<Value>)> {
    match state
        .kv_service
        .add_to_cart(&user_id, &form.product_id, form.quantity)
        .await
    {
        Ok(cart) => Ok(Json(cart_response(
            &cart,
            Some("Item added to cart successfully"),
        ))),
        Err(err) => Err((
            StatusCode::BAD_REQUEST,
            Json(json!({ "success": false, "error": err.to_string() })),
        )),
    }
}

#[derive(serde::Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateCartForm {
    product_id: String,
    quantity: i32,
}

async fn update_cart_item(
    State(state): State<Arc<AppState>>,
    Path(user_id): Path<String>,
    Form(form): Form<UpdateCartForm>,
) -> Result<Json<CartResponse>, (StatusCode, Json<Value>)> {
    match state
        .kv_service
        .update_cart_item(&user_id, &form.product_id, form.quantity)
        .await
    {
        Ok(cart) => Ok(Json(cart_response(&cart, Some("Cart updated successfully")))),
        Err(err) => Err((
            StatusCode::BAD_REQUEST,
            Json(json!({ "success": false, "error": err.to_string() })),
        )),
    }
}

#[derive(serde::Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemoveForm {
    product_id: String,
}

async fn remove_from_cart(
    State(state): State<Arc<AppState>>,
    Path(user_id): Path<String>,
    Query(form): Query<RemoveForm>,
) -> Result<Json<CartResponse>, (StatusCode, Json<Value>)> {
    match state
        .kv_service
        .remove_from_cart(&user_id, &form.product_id)
        .await
    {
        Ok(cart) => Ok(Json(cart_response(&cart, Some("Item removed from cart")))),
        Err(err) => Err((
            StatusCode::BAD_REQUEST,
            Json(json!({ "success": false, "error": err.to_string() })),
        )),
    }
}

async fn clear_cart(
    State(state): State<Arc<AppState>>,
    Path(user_id): Path<String>,
) -> Result<Json<CartResponse>, (StatusCode, Json<Value>)> {
    match state.kv_service.clear_cart(&user_id).await {
        Ok(cart) => Ok(Json(cart_response(&cart, Some("Cart cleared successfully")))),
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({ "success": false, "error": err.to_string() })),
        )),
    }
}
