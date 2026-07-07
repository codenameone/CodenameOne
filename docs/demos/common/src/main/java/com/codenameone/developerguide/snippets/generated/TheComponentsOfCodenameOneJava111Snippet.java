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


class TheComponentsOfCodenameOneJava111Snippet {

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
        // tag::the-components-of-codename-one-java-111[]
        Form hi = new Form("BrowserComponent", new BorderLayout());
        BrowserComponent bc = new BrowserComponent();
        bc.setPage( "<html lang=\"en\">\n" +
                    "    <head>\n" +
                    "        <meta charset=\"utf-8\">\n" +
                    "        <script>\n" +
                    "          function  fnc(message) {\n" +
                    "         document.write(message);\n" +
                    "            };\n" +
                    "        </script>\n" +
                    "    </head>\n" +
                    "    <body >\n" +
                    "        <p>Demo</p>\n" +
                    "    </body>\n" +
                    "</html>", null);
        TextField tf = new TextField();
        hi.add(BorderLayout.CENTER, bc).
                add(BorderLayout.SOUTH, tf);
        bc.addWebEventListener("onLoad", (e) ->  bc.execute("fnc('<p>Hello World</p>')"));
        tf.addActionListener((e) -> bc.execute("fnc('<p>" + tf.getText() +"</p>')"));
        hi.show();
        // end::the-components-of-codename-one-java-111[]
    }
}
