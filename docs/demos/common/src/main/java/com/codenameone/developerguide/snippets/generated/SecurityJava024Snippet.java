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
import com.codename1.security.Jwt;
import java.util.LinkedHashMap;
import java.util.Map;

class SecurityJava024Snippet {

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
        // tag::security-java-024[]
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "user-123");
        claims.put("exp", System.currentTimeMillis() / 1000 + 3600);

        // Sign with a shared secret (HS256)
        String token = Jwt.signHs256(claims, "secret".getBytes("UTF-8"));

        // Parse + verify
        Jwt parsed = Jwt.parse(token);
        if (!parsed.verifyHs256("secret".getBytes("UTF-8"))) {
            throw new SecurityException("bad signature");
        }
        String subject = (String) parsed.getClaim("sub");
        // end::security-java-024[]
    }
}
