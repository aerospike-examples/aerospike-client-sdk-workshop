use std::sync::Arc;

use crate::data_loading::DataLoadingService;
use crate::json_parsing::resolve_data_path;

pub async fn run_startup(data_loading_service: &DataLoadingService) {
    if let Err(err) = create_secondary_indexes(data_loading_service).await {
        tracing::debug!("Index creation note: {err}");
    }

    match data_loading_service.get_product_count().await {
        Ok(count) if count > 0 => {
            tracing::info!("Database already contains {count} products, skipping auto-load");
            return;
        }
        Ok(_) => {}
        Err(err) => {
            tracing::warn!("Could not check product count: {err}");
        }
    }

    let data_path = resolve_data_path();
    tracing::info!("Auto-loading sample data from {}", data_path.display());
    match data_loading_service
        .load_all_data(&data_path.to_string_lossy())
        .await
    {
        Ok(result) => tracing::info!(
            "Auto-load complete: {} of {} files loaded",
            result.success_count,
            result.total_files
        ),
        Err(err) => {
            tracing::warn!("Auto-load failed (database may not be ready): {err}");
            tracing::info!(
                "You can manually load data via: POST /rest/v1/data/load?dataPath=<path>"
            );
        }
    }
}

async fn create_secondary_indexes(service: &DataLoadingService) -> Result<(), String> {
    let indexes = [
        ("category", "cat_idx"),
        ("subCategory", "subCat_idx"),
        ("usage", "usage_idx"),
        ("brandName", "brand_idx"),
        ("articleType", "article_idx"),
    ];
    for (bin_name, index_name) in indexes {
        service.create_single_index(bin_name, index_name).await?;
        tracing::info!("Created secondary index: {index_name} on bin {bin_name}");
    }
    Ok(())
}

pub struct AppState {
    pub kv_service: Arc<dyn crate::services::KeyValueService>,
    pub data_loading_service: Arc<DataLoadingService>,
}
