use aerospike::operations::maps;
use std::collections::HashMap;
use std::sync::Arc;
use std::time::Instant;

use aerospike::{Bins, Client, ClientPolicy, Error, GenerationPolicy, MapPolicy, PartitionFilter, QueryPolicy, ReadPolicy, RecordExistsAction, ResultCode, Statement, WritePolicy};
use aerospike::expressions::{and, eq, string_bin, string_val};
use aerospike::operations::cdt_context::ctx_map_key;
use aerospike::operations::MapOrder;
use aerospike::query::Filter;
use async_trait::async_trait;
use futures::StreamExt;
use tokio::sync::RwLock;

use crate::config::Settings;
use crate::models::cart::Cart;
use crate::models::cart_item::CartItem;
use crate::models::model_to_value;
use crate::models::product::Product;
use crate::services::constants::*;
use crate::services::{KeyValueService, QueryResult};
use crate::services::reference_client::ReferenceClient;
use crate::utils::{as_non_null_string, extract_product_image};

/// Workshop skeleton — participants implement STEP 1 through STEP 7.
pub struct WorkshopClient {
    settings: Settings,
    client: RwLock<Option<Arc<Client>>>,
    /// Pre-implemented helpers delegate to the reference client for non-workshop methods.
    reference: ReferenceClient,
}

impl WorkshopClient {
    pub fn new(settings: Settings) -> Self {
        let reference = ReferenceClient::new(settings.clone());
        Self {
            settings,
            client: RwLock::new(None),
            reference,
        }
    }

    async fn require_client(&self) -> Result<Arc<Client>, String> {
        self.client
            .read()
            .await
            .as_ref()
            .cloned()
            .ok_or_else(|| "Aerospike client is not connected".to_string())
    }

    fn product_key(product_id: &str) -> aerospike::Key {
        as_key!(NAMESPACE, PRODUCT_SET, product_id)
    }

    fn cart_key(user_id: &str) -> aerospike::Key {
        as_key!(NAMESPACE, CARTS_SET, user_id)
    }

    fn map_policy() -> MapPolicy {
        MapPolicy::new(MapOrder::KeyOrdered, aerospike::operations::MapWriteMode::Update)
    }
}

#[async_trait]
impl KeyValueService for WorkshopClient {
    async fn connect(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        // =================================================================================
        // TODO: STEP 1 — CONNECT TO THE DATABASE
        // =================================================================================
        // Create a ClientPolicy (ClientPolicy::default() is fine to start).
        // Build the hosts string from settings.aerospike_hosts().
        // Connect with Client::new(&policy, &hosts).await? and store in Arc<Client>.
        // Save the client in self.client (RwLock).
        //
        // Hint: One Client per cluster — share it for the lifetime of the server.
        // =================================================================================
        let mut policy = ClientPolicy::default();
        // these are empty by default
        if let Some(user) = self.settings.aerospike_username.as_ref() {
            let pass = self.settings.aerospike_password.as_deref().unwrap_or("");
            policy.auth_mode = aerospike::AuthMode::Internal(user.clone(), pass.to_string());
        }

        let client = Arc::new(
            Client::new(&policy, &self.settings.aerospike_hosts())
                .await
                .map_err(|e| format!("Failed to connect: {e}"))?,
        );
        *self.client.write().await = Some(client.clone());
        self.reference.set_client(client).await;
        tracing::info!(
            hosts = %self.settings.aerospike_hosts(),
            "Connected to Aerospike"
        );
        Ok(())
    }

    async fn close(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        if let Some(client) = self.client.write().await.take() {
            client.close().await?;
        }
        self.reference.close().await
    }

    async fn store_product(
        &self,
        product: &Product,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let product_id = product.id.clone().unwrap_or_default();
        let key = Self::product_key(product_id.as_str());

        // =================================================================================
        // TODO: STEP 2 — STORE A PRODUCT OBJECT
        // =================================================================================
        // Use client.put() with WritePolicy { record_exists_action: CreateOnly, .. }.
        // Build the key with as_key!(NAMESPACE, PRODUCT_SET, product.id).
        // Convert product.to_bins() into Bin values.
        // =================================================================================
        Ok(())
    }

    async fn get_product(
        &self,
        product_id: &str,
    ) -> Result<Option<Product>, Box<dyn std::error::Error + Send + Sync>> {
        let _client = self.require_client().await?;
        // store_product takes a full Product (all bins to write); get_product takes only
        // product_id — a point read needs just the key, bins come back from Aerospike.
        let _key = Self::product_key(product_id);
        // =================================================================================
        // TODO: STEP 3 — GET A PRODUCT BY ID
        // =================================================================================
        // Use client.get() with Bins::All.
        // Match Error::ServerError(ResultCode::KeyNotFoundError, ..) for missing keys.
        // Convert record.bins with Product::from_bins().
        // =================================================================================

        Ok(None)
    }

    async fn query(
        &self,
        index: &str,
        filter_value: &str,
        count: u64,
    ) -> Result<QueryResult, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let start = Instant::now();

        // =================================================================================
        // TODO: STEP 4 — QUERY FOR PRODUCTS
        // =================================================================================
        // Build a Statement with Bins::from([...]) for projection.
        // Add Filter::equal(index, filter_value) via stmt.add_filter().
        // Set QueryPolicy.max_records = count.
        // Stream results with client.query() and futures::StreamExt.
        // =================================================================================
        let product = self.get_product("13283").await?;
        let products = product.into_iter().collect();

        Ok(QueryResult {
            products,
            time_ms: start.elapsed().as_millis() as i64,
        })
    }

    async fn advanced_search(
        &self,
        category: Option<&str>,
        article_type: Option<&str>,
        usage: Option<&str>,
        brand_name: Option<&str>,
        _search_text: Option<&str>,
        count: u64,
    ) -> Result<QueryResult, Box<dyn std::error::Error + Send + Sync>> {
        let start = Instant::now();
        let client = self.require_client().await?;

        let indexes = [
            ("category", as_non_null_string(category)),
            ("articleType", as_non_null_string(article_type)),
            ("usage", as_non_null_string(usage)),
            ("brandName", as_non_null_string(brand_name)),
        ];

        let mut filter_expr = String::new();
        for (field, value) in &indexes {
            if !value.is_empty() {
                if !filter_expr.is_empty() {
                    filter_expr.push_str(" AND ");
                }
                filter_expr.push_str(&format!("$.{field} == '{value}'"));
            }
        }
        println!("Filter expression (for discussion): {filter_expr}");
        tracing::debug!(filter_expr = %filter_expr, "Advanced search filters");

        // =================================================================================
        // TODO: STEP 5 — EXECUTE ADVANCED SEARCH
        // =================================================================================
        // Use Filter::equal() on the first non-empty field for the secondary index.
        // Build additional filters with expressions::and / eq / string_bin / string_val
        // on QueryPolicy.base_policy.filter_expression.
        // Stream and collect Product results.
        // =================================================================================
        let product = self.get_product("13283").await?;
        let products: Vec<Product> = product.into_iter().collect();

        tracing::info!(
            count = products.len(),
            time_ms = start.elapsed().as_millis(),
            "Advanced search completed"
        );

        Ok(QueryResult {
            products,
            time_ms: start.elapsed().as_millis() as i64,
        })
    }

    async fn get_cart(&self, user_id: &str) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;

        // =================================================================================
        // TODO: STEP 6 — GET THE CART OBJECT
        // =================================================================================
        // Point read on shopping_carts set using the user_id as the key.
        // Return Cart::default() when the record is not found.
        // =================================================================================
       let product = self.get_product("13283").await?;
        if product.is_none() {
            return Ok(Cart::default());
        }
        let product = product.unwrap();
        Ok(Cart {
            items: [(
                "13283".to_string(),
                CartItem::from_product(
                    user_id,
                    2,
                    Some(
                        "http://assets.myntassets.com/v1/images/style/properties/3c03eb9654656c19f467f85b7de928f5_images.jpg".to_string(),
                    ),
                    &product,
                ),
            )]
            .into_iter()
            .collect(),
        })
    }

    async fn add_to_cart(
        &self,
        user_id: &str,
        product_id: &str,
        quantity: i32,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;

        let product = self
            .get_product(product_id)
            .await?
            .ok_or_else(|| format!("Product not found: {product_id}"))?;
        let _image = extract_product_image(product.images.as_ref());

        // Retry loop for optimistic locking — each iteration is one read-modify-write attempt,
        // not a pass over cart items. Two concurrent adds can both read the same generation;
        // the first write succeeds, the second gets GenerationError. Re-read and retry so
        // both quantities are applied. Without retries, the losing request would fail (or,
        // without ExpectGenEqual at all, silently overwrite the other update and lose a count).
        loop {
            // =================================================================================
            // TODO: STEP 7 — UPDATE THE CART WITH GENERATION CHECK
            // =================================================================================
            // 7a: Read cart record and capture record.generation.
            // 7b: If item exists, use maps::increment_value with ctx_map_key(product_id)
            //     and WritePolicy { generation_policy: ExpectGenEqual, generation, .. }.
            // 7c: If cart missing, create with RecordExistsAction::CreateOnly.
            // Retry on ResultCode::GenerationError (lost update race).
            // =================================================================================
            let mut cart = self.get_cart(user_id).await?;
            let _generation = 1u32;

            if cart.find_item(product_id).is_some() {
                if let Some(item) = cart.find_item_mut(product_id) {
                    item.quantity += quantity;
                }
                // TODO STEP 7b: persist increment with generation check
            } else {
                cart.add(CartItem::from_product(user_id, quantity, _image.clone(), &product));
                // TODO STEP 7c: insert new map entry with generation check
            }

            return Ok(cart);
        }
    }

    // --- Pre-implemented (not part of the workshop steps) ---

    async fn clear_all_data(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        self.reference.clear_all_data().await
    }

    async fn load_categories(
        &self,
        category: &str,
        sub_category: &str,
        article_type: &str,
        usage: &str,
        brand_name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        self.reference
            .load_categories(category, sub_category, article_type, usage, brand_name)
            .await
    }

    async fn create_string_index(
        &self,
        bin_name: &str,
        index_name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        self.reference.create_string_index(bin_name, index_name).await
    }

    async fn get_categories(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.get_categories().await
    }

    async fn get_article_types(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.get_article_types().await
    }

    async fn get_usage(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.get_usage().await
    }

    async fn get_brand_names(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.get_brand_names().await
    }

    async fn get_product_count(&self) -> Result<i64, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.get_product_count().await
    }

    async fn update_cart_item(
        &self,
        user_id: &str,
        product_id: &str,
        quantity: i32,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        self.reference
            .update_cart_item(user_id, product_id, quantity)
            .await
    }

    async fn remove_from_cart(
        &self,
        user_id: &str,
        product_id: &str,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.remove_from_cart(user_id, product_id).await
    }

    async fn clear_cart(&self, user_id: &str) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        self.reference.clear_cart(user_id).await
    }
}
