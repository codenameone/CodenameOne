/// Client-side WebSocket support.
///
/// Promoted into Codename One core from the legacy `cn1-websockets` cn1lib.
/// Application code subclasses [com.codename1.io.websocket.WebSocket] and
/// overrides the four `onXxx` callbacks; the framework dispatches inbound
/// events on the EDT via a per-platform [com.codename1.io.websocket.WebSocketNativeImpl].
///
/// Per-platform implementations live next to the rest of the platform port:
///
/// - iOS: `ios/src/main/objectivec/com_codename1_io_websocket_WebSocketNativeImplImpl.{h,m}`
///   wrapping `URLSessionWebSocketTask` (iOS 13+).
/// - Android: `android/src/main/java/com/codename1/io/websocket/WebSocketNativeImplImpl.java`
///   wrapping `okhttp3.WebSocket` or `java.net.http.WebSocket` (API 33+).
/// - JavaScript: `javascript/src/main/javascript/com_codename1_io_websocket_WebSocketNativeImplImpl.js`
///   wrapping `window.WebSocket`.
/// - JavaSE simulator: `javase/src/main/java/com/codename1/io/websocket/WebSocketNativeImplImpl.java`
///   wrapping `java.net.http.WebSocket` (JDK 11+).
///
/// Each impl receives a numeric ID via `setId(int)` from the framework, opens
/// its underlying connection, and dispatches inbound events through the
/// static callbacks on [com.codename1.io.websocket.WebSocket]
/// (`openReceived`, `messageReceived`, `closeReceived`, `errorReceived`).
package com.codename1.io.websocket;
