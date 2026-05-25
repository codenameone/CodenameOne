/// WiFi inspection, scanning, connection and peer-to-peer (WiFi Direct).
///
/// `WiFi` exposes the current connection's SSID/BSSID/IP plus APIs to scan
/// for and connect to nearby networks (`WiFiNetwork`, `WiFiSecurity`,
/// `WiFiScanCallback`, `WiFiConnectCallback`).
///
/// `WiFiDirect` covers Android-style WiFi Direct peer discovery and
/// session setup via `WiFiDirectPeer` and `WiFiDirectListener`.
///
/// `WifiPlatform` / `WifiDirectPlatform` are the SPIs the active port
/// implements; on modern Android (10+) and iOS, the SSID/BSSID queries
/// require runtime permissions or entitlements that the build pipeline
/// injects automatically based on classpath scanning.
package com.codename1.io.wifi;
