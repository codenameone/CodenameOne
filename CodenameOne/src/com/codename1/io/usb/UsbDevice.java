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

/// One attached USB device. Immutable. The `Object` returned by
/// `getNativeDevice()` is the platform device handle and may be cast in
/// native interface code (`UsbDevice` on Android).
public final class UsbDevice {
    private final String deviceName;
    private final int vendorId;
    private final int productId;
    private final String productName;
    private final String manufacturerName;
    private final Object nativeDevice;

    public UsbDevice(String deviceName, int vendorId, int productId,
                     String productName, String manufacturerName,
                     Object nativeDevice) {
        this.deviceName = deviceName;
        this.vendorId = vendorId;
        this.productId = productId;
        this.productName = productName;
        this.manufacturerName = manufacturerName;
        this.nativeDevice = nativeDevice;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public Object getNativeDevice() {
        return nativeDevice;
    }
}
