use std::collections::HashMap;

use aerospike::Value;
use serde::{Deserialize, Serialize};

use super::product::Product;
use super::{model_to_bins, record_to_model};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CartItem {
    pub product_id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(default)]
    pub price: i64,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub brand_name: Option<String>,
    #[serde(default = "default_quantity")]
    pub quantity: i32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_id: Option<String>,
}

fn default_quantity() -> i32 {
    1
}

impl CartItem {
    pub fn from_product(
        user_id: &str,
        quantity: i32,
        image: Option<String>,
        product: &Product,
    ) -> Self {
        Self {
            product_id: product.id.clone().unwrap_or_default(),
            name: product.name.clone(),
            price: product.price.unwrap_or(0),
            brand_name: product.brand_name.clone(),
            quantity,
            image,
            user_id: Some(user_id.to_string()),
        }
    }

    pub fn from_bins(bins: &HashMap<String, Value>) -> Result<Self, serde_json::Error> {
        record_to_model(bins)
    }

    pub fn to_bins(&self) -> HashMap<String, Value> {
        model_to_bins(self)
    }
}
