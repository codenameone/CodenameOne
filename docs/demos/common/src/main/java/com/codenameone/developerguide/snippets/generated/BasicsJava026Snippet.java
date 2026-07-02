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


class BasicsJava026Snippet {

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
        // tag::basics-java-026[]
        Form hi = new Form("Layered Layout");
        int w = Math.min(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
        Button settingsLabel = new Button("");
        Style settingsStyle = settingsLabel.getAllStyles();
        settingsStyle.setFgColor(0xff);
        settingsStyle.setBorder(null);
        settingsStyle.setBgColor(0xff00);
        settingsStyle.setBgTransparency(255);
        settingsStyle.setFont(settingsLabel.getUnselectedStyle().getFont().derive(w / 3, Font.STYLE_PLAIN));
        FontImage.setMaterialIcon(settingsLabel, FontImage.MATERIAL_SETTINGS);
        Button close = new Button("");
        close.setUIID("Container");
        close.getAllStyles().setFgColor(0xff0000);
        FontImage.setMaterialIcon(close, FontImage.MATERIAL_CLOSE);
        hi.add(LayeredLayout.encloseIn(settingsLabel,
                FlowLayout.encloseRight(close)));
        // end::basics-java-026[]
    }
}
