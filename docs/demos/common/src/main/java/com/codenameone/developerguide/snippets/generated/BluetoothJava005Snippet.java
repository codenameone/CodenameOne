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
import com.codename1.bluetooth.le.BlePeripheral;

class BluetoothJava005Snippet {

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
    BlePeripheral peripheral;
    int psm = 0x81;
    void snippet() throws Exception {
        // tag::bluetooth-java-005[]
        if (!Bluetooth.getInstance().isL2capSupported()) {
            return;
        }
        peripheral.openL2capChannel(psm, true).onResult((channel, err) -> {
            if (err != null) {
                return;
            }
            // the streams block -- always consume them off the EDT
            CN.startThread(() -> {
                try {
                    OutputStream out = channel.getOutputStream();
                    out.write(new byte[] {1, 2, 3});
                    out.flush();
                    InputStream in = channel.getInputStream();
                    byte[] buffer = new byte[4096];
                    int size = in.read(buffer);
                    // process the response...
                } catch (IOException ioErr) {
                    // transport failure
                } finally {
                    try {
                        channel.close();
                    } catch (IOException ignored) {
                    }
                }
            }, "L2CAP IO").start();
        });

        // the peripheral side listens and publishes its PSM to centrals
        Bluetooth.getInstance().getLE().openL2capServer(true)
                .onResult((server, serverErr) -> {
            if (serverErr == null) {
                int assignedPsm = server.getPsm();
                // advertise assignedPsm through a GATT characteristic,
                // then server.accept() incoming channels
            }
        });
        // end::bluetooth-java-005[]
    }
}
