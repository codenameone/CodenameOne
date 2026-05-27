/// Client-side WebSocket support.
///
/// Promoted into Codename One core from the legacy `cn1-websockets` cn1lib.
/// Application code subclasses [com.codename1.io.websocket.WebSocket] and
/// overrides the four `onXxx` callbacks; the framework dispatches inbound
/// events on the EDT through the per-platform
/// [com.codename1.io.websocket.WebSocketImpl] returned by
/// [com.codename1.impl.CodenameOneImplementation#createWebSocketImpl].
///
/// Per-platform implementations live alongside the rest of the platform
/// port and subclass `WebSocketImpl` directly -- no native interface
/// marshaling, no string-id round-trip. Suggested mapping:
///
/// - iOS: `URLSessionWebSocketTask` (iOS 13+) wrapped by a `WebSocketImpl`
///   subclass returned from the iOS port's `createWebSocketImpl`
///   override.
/// - Android: `okhttp3.WebSocket` (or `java.net.http.WebSocket` on API
///   33+) wrapped by a `WebSocketImpl` subclass in the Android port's
///   `createWebSocketImpl` override.
/// - JavaScript: thin wrapper over `window.WebSocket` in the JavaScript
///   port's `createWebSocketImpl` override.
/// - JavaSE simulator: `java.net.http.WebSocket` (JDK 11+) in the JavaSE
///   port's `createWebSocketImpl` override.
///
/// Each implementation holds a reference to the owning
/// [com.codename1.io.websocket.WebSocket] passed to it in
/// `createWebSocketImpl`, hops to the EDT when an inbound event arrives,
/// and dispatches through the parent's `onOpenReceived` /
/// `onMessageReceived` / `onCloseReceived` / `onErrorReceived` methods.
package com.codename1.io.websocket;
