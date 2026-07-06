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
import com.codename1.security.Biometrics;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;

class BiometricAuthenticationJava001Snippet {

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
        // tag::biometric-authentication-java-001[]
        Biometrics b = Biometrics.getInstance();
        if (!b.canAuthenticate()) {
            // Fall back to password
            return;
        }
        b.authenticate("Unlock your account").onResult((success, err) -> {
            if (err != null) {
                BiometricError code = ((BiometricException) err).getError();
                switch (code) {
                    case USER_CANCELED: /* user dismissed the prompt */ break;
                    case LOCKED_OUT:    /* too many bad attempts */ break;
                    case NOT_ENROLLED:  /* prompt the user to enrol in Settings */ break;
                    default:            /* generic failure */ break;
                }
            } else {
                // Authenticated. Continue with the gated action.
            }
        });
        // end::biometric-authentication-java-001[]
    }
}
