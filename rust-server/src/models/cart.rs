use std::collections::HashMap;

use aerospike::Value;
use serde::{Deserialize, Serialize};

use crate::models::cart_item::CartItem;
use crate::models::{record_to_model};
use crate::utils::{value_as_map};

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct Cart {
    #[serde(default)]
    pub items: HashMap<String, CartItem>,
}

impl Cart {
    pub fn items_list(&self) -> Vec<&CartItem> {
        self.items.values().collect()
    }

    pub fn total(&self) -> f64 {
        self.items
            .values()
            .map(|item| item.price as f64 * item.quantity as f64)
            .sum()
    }

    pub fn item_count(&self) -> i64 {
        self.items.values().map(|item| item.quantity as i64).sum()
    }

    pub fn find_item(&self, product_id: &str) -> Option<&CartItem> {
        self.items.get(product_id)
    }

    pub fn find_item_mut(&mut self, product_id: &str) -> Option<&mut CartItem> {
        self.items.get_mut(product_id)
    }

    pub fn add(&mut self, item: CartItem) {
        self.items.insert(item.product_id.clone(), item);
    }

    pub fn remove(&mut self, product_id: &str) -> Option<CartItem> {
        self.items.remove(product_id)
    }

    pub fn from_bins(bins: &HashMap<String, Value>) -> Result<Self, serde_json::Error> {
        let mut cart = Cart::default();
        if let Some(items_value) = bins.get("items") {
            if let Some(items_map) = value_as_map(items_value) {
                for (_key, value) in items_map {
                    if let Some(item_map) = value_as_map(&value) {
                        if let Ok(item) = record_to_model::<CartItem>(&item_map) {
                            cart.items.insert(item.product_id.clone(), item);
                        }
                    }
                }
            }
        }
        Ok(cart)
    }
}
