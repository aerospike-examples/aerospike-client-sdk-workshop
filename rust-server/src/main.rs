#[macro_use]
extern crate aerospike;

mod config;
mod data_loading;
mod json_parsing;
mod models;
mod routers;
mod services;
mod startup;
mod utils;

use std::net::SocketAddr;
use std::path::PathBuf;
use std::sync::Arc;

use axum::Router;
use tower_http::cors::{Any, CorsLayer};
use tower_http::services::{ServeDir, ServeFile};

use crate::config::Settings;
use crate::data_loading::DataLoadingService;
use crate::services::factory::create_key_value_service;
use crate::startup::{run_startup, AppState};

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info,aerospike_rust_workshop=info".into()),
        )
        .init();

    if let Err(err) = run().await {
        tracing::error!("Server failed: {err}");
        std::process::exit(1);
    }
}

async fn run() -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let settings = Settings::from_env();
    let kv_service = create_key_value_service(&settings);
    kv_service.connect().await?;

    let data_loading_service = Arc::new(DataLoadingService::new(kv_service.clone()));
    let state = Arc::new(AppState {
        kv_service,
        data_loading_service: data_loading_service.clone(),
    });

    tracing::info!(
        "Started with client profile: {:?}",
        settings.aerospike_client_profile
    );

    run_startup(&data_loading_service).await;

    let app = build_router(state.clone());
    let addr = SocketAddr::from(([0, 0, 0, 0], settings.server_port));
    tracing::info!("Listening on http://{addr}");

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal(state))
        .await?;
    Ok(())
}

async fn shutdown_signal(state: Arc<AppState>) {
    tokio::signal::ctrl_c()
        .await
        .expect("failed to install CTRL+C handler");
    tracing::info!("Shutting down...");
    if let Err(err) = state.kv_service.close().await {
        tracing::warn!("Error closing Aerospike client: {err}");
    }
}

fn static_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("static")
}

fn build_router(state: Arc<AppState>) -> Router {
    let api = Router::new()
        .merge(routers::retail::router())
        .merge(routers::cart::router())
        .merge(routers::data_loading::router())
        .with_state(state);

    let router = {
        let static_dir = static_dir();
        if static_dir.is_dir() {
            let index = static_dir.join("index.html");
            api.fallback_service(ServeDir::new(static_dir).not_found_service(ServeFile::new(index)))
        } else {
            tracing::warn!(
                "Static directory not found at {}. Build the website with: cd website && npm run build:rust",
                static_dir.display()
            );
            api
        }
    };

    router.layer(
        CorsLayer::new()
            .allow_origin(Any)
            .allow_methods(Any)
            .allow_headers(Any),
    )
}
