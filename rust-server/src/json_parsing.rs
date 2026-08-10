use std::path::{Path, PathBuf};

use serde_json::Value as JsonValue;

pub struct JsonParsingService;

impl JsonParsingService {
    pub fn parse_product_file(&self, file_path: &str) -> Result<JsonValue, String> {
        let content = std::fs::read_to_string(file_path)
            .map_err(|e| format!("Failed to read {file_path}: {e}"))?;
        let root: JsonValue =
            serde_json::from_str(&content).map_err(|e| format!("Invalid JSON in {file_path}: {e}"))?;
        root.get("data")
            .cloned()
            .ok_or_else(|| "Invalid JSON structure - missing 'data' node".to_string())
    }

    pub fn get_style_files(&self, data_root_path: &str) -> Result<Vec<String>, String> {
        let styles_path = Path::new(data_root_path).join("styles");
        if !styles_path.is_dir() {
            return Err(format!("Styles directory not found: {}", styles_path.display()));
        }
        let mut files = Vec::new();
        collect_json_files(&styles_path, &mut files)?;
        files.sort();
        Ok(files)
    }

    pub fn extract_product_id(&self, file_path: &str) -> String {
        Path::new(file_path)
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or_default()
            .to_string()
    }

    pub fn format_product_data(&self, data: &JsonValue, product_id: &str) -> JsonValue {
        let obj = data.as_object().cloned().unwrap_or_default();
        let get_str = |key: &str| obj.get(key).and_then(|v| v.as_str()).map(str::to_string);
        let get_i64 = |key: &str| obj.get(key).and_then(|v| v.as_i64());

        let display_cat = obj
            .get("displayCategories")
            .and_then(|v| v.as_str())
            .map(|s| {
                JsonValue::Array(
                    s.split(',')
                        .map(|part| JsonValue::String(part.to_string()))
                        .collect(),
                )
            })
            .unwrap_or_else(|| JsonValue::Array(vec![JsonValue::String("NA".to_string())]));

        let category = obj
            .get("masterCategory")
            .and_then(|v| v.get("typeName"))
            .and_then(|v| v.as_str())
            .map(|s| JsonValue::String(s.to_string()));

        let sub_category = obj
            .get("subCategory")
            .and_then(|v| v.get("typeName"))
            .and_then(|v| v.as_str())
            .map(|s| JsonValue::String(s.to_string()));

        let article_type = obj
            .get("articleType")
            .and_then(|v| v.get("typeName"))
            .and_then(|v| v.as_str())
            .map(|s| JsonValue::String(s.to_string()));

        let mut product = serde_json::Map::new();
        product.insert("id".to_string(), JsonValue::String(product_id.to_string()));
        if let Some(v) = get_i64("price") {
            product.insert("price".to_string(), JsonValue::Number(v.into()));
        }
        if let Some(v) = get_i64("discountedPrice") {
            product.insert("salePrice".to_string(), JsonValue::Number(v.into()));
        }
        if let Some(v) = get_str("productDisplayName") {
            product.insert("name".to_string(), JsonValue::String(v));
        }
        if let Some(v) = obj.get("productDescriptors") {
            product.insert("descriptors".to_string(), v.clone());
        }
        if let Some(v) = get_str("variantName") {
            product.insert("variantName".to_string(), JsonValue::String(v));
        }
        if let Some(v) = get_i64("catalogAddDate") {
            product.insert("added".to_string(), JsonValue::Number(v.into()));
        }
        if let Some(v) = get_str("brandName") {
            product.insert("brandName".to_string(), JsonValue::String(v));
        }
        if let Some(v) = get_str("ageGroup") {
            product.insert("ageGroup".to_string(), JsonValue::String(v));
        }
        if let Some(v) = get_str("gender") {
            product.insert("gender".to_string(), JsonValue::String(v));
        }
        product.insert(
            "colors".to_string(),
            JsonValue::Array(vec![
                get_str("baseColour").map(JsonValue::String).unwrap_or(JsonValue::Null),
                get_str("colour1").map(JsonValue::String).unwrap_or(JsonValue::Null),
                get_str("colour2").map(JsonValue::String).unwrap_or(JsonValue::Null),
            ]),
        );
        if let Some(v) = get_str("season") {
            product.insert("season".to_string(), JsonValue::String(v));
        }
        if let Some(v) = get_str("usage") {
            product.insert("usage".to_string(), JsonValue::String(v));
        }
        if let Some(v) = obj.get("articleAttributes") {
            product.insert("articleAttr".to_string(), v.clone());
        }
        if let Some(v) = obj.get("styleImages") {
            product.insert("images".to_string(), v.clone());
        }
        if let Some(v) = obj.get("styleOptions") {
            product.insert("options".to_string(), v.clone());
        }
        product.insert("displayCat".to_string(), display_cat);
        if let Some(v) = category {
            product.insert("category".to_string(), v);
        }
        if let Some(v) = sub_category {
            product.insert("subCategory".to_string(), v);
        }
        if let Some(v) = article_type {
            product.insert("articleType".to_string(), v);
        }

        JsonValue::Object(product)
    }
}

fn collect_json_files(dir: &Path, files: &mut Vec<String>) -> Result<(), String> {
    for entry in std::fs::read_dir(dir).map_err(|e| e.to_string())? {
        let entry = entry.map_err(|e| e.to_string())?;
        let path = entry.path();
        if path.is_dir() {
            collect_json_files(&path, files)?;
        } else if path.extension().and_then(|e| e.to_str()) == Some("json") {
            files.push(path.to_string_lossy().to_string());
        }
    }
    Ok(())
}

pub fn resolve_data_path() -> PathBuf {
    let relative = PathBuf::from("../data");
    if relative.join("styles").is_dir() {
        return relative.canonicalize().unwrap_or(relative);
    }
    let from_root = PathBuf::from("data");
    if from_root.join("styles").is_dir() {
        return from_root.canonicalize().unwrap_or(from_root);
    }
    relative
}
