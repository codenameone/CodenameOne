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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;
import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothPermission;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.BleScan;
import com.codename1.bluetooth.le.ScanFilter;
import com.codename1.bluetooth.le.ScanSettings;

class BluetoothJava001Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    void snippet() throws Exception {
        // tag::bluetooth-java-001[]
        Bluetooth bt = Bluetooth.getInstance();
        if (!bt.isLeSupported()) {
            // hide the feature on ports without BLE
            return;
        }
        bt.requestPermissions(BluetoothPermission.SCAN, BluetoothPermission.CONNECT)
                .onResult((granted, permissionErr) -> {
            if (permissionErr != null || !granted) {
                return;
            }
            ScanSettings settings = new ScanSettings()
                    .addFilter(new ScanFilter()
                            .setServiceUuid(BluetoothUuid.fromShort(0x180D))
                            .setNamePrefix("Polar"));
            BleScan[] scan = new BleScan[1];
            scan[0] = bt.getLE().startScan(settings, sighting -> {
                scan[0].stop();
                sighting.getPeripheral().connect()
                        .onResult((peripheral, connectErr) -> {
                    if (connectErr != null) {
                        return;
                    }
                    peripheral.discoverServices()
                            .onResult((services, discoverErr) -> {
                        // the GATT database is ready -- see the
                        // following sections
                    });
                });
            });
        });
        // end::bluetooth-java-001[]
    }
}
