use std::collections::HashMap;

use aerospike::Value;
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;

use super::{model_to_bins, record_to_model};

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct Product {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub brand_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub images: Option<JsonValue>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sub_category: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub gender: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sale_price: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub added: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub usage: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_cat: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub age_group: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub colors: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub descriptors: Option<JsonValue>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub article_type: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub price: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<Vec<JsonValue>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub season: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub article_attr: Option<HashMap<String, String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub variant_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub category: Option<String>,
}

impl Product {
    pub fn from_bins(bins: &HashMap<String, Value>) -> Result<Self, serde_json::Error> {
        record_to_model(bins)
    }

    pub fn to_bins(&self) -> HashMap<String, Value> {
        model_to_bins(self)
    }
}
