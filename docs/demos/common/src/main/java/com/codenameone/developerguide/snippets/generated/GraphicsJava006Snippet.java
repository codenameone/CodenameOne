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


class GraphicsJava006Snippet {

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
        // tag::graphics-java-006[]
        Form hi = new Form("Glass Pane", new BoxLayout(BoxLayout.Y_AXIS));
        Style s = UIManager.getInstance().getComponentStyle("Label");
        s.setFgColor(0xff0000);
        s.setBgTransparency(0);
        Image warningImage = FontImage.createMaterial(FontImage.MATERIAL_WARNING, s).toImage();
        TextField tf1 = new TextField("My Field");
        tf1.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        tf1.getAllStyles().setMargin(5, 5, 5, 5);
        hi.add(tf1);
        hi.setGlassPane((g, rect) -> {
            int x = tf1.getAbsoluteX() + tf1.getWidth();
            int y = tf1.getAbsoluteY();
            x -= warningImage.getWidth() / 2;
            y += (tf1.getHeight() / 2 - warningImage.getHeight() / 2);
            g.drawImage(warningImage, x, y);
        });
        hi.show();
        // end::graphics-java-006[]
    }
}
