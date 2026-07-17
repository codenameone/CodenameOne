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
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.classic.BluetoothClassic;

class BluetoothJava006Snippet {

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
        // tag::bluetooth-java-006[]
        Bluetooth bt = Bluetooth.getInstance();
        if (!bt.isClassicSupported()) {
            // classic Bluetooth is unavailable -- iOS among others
            return;
        }
        BluetoothClassic classic = bt.getClassic();
        classic.startDiscovery(sighting -> {
            if ("SerialPrinter".equals(sighting.getDevice().getName())) {
                classic.connect(sighting.getDevice(), BluetoothUuid.SPP, true)
                        .onResult((connection, err) -> {
                    if (err != null) {
                        return;
                    }
                    // consume the blocking streams off the EDT, exactly
                    // like the L2CAP example
                });
            }
        });

        // accepting incoming SPP clients
        classic.listen("Codename One SPP", BluetoothUuid.SPP, true)
                .onResult((server, err) -> {
            if (err == null) {
                server.accept().onResult((connection, acceptErr) -> {
                    // one client accepted; call accept() again for the next
                });
            }
        });
        // end::bluetooth-java-006[]
    }
}
