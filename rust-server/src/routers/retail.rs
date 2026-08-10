use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};

use axum::extract::{Query, State};
use axum::routing::get;
use axum::Json;
use axum::Router;
use serde::Serialize;
use serde_json::{json, Value};

use crate::models::product::Product;
use crate::startup::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ProductResponse {
    #[serde(flatten)]
    product: Product,
}

pub fn router() -> Router<Arc<AppState>> {
    Router::new()
        .route("/rest/v1/health", get(health))
        .route("/rest/v1/home", get(get_home))
        .route("/rest/v1/get", get(get_product))
        .route("/rest/v1/search", get(search))
        .route("/rest/v1/categories", get(get_categories))
        .route("/rest/v1/article-types", get(get_article_types))
        .route("/rest/v1/usage-types", get(get_usage_types))
        .route("/rest/v1/brand-names", get(get_brand_names))
        .route("/rest/v1/category", get(get_category))
}

async fn health() -> Json<Value> {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0);
    Json(json!({
        "status": "OK",
        "message": "Rust Axum server is running",
        "timestamp": timestamp,
    }))
}

async fn get_home(State(state): State<Arc<AppState>>) -> Result<Json<Value>, String> {
    let kv = &state.kv_service;
    let shoes = kv.query("subCategory", "Shoes", 10).await.map_err(|e| e.to_string())?;
    let bags = kv.query("subCategory", "Bags", 10).await.map_err(|e| e.to_string())?;
    let wallets = kv.query("subCategory", "Wallets", 10).await.map_err(|e| e.to_string())?;
    let watches = kv.query("subCategory", "Watches", 10).await.map_err(|e| e.to_string())?;
    let headwear = kv.query("subCategory", "Headwear", 10).await.map_err(|e| e.to_string())?;

    Ok(Json(json!({
        "Shoes": shoes.products,
        "Bags": bags.products,
        "Wallets": wallets.products,
        "Watches": watches.products,
        "Headwear": headwear.products,
    })))
}

#[derive(serde::Deserialize)]
struct ProductQuery {
    prod: String,
}

async fn get_product(
    State(state): State<Arc<AppState>>,
    Query(params): Query<ProductQuery>,
) -> Result<Json<Value>, String> {
    match state.kv_service.get_product(&params.prod).await {
        Ok(Some(product)) => Ok(Json(json!({
            "error": null,
            "product": product,
            "related": [],
            "also_bought": [],
        }))),
        Ok(None) => Ok(Json(json!({ "error": "Product not found" }))),
        Err(err) => Err(err.to_string()),
    }
}

#[derive(serde::Deserialize)]
struct SearchQuery {
    q: Option<String>,
    category: Option<String>,
    #[serde(rename = "articleType")]
    article_type: Option<String>,
    usage: Option<String>,
    #[serde(rename = "brandName")]
    brand_name: Option<String>,
}

async fn search(
    State(state): State<Arc<AppState>>,
    Query(params): Query<SearchQuery>,
) -> Result<Json<Value>, String> {
    let result = state
        .kv_service
        .advanced_search(
            params.category.as_deref(),
            params.article_type.as_deref(),
            params.usage.as_deref(),
            params.brand_name.as_deref(),
            params.q.as_deref(),
            20,
        )
        .await
        .map_err(|e| e.to_string())?;

    Ok(Json(json!({
        "products": result.products,
        "time": result.time_ms,
    })))
}

async fn get_categories(State(state): State<Arc<AppState>>) -> Result<Json<Value>, String> {
    let categories = state.kv_service.get_categories().await.map_err(|e| e.to_string())?;
    Ok(Json(json!({
        "categories": categories,
        "count": categories.len(),
    })))
}

async fn get_article_types(State(state): State<Arc<AppState>>) -> Result<Json<Value>, String> {
    let values = state.kv_service.get_article_types().await.map_err(|e| e.to_string())?;
    Ok(Json(json!({
        "articleTypes": values,
        "count": values.len(),
    })))
}

async fn get_usage_types(State(state): State<Arc<AppState>>) -> Result<Json<Value>, String> {
    let values = state.kv_service.get_usage().await.map_err(|e| e.to_string())?;
    Ok(Json(json!({
        "usageTypes": values,
        "count": values.len(),
    })))
}

async fn get_brand_names(State(state): State<Arc<AppState>>) -> Result<Json<Value>, String> {
    let values = state.kv_service.get_brand_names().await.map_err(|e| e.to_string())?;
    Ok(Json(json!({
        "brandNames": values,
        "count": values.len(),
    })))
}

#[derive(serde::Deserialize)]
struct CategoryQuery {
    idx: String,
    filter_value: String,
}

async fn get_category(
    State(state): State<Arc<AppState>>,
    Query(params): Query<CategoryQuery>,
) -> Result<Json<Value>, String> {
    let result = state
        .kv_service
        .query(&params.idx, &params.filter_value, 20)
        .await
        .map_err(|e| e.to_string())?;
    Ok(Json(json!({
        "products": result.products,
        "time": result.time_ms,
    })))
}
