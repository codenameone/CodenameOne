/// Cross-platform Bluetooth API: adapter state, runtime permissions and
/// capability queries.
///
/// `Bluetooth.getInstance()` returns the platform implementation and is
/// the single entry point; from it, `getLE()` reaches the BLE central
/// role (`com.codename1.bluetooth.le`), `getLE().openGattServer(...)` /
/// `startAdvertising(...)` the peripheral role
/// (`com.codename1.bluetooth.le.server`) and `getClassic()` classic
/// RFCOMM (`com.codename1.bluetooth.classic`). Identity types
/// (`BluetoothDevice`, `BluetoothUuid`) and the typed error model
/// (`BluetoothError`, `BluetoothException`) live here.
///
/// Every callback of the API is delivered on the EDT; only the blocking
/// RFCOMM/L2CAP streams are consumed off it.
package com.codename1.bluetooth;
