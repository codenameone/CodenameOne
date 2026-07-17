/// Classic Bluetooth (BR/EDR): inquiry discovery (`ClassicDiscovery`),
/// bonded-device listing and RFCOMM stream connections
/// (`RfcommConnection`, `RfcommServer`).
///
/// Obtain via `Bluetooth.getInstance().getClassic()` and branch on
/// `Bluetooth.getInstance().isClassicSupported()` -- iOS does not expose
/// classic Bluetooth to applications. The RFCOMM streams block and must
/// be consumed off the EDT.
package com.codename1.bluetooth.classic;
