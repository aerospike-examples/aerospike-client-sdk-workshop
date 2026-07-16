pub mod cart;
pub mod cart_item;
pub mod product;

use std::collections::HashMap;

use aerospike::Value;
use serde_json::Value as JsonValue;

use crate::utils::value_conv::{bins_to_json_map, json_to_value};

pub fn record_to_model<T: serde::de::DeserializeOwned>(
    bins: &HashMap<String, Value>,
) -> Result<T, serde_json::Error> {
    let map = bins_to_json_map(bins);
    serde_json::from_value(JsonValue::Object(map))
}

pub fn model_to_bins<T: serde::Serialize>(model: &T) -> HashMap<String, Value> {
    let json = serde_json::to_value(model).expect("model serializes to JSON");
    match json {
        JsonValue::Object(map) => map
            .into_iter()
            .map(|(k, v)| (k, json_to_value(&v)))
            .collect(),
        _ => HashMap::new(),
    }
}

pub fn model_to_value<T: serde::Serialize>(model: &T) -> Value {
    json_to_value(&serde_json::to_value(model).expect("model serializes to JSON"))
}
