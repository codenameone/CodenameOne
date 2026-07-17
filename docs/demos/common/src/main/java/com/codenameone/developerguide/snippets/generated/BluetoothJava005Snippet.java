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
