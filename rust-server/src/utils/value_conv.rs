use std::collections::HashMap;

use aerospike::Value;
use serde_json::{Map, Number, Value as JsonValue};

pub fn value_to_json(value: &Value) -> JsonValue {
    match value {
        Value::Nil => JsonValue::Null,
        Value::Bool(b) => JsonValue::Bool(*b),
        Value::Int(i) => JsonValue::Number(Number::from(*i)),
        Value::Float(f) => {
            let n: f64 = f.into();
            Number::from_f64(n).map(JsonValue::Number).unwrap_or(JsonValue::Null)
        }
        Value::String(s) | Value::GeoJSON(s) => JsonValue::String(s.clone()),
        Value::Blob(b) => JsonValue::String(String::from_utf8_lossy(b).to_string()),
        Value::List(items) => JsonValue::Array(items.iter().map(value_to_json).collect()),
        Value::HashMap(map) => {
            let mut obj = Map::new();
            for (k, v) in map {
                obj.insert(value_key_to_string(k), value_to_json(v));
            }
            JsonValue::Object(obj)
        }
        Value::OrderedMap(map) => {
            let mut obj = Map::new();
            for (k, v) in map {
                obj.insert(value_key_to_string(k), value_to_json(v));
            }
            JsonValue::Object(obj)
        }
        Value::KeyValueList(pairs) => {
            let mut obj = Map::new();
            for (k, v) in pairs {
                obj.insert(value_key_to_string(k), value_to_json(v));
            }
            JsonValue::Object(obj)
        }
        other => JsonValue::String(format!("{other:?}")),
    }
}

fn value_key_to_string(key: &Value) -> String {
    match key {
        Value::String(s) | Value::GeoJSON(s) => s.clone(),
        Value::Int(i) => i.to_string(),
        other => format!("{other:?}"),
    }
}

pub fn json_to_value(value: &JsonValue) -> Value {
    match value {
        JsonValue::Null => Value::Nil,
        JsonValue::Bool(b) => Value::Bool(*b),
        JsonValue::Number(n) => {
            if let Some(i) = n.as_i64() {
                Value::Int(i)
            } else if let Some(f) = n.as_f64() {
                Value::Float(f.into())
            } else {
                Value::Nil
            }
        }
        JsonValue::String(s) => Value::String(s.clone()),
        JsonValue::Array(items) => Value::List(items.iter().map(json_to_value).collect()),
        JsonValue::Object(map) => {
            let mut hm = HashMap::new();
            for (k, v) in map {
                hm.insert(Value::String(k.clone()), json_to_value(v));
            }
            Value::HashMap(hm)
        }
    }
}

pub fn bins_to_json_map(bins: &HashMap<String, Value>) -> Map<String, JsonValue> {
    bins.iter()
        .map(|(k, v)| (k.clone(), value_to_json(v)))
        .collect()
}
