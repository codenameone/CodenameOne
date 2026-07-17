/// BLE peripheral role: the local GATT server (`GattServer`,
/// `GattLocalService`, `GattLocalCharacteristic`, request envelopes) and
/// advertising (`AdvertiseSettings`, `AdvertiseData`,
/// `BleAdvertisement`).
///
/// Obtain via `Bluetooth.getInstance().getLE().openGattServer(...)` and
/// `startAdvertising(...)`; branch on
/// `Bluetooth.getInstance().isPeripheralModeSupported()` first.
///
/// Referencing this package is what triggers the automatic injection of
/// advertise permissions (Android `BLUETOOTH_ADVERTISE`) at build time --
/// central-only apps that never touch it are not burdened with them.
package com.codename1.bluetooth.le.server;
