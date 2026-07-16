use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};

use axum::extract::{Query, State};
use axum::http::StatusCode;
use axum::routing::{delete, get, post};
use axum::Json;
use axum::Router;
use serde_json::{json, Value};

use crate::startup::AppState;

pub fn router() -> Router<Arc<AppState>> {
    Router::new()
        .route("/rest/v1/data/health", get(health))
        .route("/rest/v1/data/load", post(load_data))
        .route("/rest/v1/data/load-single", post(load_single_product))
        .route("/rest/v1/data/count", get(get_product_count))
        .route("/rest/v1/data/clear", delete(clear_all_data))
        .route("/rest/v1/data/create-indexes", post(create_secondary_indexes))
}

async fn health() -> Json<Value> {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0);
    Json(json!({
        "status": "OK",
        "service": "Data Loading Service",
        "message": "Data loading service is operational",
        "timestamp": timestamp,
    }))
}

#[derive(serde::Deserialize)]
struct LoadDataQuery {
    #[serde(rename = "dataPath")]
    data_path: String,
}

async fn load_data(
    State(state): State<Arc<AppState>>,
    Query(params): Query<LoadDataQuery>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    let path = std::path::Path::new(&params.data_path);
    if !path.is_dir() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({
                "error": format!("Invalid data path: {}", params.data_path),
                "message": "Path does not exist or is not a directory",
            })),
        ));
    }
    if !path.join("styles").is_dir() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({
                "error": "Missing styles directory",
                "message": "The data path must contain a 'styles' subdirectory with JSON files",
            })),
        ));
    }

    match state
        .data_loading_service
        .load_all_data(&params.data_path)
        .await
    {
        Ok(result) => Ok(Json(json!({
            "success": true,
            "totalFiles": result.total_files,
            "successCount": result.success_count,
            "errorCount": result.error_count,
            "successRate": result.success_rate(),
            "message": "Data loading completed",
        }))),
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({
                "error": "Unexpected error during data loading",
                "message": err,
            })),
        )),
    }
}

#[derive(serde::Deserialize)]
struct LoadSingleQuery {
    #[serde(rename = "filePath")]
    file_path: String,
}

async fn load_single_product(
    State(state): State<Arc<AppState>>,
    Query(params): Query<LoadSingleQuery>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    let path = std::path::Path::new(&params.file_path);
    if !path.is_file() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({
                "error": format!("Invalid file path: {}", params.file_path),
                "message": "File does not exist or is not a regular file",
            })),
        ));
    }

    match state
        .data_loading_service
        .load_single_product(&params.file_path)
        .await
    {
        Ok(()) => Ok(Json(json!({
            "success": true,
            "filePath": params.file_path,
            "message": "Product loaded successfully",
        }))),
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({
                "error": "Unexpected error loading product",
                "message": err,
                "filePath": params.file_path,
            })),
        )),
    }
}

async fn get_product_count(
    State(state): State<Arc<AppState>>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    match state.data_loading_service.get_product_count().await {
        Ok(count) => Ok(Json(json!({
            "productCount": count,
            "message": "Product count retrieved successfully",
        }))),
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({
                "error": "Error retrieving product count",
                "message": err,
            })),
        )),
    }
}

#[derive(serde::Deserialize)]
struct ClearQuery {
    confirm: String,
}

async fn clear_all_data(
    State(state): State<Arc<AppState>>,
    Query(params): Query<ClearQuery>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    if params.confirm != "yes-delete-all" {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({
                "error": "Missing or invalid confirmation",
                "message": "To clear all data, provide confirm=yes-delete-all parameter",
            })),
        ));
    }

    match state.data_loading_service.clear_all_data().await {
        Ok(()) => Ok(Json(json!({
            "success": true,
            "message": "All product data cleared",
        }))),
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({
                "error": "Error clearing data",
                "message": err,
            })),
        )),
    }
}

async fn create_secondary_indexes(
    State(state): State<Arc<AppState>>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    let indexes = [
        ("category", "cat_idx"),
        ("subCategory", "subCat_idx"),
        ("usage", "usage_idx"),
        ("brandName", "brand_idx"),
        ("articleType", "article_idx"),
    ];

    let mut results = serde_json::Map::new();
    let mut success_count = 0;
    let mut error_count = 0;

    let total_indexes = indexes.len();
    for (bin_name, index_name) in indexes {
        match state
            .data_loading_service
            .create_single_index(bin_name, index_name)
            .await
        {
            Ok(()) => {
                results.insert(
                    index_name.to_string(),
                    Value::String("Created successfully".to_string()),
                );
                success_count += 1;
            }
            Err(err) => {
                results.insert(
                    index_name.to_string(),
                    Value::String(format!("Error: {err}")),
                );
                error_count += 1;
            }
        }
    }

    Ok(Json(json!({
        "success": error_count == 0,
        "totalIndexes": total_indexes,
        "successCount": success_count,
        "errorCount": error_count,
        "indexResults": results,
        "message": "Secondary index creation completed",
    })))
}
