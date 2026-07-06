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


class TheComponentsOfCodenameOneJava136Snippet {

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
        // tag::the-components-of-codename-one-java-136[]
        Form f = new Form("Accordion", new BoxLayout(BoxLayout.Y_AXIS));
        f.setScrollableY(true);
        Accordion accr = new Accordion();
        accr.addContent("Item1", new SpanLabel("The quick brown fox jumps over the lazy dog\n"
                + "The quick brown fox jumps over the lazy dog"));
        accr.addContent("Item2", new SpanLabel("The quick brown fox jumps over the lazy dog\n"
                + "The quick brown fox jumps over the lazy dog\n "
                + "The quick brown fox jumps over the lazy dog\n "
                + "The quick brown fox jumps over the lazy dog\n "
                + ""));

        accr.addContent("Item3", BoxLayout.encloseY(new Label("Label"), new TextField(), new Button("Button"), new CheckBox("CheckBox")));

        f.add(accr);
        f.show();
        // end::the-components-of-codename-one-java-136[]
    }
}
