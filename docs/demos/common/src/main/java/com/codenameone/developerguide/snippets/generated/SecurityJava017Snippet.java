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


class SecurityJava017Snippet {

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
        // tag::security-java-017[]
        // Switch on tapjacking protection once, during startup. BLOCK drops any gesture
        // that begins while another app's window covers the touched point.
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);

        // A blocked gesture never reaches your components, so this listener is how the
        // app learns the tap happened. It fires when the state changes, not per touch.
        DeviceIntegrity.addTapjackingListener(e -> {
            if (DeviceIntegrity.isScreenObscured()) {
                Dialog.show("Security warning",
                        "Another app is drawing over this screen. Close it before continuing.",
                        "OK", null);
            }
        });

        // On a sensitive screen, remove the overlay instead of merely filtering its taps.
        // This is Android 12+ only, and unlike the touch filter it also protects native
        // peer components such as BrowserComponent.
        if (DeviceIntegrity.isHideOverlayWindowsSupported()) {
            DeviceIntegrity.setHideOverlayWindows(true);
        }
        DeviceIntegrity.setSecureScreen(true);
        // end::security-java-017[]
    }
}
