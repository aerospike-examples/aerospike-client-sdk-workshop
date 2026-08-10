use aerospike::as_val;

use std::collections::HashMap;
use std::sync::Arc;
use std::time::Instant;

use aerospike::expressions::{and, eq, string_bin, string_val};
use aerospike::operations::cdt_context::ctx_map_key;
use aerospike::operations::lists::{self, ListOrderType, ListWriteFlags};
use aerospike::operations::maps::{self, MapOrder, MapPolicy, MapReturnType, MapWriteFlags};
use aerospike::query::{Filter, PartitionFilter};
use aerospike::{
    AdminPolicy, Bins, Client, ClientPolicy, CollectionIndexType, Error, GenerationPolicy,
    IndexType, QueryPolicy, ReadPolicy, RecordExistsAction, ResultCode, Statement, Task, Value,
    WritePolicy,
};
use async_trait::async_trait;
use futures::StreamExt;
use tokio::sync::RwLock;

use crate::config::Settings;
use crate::models::cart::Cart;
use crate::models::cart_item::CartItem;
use crate::models::product::Product;
use crate::models::model_to_value;
use crate::services::constants::*;
use crate::services::{KeyValueService, QueryResult};
use crate::utils::{as_non_null_string, extract_product_image, value_as_list, value_as_string};

pub struct ReferenceClient {
    settings: Settings,
    client: RwLock<Option<Arc<Client>>>,
}

impl ReferenceClient {
    pub fn new(settings: Settings) -> Self {
        Self {
            settings,
            client: RwLock::new(None),
        }
    }

    pub async fn set_client(&self, client: Arc<Client>) {
        *self.client.write().await = Some(client);
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

    fn category_key() -> aerospike::Key {
        as_key!(NAMESPACE, CATEGORY_SET, CATEGORY_KEY)
    }

    fn map_policy() -> MapPolicy {
        MapPolicy::new(MapOrder::KeyOrdered, aerospike::operations::MapWriteMode::Update)
    }

    fn load_categories_map_policy() -> MapPolicy {
        MapPolicy::new_with_flags(
            MapOrder::KeyOrdered,
            MapWriteFlags::CREATE_ONLY | MapWriteFlags::NO_FAIL,
        )
    }

    fn list_policy() -> aerospike::operations::lists::ListPolicy {
        lists::ListPolicy::new_with_flags(
            ListOrderType::Ordered,
            [ListWriteFlags::AddUnique, ListWriteFlags::NoFail],
        )
    }
}

#[async_trait]
impl KeyValueService for ReferenceClient {
    async fn connect(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut policy = ClientPolicy::default();
        if let Some(user) = self.settings.aerospike_username.as_ref() {
            let pass = self.settings.aerospike_password.as_deref().unwrap_or("");
            policy.auth_mode = aerospike::AuthMode::Internal(user.clone(), pass.to_string());
        }

        // One Aerospike Client per cluster for the lifetime of this server process.
        // Client::new() opens connections, discovers cluster nodes, and starts background
        // tending — expensive work you must not repeat on every HTTP request.
        //
        // Arc lets every handler clone a cheap pointer to the same Client instead of
        // building a new one per incoming /rest call (which would leak connections and
        // destroy throughput).
        let client = Arc::new(
            Client::new(&policy, &self.settings.aerospike_hosts())
                .await
                .map_err(|e| format!("Failed to connect: {e}"))?,
        );
        *self.client.write().await = Some(client);
        Ok(())
    }

    async fn close(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        if let Some(client) = self.client.write().await.take() {
            client.close().await?;
        }
        Ok(())
    }

    async fn store_product(
        &self,
        product: &Product,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        // unwrap_or_default() on Option<String> builds an owned String (clone when Some,
        // empty String when None) — extra allocation. To borrow as &str instead, use
        // product.id.as_deref().unwrap_or_default().
        let product_id = product.id.clone().unwrap_or_default();
        let key = Self::product_key(product_id.as_str());
        let mut wp = WritePolicy::default();
        wp.record_exists_action = RecordExistsAction::CreateOnly;

        let bins: Vec<_> = product
            .to_bins()
            .into_iter()
            .map(|(name, value)| aerospike::Bin::new(name, value))
            .collect();

        client.put(&wp, &key, &bins).await?;
        Ok(())
    }

    async fn get_product(
        &self,
        product_id: &str,
    ) -> Result<Option<Product>, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        // store_product takes a full Product (all bins to write); get_product takes only
        // product_id — a point read needs just the key, bins come back from Aerospike.
        let key = Self::product_key(product_id);

        match client.get(&ReadPolicy::default(), &key, Bins::All).await {
            Ok(record) => Ok(Some(Product::from_bins(&record.bins)?)),
            Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => Ok(None),
            Err(err) => Err(err.into()),
        }
    }

    async fn clear_all_data(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let admin = AdminPolicy::default();
        client.truncate(&admin, NAMESPACE, PRODUCT_SET, 0).await?;
        client.truncate(&admin, NAMESPACE, CARTS_SET, 0).await?;
        let _ = client
            .delete(&WritePolicy::default(), &Self::category_key())
            .await;
        Ok(())
    }

    async fn query(
        &self,
        index: &str,
        filter_value: &str,
        count: u64,
    ) -> Result<QueryResult, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let start = Instant::now();

        let mut policy = QueryPolicy::default();
        policy.max_records = count;

        let mut stmt = Statement::new(
            NAMESPACE,
            PRODUCT_SET,
            Bins::from(["id", "name", "images", "brandName", "price"]),
        );
        stmt.add_filter(Filter::equal(index, filter_value));

        let rs = client
            .query(&policy, PartitionFilter::all(), stmt)
            .await?;
        let mut stream = rs.into_stream();
        let mut products = Vec::new();

        while let Some(result) = stream.next().await {
            let record = result?;
            products.push(Product::from_bins(&record.bins)?);
        }

        Ok(QueryResult {
            products,
            time_ms: start.elapsed().as_millis() as i64,
        })
    }

    async fn get_categories(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let key = Self::category_key();
        let mpolicy = Self::map_policy();
        let op = maps::get_by_key_range(
            "categories",
            as_val!("A"),
            as_val!("Z"),
            MapReturnType::Key,
        );
        let record = client
            .operate(&WritePolicy::default(), &key, &[op])
            .await?;
        let categories = record
            .bins
            .get("categories")
            .map(value_as_list)
            .unwrap_or_default();
        Ok(categories
            .into_iter()
            .filter_map(|v| value_as_string(&v))
            .collect())
    }

    async fn get_article_types(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        self.get_category_part("articleTypes").await
    }

    async fn get_usage(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        let values = self.get_category_part("usage").await?;
        Ok(values
            .into_iter()
            .filter(|v| !v.is_empty() && v != "NA")
            .collect())
    }

    async fn get_brand_names(&self) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        let values = self.get_category_part("brandNames").await?;
        Ok(values
            .into_iter()
            .filter(|v| !v.is_empty() && v != "NA")
            .collect())
    }

    async fn load_categories(
        &self,
        category: &str,
        sub_category: &str,
        article_type: &str,
        usage: &str,
        brand_name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let key = Self::category_key();
        let mpolicy = Self::load_categories_map_policy();
        let lpolicy = Self::list_policy();

        let ops = vec![
            maps::put(
                &mpolicy,
                "categories",
                as_val!(category),
                Value::HashMap(HashMap::new()),
            ),
            maps::increment_value(
                &mpolicy,
                "categories",
                as_val!(sub_category),
                as_val!(1),
            )
            .context(vec![ctx_map_key(as_val!(category))]),
            lists::append(&lpolicy, "articleTypes", as_val!(article_type)),
            lists::append(&lpolicy, "usage", as_val!(usage)),
            lists::append(&lpolicy, "brandNames", as_val!(brand_name)),
        ];

        client.operate(&WritePolicy::default(), &key, &ops).await?;
        Ok(())
    }

    async fn create_string_index(
        &self,
        bin_name: &str,
        index_name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        match client
            .create_index_on_bin(
                &AdminPolicy::default(),
                NAMESPACE,
                PRODUCT_SET,
                bin_name,
                index_name,
                IndexType::String,
                CollectionIndexType::Default,
                None,
            )
            .await
        {
            Ok(task) => {
                task.wait_till_complete(None).await?;
                Ok(())
            }
            Err(Error::ServerError(ResultCode::IndexFound, _, _)) => Ok(()),
            Err(err) => {
                tracing::warn!("Index {index_name} already exists or failed to create: {err}");
                Ok(())
            }
        }
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
        let client = self.require_client().await?;
        let start = Instant::now();

        let indexes = [
            ("category", as_non_null_string(category)),
            ("articleType", as_non_null_string(article_type)),
            ("usage", as_non_null_string(usage)),
            ("brandName", as_non_null_string(brand_name)),
        ];

        let mut index_field: Option<&str> = None;
        let mut expr_parts = Vec::new();
        for (field, value) in &indexes {
            if !value.is_empty() {
                if index_field.is_none() {
                    index_field = Some(*field);
                } else {
                    expr_parts.push(eq(
                        string_bin(field.to_string()),
                        string_val(value.clone()),
                    ));
                }
            }
        }

        let mut policy = QueryPolicy::default();
        policy.max_records = count;

        if expr_parts.len() > 1 {
            policy.base_policy.filter_expression = Some(and(expr_parts));
        } else if let Some(expr) = expr_parts.into_iter().next() {
            // and() requires 2+ expressions; a single extra filter stands alone
            policy.base_policy.filter_expression = Some(expr);
        }

        let mut stmt = Statement::new(
            NAMESPACE,
            PRODUCT_SET,
            Bins::from(["id", "name", "images", "brandName", "price"]),
        );

        if let Some(field) = index_field {
            let value = indexes
                .iter()
                .find(|(f, _)| *f == field)
                .map(|(_, v)| v.as_str())
                .unwrap_or("");
            stmt.add_filter(Filter::equal(field, value));
        }

        let rs = client
            .query(&policy, PartitionFilter::all(), stmt)
            .await?;
        let mut stream = rs.into_stream();
        let mut products = Vec::new();

        while let Some(result) = stream.next().await {
            let record = result?;
            products.push(Product::from_bins(&record.bins)?);
        }

        Ok(QueryResult {
            products,
            time_ms: start.elapsed().as_millis() as i64,
        })
    }

    async fn get_product_count(&self) -> Result<i64, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let mut policy = QueryPolicy::default();
        policy.include_bin_data = false;

        let stmt = Statement::new(NAMESPACE, PRODUCT_SET, Bins::None);
        let rs = client
            .query(&policy, PartitionFilter::all(), stmt)
            .await?;
        let mut stream = rs.into_stream();
        let mut count = 0i64;
        while stream.next().await.is_some() {
            count += 1;
        }
        Ok(count)
    }

    async fn get_cart(&self, user_id: &str) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let key = Self::cart_key(user_id);
        match client.get(&ReadPolicy::default(), &key, Bins::All).await {
            Ok(record) => Ok(Cart::from_bins(&record.bins)?),
            Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => Ok(Cart::default()),
            Err(err) => {
                tracing::error!("Error getting cart: {err}");
                Ok(Cart::default())
            }
        }
    }

    async fn add_to_cart(
        &self,
        user_id: &str,
        product_id: &str,
        quantity: i32,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        let product = self
            .get_product(product_id)
            .await?
            .ok_or_else(|| format!("Product not found: {product_id}"))?;

        let client = self.require_client().await?;
        let key = Self::cart_key(user_id);
        let image = extract_product_image(product.images.as_ref());
        let mpolicy = Self::map_policy();

        // Retry loop for optimistic locking — each iteration is one read-modify-write attempt,
        // not a pass over cart items. Two concurrent adds can both read the same generation;
        // the first write succeeds, the second gets GenerationError. We re-read and retry so
        // both quantities are applied. Without retries, the losing request would fail (or,
        // without ExpectGenEqual at all, silently overwrite the other update and lose a count).
        loop {
            let existing = match client.get(&ReadPolicy::default(), &key, Bins::All).await {
                Ok(record) => Some(record),
                Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => None,
                Err(err) => return Err(err.into()),
            };

            let mut wp = WritePolicy::default();
            let mut cart = Cart::default();
            let mut existing_item = false;

            if let Some(record) = existing {
                wp.generation_policy = GenerationPolicy::ExpectGenEqual;
                wp.generation = record.generation;
                cart = Cart::from_bins(&record.bins)?;
                if cart.find_item(product_id).is_some() {
                    existing_item = true;
                    if let Some(item) = cart.find_item_mut(product_id) {
                        item.quantity += quantity;
                    }
                } else {
                    cart.add(CartItem::from_product(user_id, quantity, image.clone(), &product));
                }
            } else {
                wp.record_exists_action = RecordExistsAction::CreateOnly;
                cart.add(CartItem::from_product(user_id, quantity, image.clone(), &product));
            }

            let op = if existing_item {
                maps::increment_value(
                    &mpolicy,
                    ITEMS_BIN,
                    as_val!("quantity"),
                    as_val!(quantity as i64),
                )
                .context(vec![ctx_map_key(as_val!(product_id))])
            } else {
                let item = cart
                    .find_item(product_id)
                    .expect("cart item must exist");
                maps::put(
                    &mpolicy,
                    ITEMS_BIN,
                    as_val!(product_id),
                    model_to_value(item),
                )
            };

            match client.operate(&wp, &key, &[op]).await {
                Ok(_) => return Ok(cart),
                Err(Error::ServerError(ResultCode::GenerationError, _, _)) => {
                    tracing::info!("Lost race condition when adding product {product_id}");
                    continue;
                }
                Err(err) => return Err(err.into()),
            }
        }
    }

    async fn update_cart_item(
        &self,
        user_id: &str,
        product_id: &str,
        quantity: i32,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let key = Self::cart_key(user_id);

        let record = match client.get(&ReadPolicy::default(), &key, Bins::All).await {
            Ok(record) => record,
            Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => return Ok(Cart::default()),
            Err(err) => return Err(err.into()),
        };

        let mut cart = Cart::from_bins(&record.bins)?;
        let mpolicy = Self::map_policy();

        if quantity <= 0 {
            cart.remove(product_id);
            let op = maps::remove_by_key(ITEMS_BIN, as_val!(product_id), MapReturnType::None);
            client.operate(&WritePolicy::default(), &key, &[op]).await?;
            return Ok(cart);
        }

        if let Some(item) = cart.find_item_mut(product_id) {
            item.quantity = quantity;
            let op = maps::put(
                &mpolicy,
                ITEMS_BIN,
                as_val!("quantity"),
                as_val!(quantity as i64),
            )
            .context(vec![ctx_map_key(as_val!(product_id))]);
            client.operate(&WritePolicy::default(), &key, &[op]).await?;
        }

        Ok(cart)
    }

    async fn remove_from_cart(
        &self,
        user_id: &str,
        product_id: &str,
    ) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        self.update_cart_item(user_id, product_id, 0).await
    }

    async fn clear_cart(&self, user_id: &str) -> Result<Cart, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let key = Self::cart_key(user_id);
        let op = maps::clear(ITEMS_BIN);
        match client.operate(&WritePolicy::default(), &key, &[op]).await {
            Ok(_) => Ok(Cart::default()),
            Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => Ok(Cart::default()),
            Err(err) => Err(err.into()),
        }
    }
}

impl ReferenceClient {
    async fn get_category_part(
        &self,
        bin_name: &str,
    ) -> Result<Vec<String>, Box<dyn std::error::Error + Send + Sync>> {
        let client = self.require_client().await?;
        let key = Self::category_key();
        match client
            .get(&ReadPolicy::default(), &key, Bins::from([bin_name]))
            .await
        {
            Ok(record) => {
                let values = record
                    .bins
                    .get(bin_name)
                    .map(value_as_list)
                    .unwrap_or_default();
                Ok(values
                    .into_iter()
                    .filter_map(|v| value_as_string(&v))
                    .collect())
            }
            Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => Ok(Vec::new()),
            Err(err) => Err(err.into()),
        }
    }
}
