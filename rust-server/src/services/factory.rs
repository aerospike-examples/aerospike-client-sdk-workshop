use std::sync::Arc;

use crate::config::{ClientProfile, Settings};
use crate::services::reference_client::ReferenceClient;
use crate::services::workshop_client::WorkshopClient;
use crate::services::KeyValueService;

pub fn create_key_value_service(settings: &Settings) -> Arc<dyn KeyValueService> {
    match settings.aerospike_client_profile {
        ClientProfile::Reference => Arc::new(ReferenceClient::new(settings.clone())),
        ClientProfile::Workshop => Arc::new(WorkshopClient::new(settings.clone())),
        ClientProfile::WorkshopAnswers => Arc::new(ReferenceClient::new(settings.clone())),
    }
}
