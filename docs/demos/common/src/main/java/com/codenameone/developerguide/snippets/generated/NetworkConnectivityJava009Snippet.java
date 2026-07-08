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
import com.codename1.io.usb.Usb;
import com.codename1.io.usb.UsbDevice;
import com.codename1.io.usb.UsbDeviceListener;

class NetworkConnectivityJava009Snippet {

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
        // tag::network-connectivity-java-009[]
        if (!Usb.isSupported()) { return; }

        Usb.addDeviceListener(new UsbDeviceListener() {
            @Override
            public void onDeviceAttached(UsbDevice d) {
                if (d.getVendorId() == 0x0403 && d.getProductId() == 0x6001) {
                    Usb.requestPermission(d);
                }
            }
            @Override
            public void onDeviceDetached(UsbDevice d) { }
            @Override
            public void onPermissionResult(UsbDevice d, boolean granted) {
                if (granted) {
                    try (InputStream in = Usb.openInputStream(d, 0x81);
                         OutputStream out = Usb.openOutputStream(d, 0x02)) {
                        out.write("AT\r\n".getBytes());
                        byte[] buf = new byte[64];
                        int n = in.read(buf);
                        // null
                    } catch (IOException e) { Log.e(e); }
                }
            }
        });
        // end::network-connectivity-java-009[]
    }
}
