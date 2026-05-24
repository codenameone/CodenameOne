/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.usb;

import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// USB Host API.
///
/// Lets the device act as a USB host and talk to attached peripherals -- a
/// barcode scanner, a serial-over-USB device, a microcontroller. This is
/// **Android-only** in practice; iOS doesn't expose third-party USB host
/// access and the simulator/JavaSE port stubs everything out.
///
/// #### Android specifics
///
/// The build pipeline adds the `android.hardware.usb.host` feature and a
/// `<uses-feature>` declaration whenever `Usb` is referenced. To launch your
/// app automatically when a device is plugged in, declare a
/// `USB_DEVICE_ATTACHED` intent filter in `android.xintent_filter` using a
/// `device_filter.xml` resource you ship in `native/android/res/xml/`. See
/// `Developer Guide -> Network Connectivity -> USB`.
public final class Usb {
    private Usb() {
    }

    private static UsbPlatform platform() {
        return Display.getInstance().getUsbPlatform();
    }

    /// `true` if the current platform implements USB host access.
    public static boolean isSupported() {
        return platform().isSupported();
    }

    /// All currently-attached USB devices.
    public static UsbDevice[] listDevices() {
        return platform().listDevices();
    }

    /// Subscribes `listener` to attach / detach events. Returns immediately.
    /// Calls on the EDT.
    public static void addDeviceListener(UsbDeviceListener listener) {
        platform().addDeviceListener(listener);
    }

    public static void removeDeviceListener(UsbDeviceListener listener) {
        platform().removeDeviceListener(listener);
    }

    /// Requests permission from the user to talk to `device`. The result is
    /// reported asynchronously via `UsbDeviceListener.onPermissionResult`.
    public static void requestPermission(UsbDevice device) {
        platform().requestPermission(device);
    }

    /// `true` if the user has granted access to `device`.
    public static boolean hasPermission(UsbDevice device) {
        return platform().hasPermission(device);
    }

    /// Opens a bulk-transfer endpoint on the device. `endpointAddress` matches
    /// the USB endpoint address from the device's descriptor. The caller must
    /// have called `requestPermission` and received approval first.
    public static InputStream openInputStream(UsbDevice device,
                                              int endpointAddress)
            throws IOException {
        return platform().openInputStream(device, endpointAddress);
    }

    public static OutputStream openOutputStream(UsbDevice device,
                                                int endpointAddress)
            throws IOException {
        return platform().openOutputStream(device, endpointAddress);
    }
}
