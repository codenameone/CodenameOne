/// BLE central role: scanning (`BluetoothLE`, `ScanSettings`,
/// `ScanFilter`, `ScanResult`, `BleScan`), connections and the GATT
/// client (`BlePeripheral`, connection events) and L2CAP
/// connection-oriented channels (`L2capChannel`, `L2capServer`).
///
/// Obtain the entry point via `Bluetooth.getInstance().getLE()`. Any
/// number of scans and per-peripheral GATT operations may run
/// concurrently -- an internal queue serializes operations toward the
/// platform stack and every call returns its own `AsyncResource`.
package com.codename1.bluetooth.le;
