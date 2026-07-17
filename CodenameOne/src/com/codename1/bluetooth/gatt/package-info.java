/// The client-side GATT model discovered on a remote peripheral:
/// `GattService`, `GattCharacteristic` (read/write/subscribe),
/// `GattDescriptor`, notification listeners and ATT status codes.
///
/// Instances are constructed by the active port during
/// `BlePeripheral.discoverServices()`; application code navigates the
/// model and issues operations, which route through the owning
/// peripheral's serialized operation queue.
package com.codename1.bluetooth.gatt;
