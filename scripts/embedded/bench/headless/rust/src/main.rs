// The Rust half of the headless comparison: the same two routes on axum, the
// most used Rust web framework, with tokio's default multi-threaded runtime.
use axum::{routing::get, Json, Router};
use serde::Serialize;

#[derive(Serialize)]
struct Message {
    message: &'static str,
}

#[tokio::main]
async fn main() {
    let port = std::env::var("PORT").unwrap_or_else(|_| "8080".into());
    let app = Router::new()
        .route("/plaintext", get(|| async { "Hello, World!" }))
        .route("/json", get(|| async { Json(Message { message: "Hello, World!" }) }));
    let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{port}")).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
