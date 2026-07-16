pub mod value_conv;

use std::collections::HashMap;

use aerospike::Value;

pub fn as_non_null_string(value: Option<&str>) -> String {
    value.unwrap_or("").to_string()
}

pub fn extract_product_image(images: Option<&serde_json::Value>) -> Option<String> {
    images.and_then(|root| {
        get_image_from_path(root, &["search", "resolutions", "125X161"])
            .or_else(|| get_image_from_path(root, &["front", "resolutions", "125X161"]))
    })
}

fn get_image_from_path(root: &serde_json::Value, path: &[&str]) -> Option<String> {
    let mut current = root;
    for segment in &path[..path.len() - 1] {
        current = current.get(*segment)?;
    }
    current.get(path[path.len() - 1])?.as_str().map(str::to_string)
}

pub fn value_as_map(value: &Value) -> Option<HashMap<String, Value>> {
    let mut hm = HashMap::new();
    match value {
        Value::HashMap(map) => {
            for (k, v) in map {
                hm.insert(value_key_to_string(k), v.clone());
            }
        }
        Value::OrderedMap(map) => {
            for (k, v) in map {
                hm.insert(value_key_to_string(k), v.clone());
            }
        }
        Value::KeyValueList(pairs) => {
            for (k, v) in pairs {
                hm.insert(value_key_to_string(k), v.clone());
            }
        }
        _ => return None,
    }
    Some(hm)
}

fn value_key_to_string(key: &Value) -> String {
    match key {
        Value::String(s) | Value::GeoJSON(s) => s.clone(),
        Value::Int(i) => i.to_string(),
        other => format!("{other:?}"),
    }
}

pub fn value_as_list(value: &Value) -> Vec<Value> {
    match value {
        Value::List(items) => items.clone(),
        _ => Vec::new(),
    }
}

pub fn value_as_string(value: &Value) -> Option<String> {
    match value {
        Value::String(s) | Value::GeoJSON(s) => Some(s.clone()),
        _ => None,
    }
}

pub fn value_as_i64(value: &Value) -> Option<i64> {
    match value {
        Value::Int(i) => Some(*i),
        _ => None,
    }
}
