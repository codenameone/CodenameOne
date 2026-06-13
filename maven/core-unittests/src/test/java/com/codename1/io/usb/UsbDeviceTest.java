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
package com.codename1.io.usb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the immutable {@link UsbDevice} value object.
 */
class UsbDeviceTest {

    @Test
    void allFieldsAreExposed() {
        Object native_ = new Object();
        UsbDevice d = new UsbDevice("/dev/bus/usb/001/002", 0x1234, 0x5678,
                "Widget Reader", "Acme Corp", native_);
        assertEquals("/dev/bus/usb/001/002", d.getDeviceName());
        assertEquals(0x1234, d.getVendorId());
        assertEquals(0x5678, d.getProductId());
        assertEquals("Widget Reader", d.getProductName());
        assertEquals("Acme Corp", d.getManufacturerName());
        assertSame(native_, d.getNativeDevice());
    }

    @Test
    void nullableFieldsAreTolerated() {
        UsbDevice d = new UsbDevice(null, 0, 0, null, null, null);
        assertNull(d.getDeviceName());
        assertEquals(0, d.getVendorId());
        assertEquals(0, d.getProductId());
        assertNull(d.getProductName());
        assertNull(d.getManufacturerName());
        assertNull(d.getNativeDevice());
    }
}
