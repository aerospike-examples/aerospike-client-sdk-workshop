use std::env;

#[derive(Debug, Clone)]
pub struct Settings {
    pub aerospike_host: String,
    pub aerospike_port: u16,
    pub aerospike_username: Option<String>,
    pub aerospike_password: Option<String>,
    pub aerospike_client_profile: ClientProfile,
    pub server_port: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ClientProfile {
    Reference,
    Workshop,
    WorkshopAnswers,
}

impl Settings {
    pub fn from_env() -> Self {
        let profile = match env::var("AEROSPIKE_CLIENT_PROFILE")
            .unwrap_or_else(|_| "reference".to_string())
            .as_str()
        {
            "reference" | "old-client" => ClientProfile::Reference,
            "workshop" | "new-client" => ClientProfile::Workshop,
            "workshop-answers" | "new-client-answers" => ClientProfile::WorkshopAnswers,
            other => panic!("Unknown AEROSPIKE_CLIENT_PROFILE: {other}"),
        };

        Self {
            aerospike_host: env::var("AEROSPIKE_HOST").unwrap_or_else(|_| "localhost".to_string()),
            aerospike_port: env::var("AEROSPIKE_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(3000),
            aerospike_username: env::var("AEROSPIKE_USERNAME").ok().filter(|s| !s.is_empty()),
            aerospike_password: env::var("AEROSPIKE_PASSWORD").ok(),
            aerospike_client_profile: profile,
            server_port: env::var("SERVER_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(8080),
        }
    }

    pub fn aerospike_hosts(&self) -> String {
        format!("{}:{}", self.aerospike_host, self.aerospike_port)
    }
}
