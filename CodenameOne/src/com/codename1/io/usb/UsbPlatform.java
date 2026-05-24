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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Platform-supplied implementation of the USB host APIs. Application code
/// talks to `Usb`; the facade fetches this via
/// `Display.getInstance().getUsbPlatform()`.
///
/// Part of the framework's service-provider interface, not intended for
/// application use.
public class UsbPlatform {
    public boolean isSupported() {
        return false;
    }

    public UsbDevice[] listDevices() {
        return new UsbDevice[0];
    }

    public void addDeviceListener(UsbDeviceListener listener) {
    }

    public void removeDeviceListener(UsbDeviceListener listener) {
    }

    public void requestPermission(UsbDevice device) {
    }

    public boolean hasPermission(UsbDevice device) {
        return false;
    }

    public InputStream openInputStream(UsbDevice device, int endpointAddress)
            throws IOException {
        throw new IOException("USB is not supported on this platform");
    }

    public OutputStream openOutputStream(UsbDevice device, int endpointAddress)
            throws IOException {
        throw new IOException("USB is not supported on this platform");
    }
}
