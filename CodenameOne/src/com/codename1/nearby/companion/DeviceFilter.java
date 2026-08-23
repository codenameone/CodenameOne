/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.nearby.companion;

/// Narrows what the system device chooser offers the user.
///
/// A request with no filter shows everything the radios can see, which is a
/// long and confusing list; a request with one good filter usually shows
/// exactly the accessory the user is holding. Filters within one
/// [AssociationRequest] are OR-combined -- a device matching any of them is
/// offered.
///
/// ```java
/// AssociationRequest req = new AssociationRequest.Builder()
///         .addFilter(DeviceFilter.bleService("180D"))     // heart rate
///         .addFilter(DeviceFilter.namePattern("Acme.*"))
///         .build();
/// ```
public final class DeviceFilter {

    /// Filter kind: match a BLE service UUID being advertised.
    public static final int KIND_BLE_SERVICE = 0;

    /// Filter kind: match the advertised device name against a regular
    /// expression.
    public static final int KIND_NAME_PATTERN = 1;

    /// Filter kind: match one exact device address.
    public static final int KIND_ADDRESS = 2;

    /// Filter kind: match a Wi-Fi SSID.
    public static final int KIND_WIFI_SSID = 3;

    private final int kind;
    private final String value;

    private DeviceFilter(int kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    /// Offers only devices advertising the given BLE service.
    ///
    /// #### Parameters
    ///
    /// - `serviceUuid`: the service UUID, in either the 16-bit short form
    ///   (`"180D"`) or the full 128-bit form
    ///
    /// #### Returns
    ///
    /// the filter
    public static DeviceFilter bleService(String serviceUuid) {
        return new DeviceFilter(KIND_BLE_SERVICE, require(serviceUuid));
    }

    /// Offers only devices whose advertised name matches a regular
    /// expression.
    ///
    /// The pattern is passed through to the platform, which on Android is
    /// `java.util.regex` and on iOS is a substring match on the accessory
    /// name -- so keep patterns simple if the app runs on both.
    ///
    /// #### Parameters
    ///
    /// - `pattern`: the pattern to match the name against
    ///
    /// #### Returns
    ///
    /// the filter
    public static DeviceFilter namePattern(String pattern) {
        return new DeviceFilter(KIND_NAME_PATTERN, require(pattern));
    }

    /// Offers only the device at one exact address -- the reconnect case,
    /// where the app already knows which device it wants.
    ///
    /// #### Parameters
    ///
    /// - `address`: the device address, as
    ///   `com.codename1.bluetooth.BluetoothDevice#getAddress()` reports it
    ///
    /// #### Returns
    ///
    /// the filter
    public static DeviceFilter address(String address) {
        return new DeviceFilter(KIND_ADDRESS, require(address));
    }

    /// Offers only the Wi-Fi network with the given SSID. Android only;
    /// ignored on platforms that associate Bluetooth accessories alone.
    ///
    /// #### Parameters
    ///
    /// - `ssid`: the network name
    ///
    /// #### Returns
    ///
    /// the filter
    public static DeviceFilter wifiSsid(String ssid) {
        return new DeviceFilter(KIND_WIFI_SSID, require(ssid));
    }

    /// Which of the `KIND_` constants this filter is.
    public int getKind() {
        return kind;
    }

    /// The UUID, pattern, address or SSID, depending on [#getKind()].
    public String getValue() {
        return value;
    }

    public String toString() {
        return "DeviceFilter[kind=" + kind + ", value=" + value + "]";
    }

    private static String require(String v) {
        if (v == null || v.length() == 0) {
            throw new IllegalArgumentException(
                    "a device filter needs a non-empty value");
        }
        return v;
    }
}
