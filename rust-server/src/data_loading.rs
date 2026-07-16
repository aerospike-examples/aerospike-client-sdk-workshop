use std::sync::Arc;

use crate::json_parsing::JsonParsingService;
use crate::models::product::Product;
use crate::services::KeyValueService;
use crate::utils::value_conv::json_to_value;

#[derive(Debug, Clone)]
pub struct LoadResult {
    pub success_count: usize,
    pub error_count: usize,
    pub total_files: usize,
}

impl LoadResult {
    pub fn success_rate(&self) -> f64 {
        if self.total_files == 0 {
            0.0
        } else {
            self.success_count as f64 / self.total_files as f64 * 100.0
        }
    }
}

pub struct DataLoadingService {
    json: JsonParsingService,
    kv: Arc<dyn KeyValueService>,
}

impl DataLoadingService {
    pub fn new(kv: Arc<dyn KeyValueService>) -> Self {
        Self {
            json: JsonParsingService,
            kv,
        }
    }

    pub async fn load_all_data(&self, data_root_path: &str) -> Result<LoadResult, String> {
        self.create_secondary_indexes().await?;
        let json_files = self.json.get_style_files(data_root_path)?;
        let mut success_count = 0;
        let mut error_count = 0;

        for file_path in &json_files {
            match self.load_single_product(file_path).await {
                Ok(()) => success_count += 1,
                Err(err) => {
                    error_count += 1;
                    tracing::warn!("Error loading file {file_path}: {err}");
                }
            }
        }

        Ok(LoadResult {
            success_count,
            error_count,
            total_files: json_files.len(),
        })
    }

    pub async fn load_single_product(&self, file_path: &str) -> Result<(), String> {
        let raw = self.json.parse_product_file(file_path)?;
        let product_id = self.json.extract_product_id(file_path);
        let product_map = self.json.format_product_data(&raw, &product_id);
        self.load_product_categories(&product_map).await?;
        let bins = match product_map {
            serde_json::Value::Object(map) => map
                .into_iter()
                .map(|(k, v)| (k, json_to_value(&v)))
                .collect(),
            _ => return Err("formatted product is not an object".to_string()),
        };
        let product = Product::from_bins(&bins).map_err(|e| e.to_string())?;
        self.kv
            .store_product(&product)
            .await
            .map_err(|e| e.to_string())
    }

    async fn create_secondary_indexes(&self) -> Result<(), String> {
        let indexes = [
            ("category", "cat_idx"),
            ("subCategory", "subCat_idx"),
            ("articleType", "articleType_idx"),
            ("usage", "usage_idx"),
            ("brandName", "brand_idx"),
        ];
        for (bin_name, index_name) in indexes {
            self.kv
                .create_string_index(bin_name, index_name)
                .await
                .map_err(|e| e.to_string())?;
        }
        Ok(())
    }

    async fn load_product_categories(&self, product: &serde_json::Value) -> Result<(), String> {
        let get = |key: &str| product.get(key).and_then(|v| v.as_str()).map(str::to_string);
        if let (Some(category), Some(sub_category), Some(article_type), Some(usage), Some(brand_name)) =
            (get("category"), get("subCategory"), get("articleType"), get("usage"), get("brandName"))
        {
            self.kv
                .load_categories(&category, &sub_category, &article_type, &usage, &brand_name)
                .await
                .map_err(|e| e.to_string())?;
        }
        Ok(())
    }

    pub async fn get_product_count(&self) -> Result<i64, String> {
        self.kv.get_product_count().await.map_err(|e| e.to_string())
    }

    pub async fn create_single_index(&self, bin_name: &str, index_name: &str) -> Result<(), String> {
        self.kv
            .create_string_index(bin_name, index_name)
            .await
            .map_err(|e| e.to_string())
    }

    pub async fn clear_all_data(&self) -> Result<(), String> {
        self.kv.clear_all_data().await.map_err(|e| e.to_string())
    }
}
