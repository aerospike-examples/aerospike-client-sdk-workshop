pub mod constants;
pub mod factory;
pub mod reference_client;
pub mod workshop_answers;
pub mod workshop_client;

use async_trait::async_trait;

use crate::models::cart::Cart;
use crate::models::product::Product;

#[derive(Debug, Clone)]
pub struct QueryResult {
    pub products: Vec<Product>,
    pub time_ms: i64,
}

#[async_trait]
pub trait KeyValueService: Send + Sync {
    async fn connect(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>>;
    async fn close(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>>;
    async fn clear_all_data(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>>;
    async fn get_product(
        &self,
        product_id: &str,
    ) -> Result<Option<Product>, Box<dyn std::error::Error + Send + Sync>>;
    async fn query(
        &self,
        index: &str,
        filter_value: &str,
        count: u64,
    ) -> Result<QueryResult, Box<dyn std::error::Error + Send + Sync>>;
    async fn load_categories(
        &self,
        category: &str,
        sub_category: &str,
        article_type: &str,
        usage: &str,
        brand_name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>>;
    async fn create_string_index(
        &self,
        bin_name: &str,
        index_name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>>;
    async fn store_product(
        &self,
        product: &Product,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>>;
    async fn get_categories(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>>;
    async fn get_article_types(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>>;
    async fn get_usage(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>>;
    async fn get_brand_names(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>>;
    async fn advanced_search(
        &self,
        category: Option<&str>,
        article_type: Option<&str>,
        usage: Option<&str>,
        brand_name: Option<&str>,
        search_text: Option<&str>,
        count: u64,
    ) -> Result<QueryResult, Box<dyn std::error::Error + Send + Sync>>;
    async fn get_product_count(&self) -> Result<i64, Box<dyn std::error::Error + Send + Sync>>;
    async fn get_cart(&self, user_id: &str) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>>;
    async fn add_to_cart(
        &self,
        user_id: &str,
        product_id: &str,
        quantity: i32,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>>;
    async fn update_cart_item(
        &self,
        user_id: &str,
        product_id: &str,
        quantity: i32,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>>;
    async fn remove_from_cart(
        &self,
        user_id: &str,
        product_id: &str,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>>;
    async fn clear_cart(&self, user_id: &str) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>>;
}
